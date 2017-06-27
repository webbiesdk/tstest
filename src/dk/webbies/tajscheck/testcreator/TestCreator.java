package dk.webbies.tajscheck.testcreator;

import dk.au.cs.casa.typescript.types.*;
import dk.webbies.tajscheck.Main;
import dk.webbies.tajscheck.TypeWithContext;
import dk.webbies.tajscheck.benchmark.BenchmarkInfo;
import dk.webbies.tajscheck.testcreator.test.*;
import dk.webbies.tajscheck.testcreator.test.check.Check;
import dk.webbies.tajscheck.typeutil.TypesUtil;
import dk.webbies.tajscheck.typeutil.typeContext.TypeContext;
import dk.webbies.tajscheck.util.*;
import dk.webbies.tajscheck.util.trie.Trie;

import java.util.*;
import java.util.stream.Collectors;

public class TestCreator {
    private BenchmarkInfo info;

    public TestCreator(BenchmarkInfo info) {
        this.info = info;
    }

    public List<Test> createTests() {
        return createTests(false);
    }

    public List<Test> createTests(@SuppressWarnings("SameParameterValue") boolean concatDuplicates) {
        List<Test> loadTests = new ArrayList<>();
        for (Map.Entry<String, Type> userDefinedType : info.userDefinedTypes.entrySet()) {
            loadTests.add(new LoadModuleTest(Main.getRequirePath(info.bench), userDefinedType.getValue(), info, userDefinedType.getKey()));
        }

        if (info.bench.options.onlyInitialize) {
            return loadTests;
        }

        PriorityQueue<TestQueueElement> queue = new PriorityQueue<>();

        Set<TypeWithContext> negativeTypesSeen = new HashSet<>();

        CreateTestVisitor visitor = new CreateTestVisitor(queue, negativeTypesSeen);

        Set<TypeWithContext> seenTopLevel = new HashSet<>();

        List<Test> topLevelFunctionTests = new ArrayList<>();
        for (Map.Entry<String, Type> userDefinedType : info.userDefinedTypes.entrySet()) {
            TypeContext context = TypeContext.create(info);
            if (info.freeGenericsFinder.hasThisTypes(userDefinedType.getValue())) {
                context = context.withThisType(userDefinedType.getValue());
            }
            topLevelFunctionTests.addAll(addTopLevelFunctionTests(userDefinedType.getValue(), userDefinedType.getKey(), context, visitor, negativeTypesSeen, info.nativeTypes, 0, seenTopLevel));

            queue.add(new TestQueueElement(userDefinedType.getValue(), new Arg(userDefinedType.getKey(), context, 0)));
        }

        Trie pathsToTestTrie = info.bench.pathsToTest != null ? Trie.create(info.bench.pathsToTest) : null;

        while (!queue.isEmpty()) {
            TestQueueElement element = queue.poll();
            Arg arg = element.arg;

            if (info.bench.pathsToTest != null) {
                if (!isRelevantPath(arg.path, pathsToTestTrie)) {
                    continue;
                }
            }

            arg = arg.withTypeContext(arg.typeContext.optimizeTypeParameters(element.type));

            if (info.freeGenericsFinder.hasThisTypes(element.type)) {
                arg = arg.withThisType(element.type);
            }

            if (arg.withTopLevelFunctions) {
                topLevelFunctionTests.addAll(addTopLevelFunctionTests(element.type, arg.path, arg.typeContext, visitor, negativeTypesSeen, info.nativeTypes, arg.depth, seenTopLevel));
            }

            if (arg.path.equals("module.LayerGroup.new()")) {
                System.out.println();
            }

            element.type.accept(visitor, arg.noTopLevelFunctions());
        }


        List<Test> tests = Util.concat(loadTests, visitor.getTests(), topLevelFunctionTests);

        if (info.bench.pathsToTest != null) {
            tests = tests.stream().filter(test -> isRelevantPath(test.getPath(), pathsToTestTrie)).collect(Collectors.toList());

            Set<String> paths = tests.stream().map(Test::getPath).map(TestCreator::simplifyPath).collect(Collectors.toSet());

            assert Util.intersection(paths, info.bench.pathsToTest).size() == info.bench.pathsToTest.size();
        }

        if (info.bench.options.writePrimitives) {
            for (Test test : new ArrayList<>(tests)) {
                if (test instanceof PropertyReadTest) {
                    PropertyReadTest readTest = (PropertyReadTest) test;
                    boolean shouldWrite = info.bench.options.writeAll;
                    assert test.getProduces().size() == 1;
                    Type produces = test.getProduces().iterator().next();
                    shouldWrite |= produces instanceof SimpleType;

                    if (shouldWrite) {
                        tests.add(
                                new PropertyWriteTest(readTest.getBaseType(), produces, readTest.getProperty(), test.getPath(), test.getTypeContext())
                        );
                    }
                }
            }
        }

        if (concatDuplicates) {
            tests = concatDuplicateTests(tests);
        }

        return tests;
    }

    private boolean isRelevantPath(String path, Trie potentialPaths) {
        if (path.contains("[arg")) {
            path = path.substring(0, path.indexOf(".[arg"));
        }
        return path.contains("[arg") || potentialPaths.startsWith(TestCreator.simplifyPath(path));
    }

    public static String simplifyPath(String path) {
        int fromIndex = -1;
        while (true) {
            fromIndex = path.indexOf('(', fromIndex+1);
            if (fromIndex == -1) {
                break;
            }
            int toIndex = path.indexOf(')', fromIndex);
            assert toIndex != -1;
            path = path.substring(0, fromIndex + 1) + path.substring(toIndex, path.length());
        }
        return path;
    }

    private List<Test> addTopLevelFunctionTests(Type type, String path, TypeContext typeContext, CreateTestVisitor visitor, Set<TypeWithContext> negativeTypesSeen, Set<Type> nativeTypes, int depth, Set<TypeWithContext> seenTopLevel) {
        TypeWithContext key = new TypeWithContext(type, typeContext);
        if (seenTopLevel.contains(key)) {
            return Collections.emptyList();
        }
        seenTopLevel.add(key);

        if (info.nativeTypes.contains(type)) {
            return new ArrayList<>();
        }

        if (type instanceof SimpleType || type instanceof StringLiteral || type instanceof NumberLiteral || type instanceof BooleanLiteral || type instanceof AnonymousType || type instanceof ClassType /* The class in classType are handled in the visitor */ || type instanceof ClassInstanceType || type instanceof TupleType || type instanceof ThisType) {
            return Collections.emptyList();
        }

        if (type instanceof IndexedAccessType) {
            return Collections.emptyList();
        }

        if (type instanceof IntersectionType) {
            List<Test> result = new ArrayList<>();
            for (Type subType : ((IntersectionType) type).getElements()) {
                result.addAll(addTopLevelFunctionTests(subType, path, typeContext, visitor, negativeTypesSeen, info.nativeTypes, depth, seenTopLevel));
            }

            return result;
        }

        if (type instanceof UnionType) {
            List<Test> result = new ArrayList<>();
            List<Type> element = ((UnionType) type).getElements();
            for (int i = 0; i < element.size(); i++) {
                Type subType = element.get(i);
                result.addAll(addTopLevelFunctionTests(subType, path + ".[union" + i + "]", typeContext, visitor, negativeTypesSeen, info.nativeTypes, depth, seenTopLevel));
            }
            return result;
        }

        if (type instanceof TypeParameterType) {
            List<Test> result = new ArrayList<>();
            TypeParameterType typeParameterType = (TypeParameterType) type;
            if (typeParameterType.getConstraint() != null) {
                result.addAll(addTopLevelFunctionTests(((TypeParameterType) type).getConstraint(), path, typeContext, visitor, negativeTypesSeen, info.nativeTypes, depth, seenTopLevel));
            }
            if (typeContext.containsKey(typeParameterType)) {
                TypeWithContext lookup = typeContext.get(typeParameterType);
                result.addAll(addTopLevelFunctionTests(lookup.getType(), path, lookup.getTypeContext(), visitor, negativeTypesSeen, info.nativeTypes, depth, seenTopLevel));
            }
            return result;
        }


        if (type instanceof ReferenceType) {
            TypeContext newParameters = new TypesUtil(info).generateParameterMap((ReferenceType) type);
            type = ((ReferenceType) type).getTarget();
            path = path + ".<>";
            typeContext = typeContext.append(newParameters);
            return addTopLevelFunctionTests(type, path, typeContext, visitor, negativeTypesSeen, info.nativeTypes, depth, seenTopLevel);
        }

        if (type instanceof GenericType) {
            type = ((GenericType) type).toInterface();
            return addTopLevelFunctionTests(type, path, typeContext, visitor, negativeTypesSeen, info.nativeTypes, depth, seenTopLevel);
        }

        if (type instanceof InterfaceType) {
            List<Test> result = new ArrayList<>();
            List<Signature> callSignatures = ((InterfaceType) type).getDeclaredCallSignatures();

            List<Signature> precedingSignatures = new ArrayList<>();
            for (Signature callSignature : callSignatures) {
                List<Type> parameters = callSignature.getParameters().stream().map(Signature.Parameter::getType).collect(Collectors.toList());
                findPositiveTypesInParameters(visitor, new Arg(path, typeContext, depth), parameters);
                result.add(
                        new FunctionCallTest(type, parameters, callSignature.getResolvedReturnType(), path, typeContext, callSignature.isHasRestParameter(), new ArrayList<>(precedingSignatures))
                );
                precedingSignatures.add(callSignature);

                visitor.recurse(callSignature.getResolvedReturnType(), new Arg(path + "()", typeContext, depth + 1).withTopLevelFunctions());
            }

            precedingSignatures.clear();

            List<Signature> constructSignatures = ((InterfaceType) type).getDeclaredConstructSignatures();
            for (Signature constructSignature : constructSignatures) {
                List<Type> parameters = constructSignature.getParameters().stream().map(Signature.Parameter::getType).collect(Collectors.toList());
                findPositiveTypesInParameters(visitor, new Arg(path, typeContext, depth), parameters);
                result.add(
                        new ConstructorCallTest(type, parameters, constructSignature.getResolvedReturnType(), path, typeContext, constructSignature.isHasRestParameter(), new ArrayList<>(precedingSignatures))
                );
                precedingSignatures.add(constructSignature);

                visitor.recurse(constructSignature.getResolvedReturnType(), new Arg(path + ".new()", typeContext, depth + 1).withTopLevelFunctions());
            }
            return result;
        }
        throw new RuntimeException(type.getClass().getName());
    }

    private void findPositiveTypesInParameters(CreateTestVisitor visitor, Arg arg, List<Type> parameters) {
        for (int i = 0; i < parameters.size(); i++) {
            Type parameter = parameters.get(i);
            findPositiveTypes(visitor, parameter, arg.append("[arg" + i + "]"));
        }
    }

    private static final class TestNoPathContainer {
        private final Test test;

        public TestNoPathContainer(Test test) {
            this.test = test;
        }

        public Test getTest() {
            return test;
        }

        @Override
        public boolean equals(Object o) {
            return this == o || !(o == null || getClass() != o.getClass()) && test.equalsNoPath(((TestNoPathContainer) o).test);
        }

        @Override
        public int hashCode() {
            return test.hashCodeNoPath();
        }
    }

    private static List<Test> concatDuplicateTests(List<Test> list) {
        MultiMap<TestNoPathContainer, TestNoPathContainer> multimap = new ArrayListMultiMap<>();
        list.forEach(test -> {
            TestNoPathContainer key = new TestNoPathContainer(test);
            multimap.put(key, key);
        });
        return multimap.asMap().entrySet().stream().map(entry -> {
            if (entry.getValue().size() == 1) {
                return entry.getValue().iterator().next().getTest();
            } else {
                List<Test> tests = entry.getValue().stream().map(TestNoPathContainer::getTest).collect(Collectors.toList());
                StringBuilder newPath = new StringBuilder("(");
                for (int i = 0; i < tests.size(); i++) {
                    newPath.append(tests.get(i).getPath());
                    if (i != tests.size() - 1) {
                        newPath.append(", ");
                    }
                }
                newPath.append(")");
                Test sample = tests.iterator().next();
                sample.setPath(newPath.toString());
                return sample;
            }
        }).collect(Collectors.toList());
    }

    private static final class Arg {
        private final String path;
        private final TypeContext typeContext;
        private final int depth;
        private final boolean withTopLevelFunctions;

        private Arg(String path, TypeContext typeContext, int depth) {
            this(path, typeContext, depth, false);
        }

        private Arg(String path, TypeContext typeContext, int depth, boolean withTopLevelFunctions) {
            this.path = path;
            this.typeContext = typeContext;
            this.depth = depth;
            this.withTopLevelFunctions = withTopLevelFunctions;
        }

        private Arg append(String path) {
            return new Arg(this.path + "." + path, typeContext, depth + 1);
        }

        public TypeContext getTypeContext() {
            return typeContext;
        }

        private Arg withParameters(TypeContext newParameters) {
            return new Arg(this.path, this.typeContext.append(newParameters), depth);
        }

        public Arg withTopLevelFunctions() {
            return new Arg(this.path, this.typeContext, this.depth, true);
        }

        public Arg noTopLevelFunctions() {
            return new Arg(this.path, this.typeContext, this.depth, false);
        }

        public Arg addDepth() {
            return new Arg(this.path, this.typeContext, this.depth + 1, this.withTopLevelFunctions);
        }

        public Arg withThisType(Type classType) {
            return new Arg(this.path, this.typeContext.withThisType(classType), this.depth, this.withTopLevelFunctions);
        }

        public Arg withTypeContext(TypeContext newContext) {
            return new Arg(this.path, newContext, this.depth, this.withTopLevelFunctions);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Arg arg = (Arg) o;

            if (depth != arg.depth) return false;
            if (withTopLevelFunctions != arg.withTopLevelFunctions) return false;
            if (path != null ? !path.equals(arg.path) : arg.path != null) return false;
            return typeContext != null ? typeContext.equals(arg.typeContext) : arg.typeContext == null;
        }

        @Override
        public int hashCode() {
            int result = path != null ? path.hashCode() : 0;
            result = 31 * result + (typeContext != null ? typeContext.hashCode() : 0);
            result = 31 * result + depth;
            result = 31 * result + (withTopLevelFunctions ? 1 : 0);
            return result;
        }
    }

    private static final class TestQueueElement implements Comparable<TestQueueElement> {
        private final Type type;
        private final Arg arg;

        private TestQueueElement(Type t, Arg arg) {
            if (t == null || arg == null) {
                throw new NullPointerException();
            }
            this.type = t;
            this.arg = arg;
        }

        @Override
        public int compareTo(TestQueueElement o) {
            int result = Integer.compare(this.arg.depth, o.arg.depth);
            if (result != 0) {
                return result;
            }
            return this.arg.path.compareTo(o.arg.path);
        }
    }



    private final class CreateTestVisitor implements TypeVisitorWithArgument<Void, Arg> {
        private final Set<TypeWithContext> seen = new HashSet<>();
        private final List<Test> tests = new ArrayList<>();
        private final PriorityQueue<TestQueueElement> queue;
        private Set<TypeWithContext> negativeTypesSeen;

        private CreateTestVisitor(PriorityQueue<TestQueueElement> queue, Set<TypeWithContext> negativeTypesSeen) {
            this.queue = queue;
            this.negativeTypesSeen = negativeTypesSeen;
        }

        @Override
        public Void visit(AnonymousType t, Arg arg) {
            TypeWithContext withParameters = new TypeWithContext(t, arg.getTypeContext());
            if (seen.contains(withParameters) || info.nativeTypes.contains(t)) {
                return null;
            }
            seen.add(withParameters);

            return null;
        }

        @Override
        public Void visit(ClassType t, Arg arg) {
            TypeWithContext withParameters = new TypeWithContext(t, arg.getTypeContext());
            if (seen.contains(withParameters) || info.nativeTypes.contains(t)) {
                return null;
            }
            seen.add(withParameters);

            if (info.freeGenericsFinder.hasThisTypes(t)) {
                arg = arg.withThisType(t.getInstanceType());
            }

            for (Type baseType : t.getBaseTypes()) {
                recurse(baseType, arg.withThisType(null).addDepth());
            }

            assert !t.getSignatures().isEmpty();
            ArrayList<Signature> precedingSignatures = new ArrayList<>();
            for (Signature signature : t.getSignatures()) {
                tests.add(new ConstructorCallTest(t, signature.getParameters().stream().map(Signature.Parameter::getType).collect(Collectors.toList()), t.getInstance(), arg.path, arg.typeContext, signature.isHasRestParameter(), new ArrayList<>(precedingSignatures)));
                precedingSignatures.add(signature);
            }

            recurse(t.getInstanceType(), arg.append("new()"));

            visitProperties(t, arg.withThisType(null), t.getStaticProperties());

            return null;
        }

        @Override
        public Void visit(GenericType t, Arg arg) {
            TypeWithContext withParameters = new TypeWithContext(t, arg.getTypeContext());
            if (info.typeNames.get(t).equals("Array")) {
                assert t.getTypeParameters().size() == 1;
                TypeParameterType parameterType = (TypeParameterType) t.getTypeParameters().iterator().next();
                Type arrayType;
                if (arg.getTypeContext().containsKey(parameterType)) {
                    TypeWithContext lookup = arg.typeContext.get(parameterType);
                    arg = arg.withParameters(arg.getTypeContext());
                    arrayType = lookup.getType();
                } else {
                    arrayType = parameterType;
                }
                tests.add(new NumberIndexTest(t, arrayType, arg.path, arg.typeContext));
                recurse(arrayType, arg.append("[numberIndexer]").withTopLevelFunctions());

                return null;
            }
            if (seen.contains(withParameters) || info.nativeTypes.contains(t)) {
                return null;
            }
            seen.add(withParameters);

            if (info.freeGenericsFinder.hasThisTypes(t)) {
                arg = arg.withThisType(t);
            }

            for (Type base : t.getBaseTypes()) {
                recurse(base, arg.addDepth());
            }

            if (t.getDeclaredStringIndexType() != null) {
                tests.add(new StringIndexTest(t, t.getDeclaredStringIndexType(), arg.path, arg.typeContext));
                recurse(t.getDeclaredStringIndexType(), arg.append("[stringIndexer]").withTopLevelFunctions());
            }
            if (t.getDeclaredNumberIndexType() != null) {
                tests.add(new NumberIndexTest(t, t.getDeclaredNumberIndexType(), arg.path, arg.typeContext));
                recurse(t.getDeclaredNumberIndexType(), arg.append("[numberIndexer]").withTopLevelFunctions());
            }

            Map<String, Type> properties = t.getDeclaredProperties();
            visitProperties(t, arg, properties);

            return null;
        }


        @Override
        public Void visit(InterfaceType t, Arg arg) {
            TypeWithContext withParameters = new TypeWithContext(t, arg.getTypeContext());
            if (seen.contains(withParameters) || info.nativeTypes.contains(t)) {
                return null;
            }
            seen.add(withParameters);

            if (info.freeGenericsFinder.hasThisTypes(t)) {
                arg = arg.withThisType(t);
            }

            TypesUtil typesUtil = new TypesUtil(info);

            for (TypeWithContext stringIndexer : typesUtil.getAllStringIndexerTypes(t, arg.typeContext)) {
                Arg subArg = arg.withTypeContext(stringIndexer.getTypeContext());
                tests.add(new StringIndexTest(t, stringIndexer.getType(), arg.path, subArg.typeContext));
                recurse(stringIndexer.getType(), subArg.append("[stringIndexer]").withTopLevelFunctions());
            }

            for (TypeWithContext stringIndexer : typesUtil.getAllNumberIndexerTypes(t, arg.typeContext)) {
                Arg subArg = arg.withTypeContext(stringIndexer.getTypeContext());
                tests.add(new NumberIndexTest(t, stringIndexer.getType(), arg.path, subArg.typeContext));
                recurse(stringIndexer.getType(), subArg.append("[numberIndexer]").withTopLevelFunctions());
            }

            for (Pair<TypeContext, Map<String, Type>> propertiesPair : typesUtil.getAllPropertyDeclarations(t, arg.typeContext)) {
                Arg subArg = arg.withTypeContext(propertiesPair.getLeft());
                visitProperties(t, subArg, propertiesPair.getRight());
            }

            return null;
        }

        private void visitProperties(Type t, Arg arg, Map<String, Type> properties) {
            for (Map.Entry<String, Type> entry : properties.entrySet()) {
                String key = entry.getKey();
                Type type = entry.getValue();

                tests.add(new PropertyReadTest(t, type, key, arg.path, arg.getTypeContext()));

                addMethodCallTest(t, arg, key, type, new HashSet<>());

                recurse(type, arg.append(key));
            }
        }

        private void addMethodCallTest(Type baseType, Arg arg, String key, Type propertyType, Set<Tuple3<Type, TypeContext, Type>> seen) {
            Tuple3<Type, TypeContext, Type> seenKey = new Tuple3<>(baseType, arg.typeContext, propertyType);
            if (seen.contains(seenKey)) {
                return;
            }
            seen.add(seenKey);

            if (propertyType instanceof InterfaceType) {
                List<Signature> callSignatures = ((InterfaceType) propertyType).getDeclaredCallSignatures();
                List<Signature> precedingSignatures = new ArrayList<>();
                for (Signature signature : callSignatures) {
                    List<Type> parameters = signature.getParameters().stream().map(Signature.Parameter::getType).collect(Collectors.toList());
                    findPositiveTypesInParameters(this, arg.append(key), parameters);
                    tests.add(new MethodCallTest(baseType, propertyType, key, parameters, signature.getResolvedReturnType(), arg.append(key).path, arg.getTypeContext(), signature.isHasRestParameter(), new ArrayList<>(precedingSignatures)));
                    precedingSignatures.add(signature);

                    recurse(signature.getResolvedReturnType(), arg.append(key + "()").addDepth().withTopLevelFunctions());
                }

                precedingSignatures.clear();
                List<Signature> constructSignatures = ((InterfaceType) propertyType).getDeclaredConstructSignatures();
                for (Signature signature : constructSignatures) {
                    List<Type> parameters = signature.getParameters().stream().map(Signature.Parameter::getType).collect(Collectors.toList());
                    findPositiveTypesInParameters(this, arg.append(key), parameters);
                    tests.add(new ConstructorCallTest(propertyType, parameters, signature.getResolvedReturnType(), arg.append(key).path, arg.getTypeContext(), signature.isHasRestParameter(), new ArrayList<>(precedingSignatures)));
                    precedingSignatures.add(signature);

                    recurse(signature.getResolvedReturnType(), arg.append(key + ".new()").addDepth().withTopLevelFunctions());
                }
                return;
            }

            if (propertyType instanceof GenericType) {
                addMethodCallTest(baseType, arg, key, ((GenericType) propertyType).toInterface(), seen);
                return;
            }
            if (propertyType instanceof ClassType) {
                return;
            }
            if (propertyType  instanceof TypeParameterType) {
                TypeParameterType typeParameterType = (TypeParameterType) propertyType ;
                if (typeParameterType.getConstraint() != null) {
                    addMethodCallTest(baseType, arg, key, ((TypeParameterType) propertyType ).getConstraint(), seen);
                }
                if (arg.typeContext.containsKey(typeParameterType)) {
                    TypeWithContext lookup = arg.typeContext.get(typeParameterType);
                    addMethodCallTest(baseType, arg.withParameters(lookup.getTypeContext()), key, lookup.getType(), seen);
                }
                return;
            }
            if (propertyType instanceof SimpleType || propertyType instanceof StringLiteral || propertyType instanceof BooleanLiteral || propertyType instanceof NumberLiteral || propertyType instanceof ThisType || propertyType instanceof TupleType) {
                return;
            }

            if (propertyType instanceof ReferenceType) {
                TypeContext newParameters = new TypesUtil(info).generateParameterMap((ReferenceType) propertyType);
                Type subType = ((ReferenceType) propertyType).getTarget();
                Arg newArg = arg.append("<>").withParameters(newParameters);
                addMethodCallTest(baseType, newArg, key, subType, seen);
                return;
            }

            if (propertyType instanceof ClassInstanceType) {
                return;
            }

            if (propertyType instanceof UnionType) {
                List<Type> elements = ((UnionType) propertyType).getElements();
                for (int i = 0; i < elements.size(); i++) {
                    Type type = elements.get(i);
                    addMethodCallTest(baseType, arg.append("[union" + i + "]"), key, type, seen);
                }
                return;
            }

            if (propertyType instanceof IntersectionType) {
                List<Type> elements = ((IntersectionType) propertyType).getElements();
                for (int i = 0; i < elements.size(); i++) {
                    Type type = elements.get(i);
                    addMethodCallTest(baseType, arg.append("[intersection" + i + "]"), key, type, seen);
                }
                return;
            }


            throw new RuntimeException(propertyType.getClass().getName());
        }

        @Override
        public Void visit(ReferenceType t, Arg arg) {
            TypeWithContext withParameters = new TypeWithContext(t, arg.getTypeContext());
            if (seen.contains(withParameters) || (info.nativeTypes.contains(t) && !("Array".equals(info.typeNames.get(t.getTarget()))))) {
                return null;
            }
            seen.add(withParameters);

            TypeContext newParameters = new TypesUtil(info).generateParameterMap(t);

            recurse(t.getTarget(), arg.append("<>").withParameters(newParameters));

            return null;
        }

        private Void recurse(Type type, Arg arg) {
            if (type instanceof ThisType && arg.getTypeContext().getThisType() == null) {
                System.out.println();
            }
            queue.add(new TestQueueElement(type, arg));
            return null;
        }

        @Override
        public Void visit(SimpleType t, Arg arg) {
            return null;
        }

        @Override
        public Void visit(TupleType tuple, Arg arg) {
            TypeWithContext withParameters = new TypeWithContext(tuple, arg.getTypeContext());
            if (seen.contains(withParameters)/* || info.nativeTypes.contains(tuple)*/) { // TupleTypes for some weird reason ends up as the result of en Array's map function.
                return null;
            }
            seen.add(withParameters);

            for (int i = 0; i < tuple.getElementTypes().size(); i++) {
                Type type = tuple.getElementTypes().get(i);
                tests.add(new PropertyReadTest(tuple, type, Integer.toString(i), arg.path, arg.typeContext));
                recurse(type, arg.append(Integer.toString(i)));
            }

            return null;
        }

        @Override
        public Void visit(UnionType union, Arg arg) {
            TypeWithContext withParameters = new TypeWithContext(union, arg.getTypeContext());
            if (seen.contains(withParameters)) { /* || info.nativeTypes.contains(union)) { sometimes union-types ends up in the native-types thing, i just test all of em. */
                return null;
            }
            seen.add(withParameters);

            List<Type> elements = union.getElements();

            if (elements.size() == 0) {
                return null;
            }

            tests.add(new UnionTypeTest(union, union.getElements(), arg.path, arg.typeContext));

            for (int i = 0; i < union.getElements().size(); i++) {
                Type type = union.getElements().get(i);
                recurse(type, arg.append("[union" + i + "]"));
            }

            return null;
        }

        @Override
        public Void visit(TypeParameterType t, Arg arg) {
            TypeWithContext withParameters = new TypeWithContext(t, arg.getTypeContext());
            if (seen.contains(withParameters) || info.nativeTypes.contains(t)) {
                return null;
            }
            seen.add(withParameters);

            if (arg.getTypeContext().containsKey(t)) {
                TypeWithContext lookup = arg.getTypeContext().get(t);
                arg = arg.withParameters(lookup.getTypeContext());
                recurse(lookup.getType(), arg);
            } else if (t.getConstraint() != null) {
                tests.add(new FilterTest(t, t.getConstraint(), arg.path, arg.getTypeContext(), Check.alwaysTrue()));
                recurse(t.getConstraint(), arg.append("[constraint]"));
            }

            return null;
        }

        @Override
        public Void visit(StringLiteral t, Arg arg) {
            return null;
        }

        @Override
        public Void visit(BooleanLiteral t, Arg arg) {
            return null;
        }

        @Override
        public Void visit(NumberLiteral t, Arg arg) {
            return null;
        }

        @Override
        public Void visit(IntersectionType t, Arg arg) {
            TypeWithContext withParameters = new TypeWithContext(t, arg.getTypeContext());
            if (seen.contains(withParameters) || info.nativeTypes.contains(t)) {
                return null;
            }
            seen.add(withParameters);


            for (int i = 0; i < t.getElements().size(); i++) {
                Type subType = t.getElements().get(i);
                tests.add(new FilterTest(t, subType, arg.path, arg.getTypeContext(), Check.alwaysTrue()));
                recurse(subType, arg.append("[intersection" + i + "]"));
            }

            return null;
        }

        @Override
        public Void visit(ClassInstanceType t, Arg arg) {
            InterfaceType instanceType = ((ClassType) t.getClassType()).getInstanceType();
            // tests.add(new FilterTest(t, instanceType, arg.path, arg.typeContext, Check.alwaysTrue())); // Not needed, the TypeCreator will make sure the actual InstanceType is found.

            recurse(instanceType, arg);
            return null;
        }

        @Override
        public Void visit(ThisType t, Arg arg) {
            return recurse(arg.typeContext.getThisType(), arg);
        }

        @Override
        public Void visit(IndexType t, Arg arg) {
            throw new RuntimeException();
        }

        @Override
        public Void visit(IndexedAccessType t, Arg arg) {
            throw new RuntimeException();
        }

        public Collection<Test> getTests() {
            return tests;
        }
    }

    private void findPositiveTypes(CreateTestVisitor visitor, Type type, Arg arg) {
        PriorityQueue<TestQueueElement> queue = new PriorityQueue<>();

        FindPositiveTypesVisitor findPositiveVisitor = new FindPositiveTypesVisitor(visitor, info, queue);
        queue.add(new TestQueueElement(type, arg));

        while (!queue.isEmpty()) {
            TestQueueElement element = queue.poll();
            arg = element.arg;

            arg = arg.withTypeContext(arg.typeContext.optimizeTypeParameters(element.type));

            if (info.freeGenericsFinder.hasThisTypes(element.type)) {
                arg = arg.withThisType(element.type);
            }

            if (visitor.negativeTypesSeen.contains(new TypeWithContext(element.type, arg.typeContext))) {
                continue;
            }
            visitor.negativeTypesSeen.add(new TypeWithContext(element.type, arg.typeContext));

            element.type.accept(findPositiveVisitor, arg);
        }
    }

    // getContexts(visitor.negativeTypesSeen, element.type).stream().map(TypeContext::getMap).filter(map -> map.size() == 1).map(Map::entrySet).map(entries -> entries.iterator().next()).collect(Collectors.toList())
    private static List<TypeContext> getContexts(Collection<TypeWithContext> list, Type type) {
        return list.stream().filter(tc -> tc.getType().equals(type)).map(TypeWithContext::getTypeContext).collect(Collectors.toList());
    }

    private static class FindPositiveTypesVisitor implements TypeVisitorWithArgument<Void, Arg> {
        private final CreateTestVisitor visitor;
        private final BenchmarkInfo info;
        private PriorityQueue<TestQueueElement> queue;

        public FindPositiveTypesVisitor(
                CreateTestVisitor createTestVisitor,
                BenchmarkInfo info,
                PriorityQueue<TestQueueElement> queue) {
            this.visitor = createTestVisitor;
            this.info = info;
            this.queue = queue;
        }

        private Void recurse(Type type, Arg arg) {
            this.queue.add(new TestQueueElement(type, arg));
            return null;
        }

        @Override
        public Void visit(AnonymousType t, Arg arg) {
            return null;
        }

        @Override
        public Void visit(ClassType t, Arg arg) {
            if (info.nativeTypes.contains(t)) {
                return null;
            }

            recurse(t.getInstanceType(), arg.append("new()"));

            assert !t.getSignatures().isEmpty();

            for (Signature signature : t.getSignatures()) {
                for (int i = 0; i < signature.getParameters().size(); i++) {
                    Signature.Parameter parameter = signature.getParameters().get(i);
                    visitor.recurse(parameter.getType(), arg.append("[arg" + i + "]").withTopLevelFunctions());
                }
            }

            for (Type baseType : t.getBaseTypes()) {
                recurse(baseType, arg.addDepth());
            }

            if (t.getDeclaredStringIndexType() != null) {
                recurse(t.getDeclaredStringIndexType(), arg.append("[stringIndexer]"));
            }
            if (t.getDeclaredNumberIndexType() != null) {
                recurse(t.getDeclaredNumberIndexType(), arg.append("[numberIndexer"));
            }

            for (Map.Entry<String, Type> entry : t.getStaticProperties().entrySet()) {
                recurse(entry.getValue(), arg.append(entry.getKey()));
            }

            return null;
        }

        @Override
        public Void visit(GenericType t, Arg arg) {
            if (info.typeNames.get(t).equals("Array")) {
                assert t.getTypeParameters().size() == 1;
                TypeParameterType parameterType = (TypeParameterType) t.getTypeParameters().iterator().next();
                Type arrayType;
                if (arg.getTypeContext().containsKey(parameterType)) {
                    TypeWithContext lookup = arg.typeContext.get(parameterType);
                    arg = arg.withParameters(arg.getTypeContext());
                    arrayType = lookup.getType();
                } else {
                    arrayType = parameterType;
                }
                recurse(arrayType, arg.append("[numberIndexer]").withTopLevelFunctions());

                return null;
            }

            if (info.nativeTypes.contains(t)) {
                return null;
            }

            if (info.freeGenericsFinder.hasThisTypes(t)) {
                arg = arg.withThisType(t);
            }

            assert t.getTypeParameters().equals(t.getTypeArguments()); // If this fails, look at the other visitor.
            recurse(t.toInterface(), arg);
            return null;
        }

        @Override
        public Void visit(InterfaceType t, Arg arg) {
            if (info.nativeTypes.contains(t)) {
                return null;
            }

            if (info.freeGenericsFinder.hasThisTypes(t)) {
                arg = arg.withThisType(t);
            }

            for (Signature signature : Util.concat(t.getDeclaredCallSignatures(), t.getDeclaredConstructSignatures())) {
                for (int i = 0; i < signature.getParameters().size(); i++) {
                    Signature.Parameter parameter = signature.getParameters().get(i);
                    visitor.recurse(parameter.getType(), arg.append("[arg" + i + "]").withTopLevelFunctions());
                }
                recurse(signature.getResolvedReturnType(), arg.append("()"));
            }

            for (Type baseType : t.getBaseTypes()) {
                recurse(baseType, arg.addDepth());
            }

            if (t.getDeclaredStringIndexType() != null) {
                recurse(t.getDeclaredStringIndexType(), arg.append("[stringIndexer]"));
            }
            if (t.getDeclaredNumberIndexType() != null) {
                recurse(t.getDeclaredNumberIndexType(), arg.append("[numberIndexer"));
            }
            for (Map.Entry<String, Type> entry : t.getDeclaredProperties().entrySet()) {
                recurse(entry.getValue(), arg.append(entry.getKey()));
            }

            return null;
        }

        @Override
        public Void visit(ReferenceType t, Arg arg) {
            if (info.nativeTypes.contains(t)) {
                return null;
            }

            TypeContext newParameters = new TypesUtil(info).generateParameterMap(t);

            recurse(t.getTarget(), arg.append("<>").withParameters(newParameters));

            return null;
        }

        @Override
        public Void visit(SimpleType t, Arg arg) {
            return null;
        }

        @Override
        public Void visit(TupleType t, Arg arg) {
            if (false /* || info.nativeTypes.contains(tuple)*/) { // TupleTypes for some weird reason ends up as the result of en Array's map function.
                return null;
            }

            for (int i = 0; i < t.getElementTypes().size(); i++) {
                Type type = t.getElementTypes().get(i);
                recurse(type, arg.append(Integer.toString(i)));
            }

            return null;
        }

        @Override
        public Void visit(UnionType union, Arg arg) {
            if (info.nativeTypes.contains(union)) {
                return null;
            }

            for (int i = 0; i < union.getElements().size(); i++) {
                Type type = union.getElements().get(i);
                recurse(type, arg.append("[union" + i + "]"));
            }
            return null;
        }

        @Override
        public Void visit(TypeParameterType t, Arg arg) {
            if (info.nativeTypes.contains(t)) {
                return null;
            }

            if (arg.getTypeContext().containsKey(t)) {
                TypeWithContext lookup = arg.getTypeContext().get(t);
                recurse(lookup.getType(), arg.withParameters(lookup.getTypeContext()));
            } else if (t.getConstraint() != null) {
                recurse(t.getConstraint(), arg.append("[constraint]"));
            }
            return null;
        }

        @Override
        public Void visit(StringLiteral t, Arg arg) {
            return null;
        }

        @Override
        public Void visit(BooleanLiteral t, Arg arg) {
            return null;
        }

        @Override
        public Void visit(NumberLiteral t, Arg arg) {
            return null;
        }

        @Override
        public Void visit(IntersectionType intersection, Arg arg) {
            if (info.nativeTypes.contains(intersection)) {
                return null;
            }

            for (int i = 0; i < intersection.getElements().size(); i++) {
                Type type = intersection.getElements().get(i);
                recurse(type, arg.append("[intersection" + i + "]"));
            }
            return null;
        }

        @Override
        public Void visit(ClassInstanceType t, Arg arg) {
            return recurse(((ClassType) t.getClassType()).getInstanceType(), arg);
        }

        @Override
        public Void visit(ThisType t, Arg arg) {
            return recurse(arg.getTypeContext().getThisType(), arg);
        }

        @Override
        public Void visit(IndexType t, Arg arg) {
            return recurse(t.getType(), arg.append("[index]"));
        }

        @Override
        public Void visit(IndexedAccessType t, Arg arg) {
            recurse(t.getObjectType(), arg.append("[objectType]"));
            recurse(t.getIndexType(), arg.append("[indexType]"));
            return null;
        }
    }
}
