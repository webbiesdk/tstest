package dk.webbies.tajscheck.buildprogram;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import dk.au.cs.casa.typescript.SpecReader;
import dk.au.cs.casa.typescript.types.*;
import dk.au.cs.casa.typescript.types.BooleanLiteral;
import dk.au.cs.casa.typescript.types.NumberLiteral;
import dk.au.cs.casa.typescript.types.StringLiteral;
import dk.webbies.tajscheck.TypeWithContext;
import dk.webbies.tajscheck.benchmark.BenchmarkInfo;
import dk.webbies.tajscheck.benchmark.CheckOptions;
import dk.webbies.tajscheck.paser.AST.*;
import dk.webbies.tajscheck.paser.AstBuilder;
import dk.webbies.tajscheck.testcreator.test.FunctionTest;
import dk.webbies.tajscheck.testcreator.test.Test;
import dk.webbies.tajscheck.typeutil.TypesUtil;
import dk.webbies.tajscheck.typeutil.typeContext.OptimizingTypeContext;
import dk.webbies.tajscheck.typeutil.typeContext.TypeContext;
import dk.webbies.tajscheck.util.*;
import org.apache.commons.lang3.RandomStringUtils;

import java.util.*;
import java.util.stream.Collectors;

import static dk.webbies.tajscheck.buildprogram.DriverProgramBuilder.*;
import static dk.webbies.tajscheck.paser.AstBuilder.*;

/**
 * Created by erik1 on 03-11-2016.
 */
public class TypeCreator {
    private final BiMap<TypeWithContext, Integer> typeIndexes;
    private final MultiMap<TypeWithContext, Integer> valueLocations;
    private final Map<Test, List<Integer>> testValueLocations = new IdentityHashMap<>();
    private final CheckOptions options;
    private TypeChecker typeChecker;
    private final BenchmarkInfo info;
    private ArrayList<Statement> functions = new ArrayList<>();

    private static final String GET_TYPE_PREFIX = "getType_";
    private static final String CONSTRUCT_TYPE_PREFIX = "constructType_";
    private List<Statement> valueVariableDeclarationList = new ArrayList<>();

    TypeCreator(List<Test> tests, BenchmarkInfo info, TypeChecker typeChecker) {
        this.options = info.options;
        this.typeChecker = typeChecker;
        this.valueLocations = new ArrayListMultiMap<>();
        this.typeIndexes = HashBiMap.create();
        this.info = info;

        for (Test test : tests) {
            List<Integer> testValueLocations = new ArrayList<>();
            this.testValueLocations.put(test, testValueLocations);
            for (Type type : test.getProduces()) {
                testValueLocations.add(createProducedValueVariable(type, test.getTypeContext()));
            }
        }

        for (Test test : tests) {
            // Forcing all these to be created ahead of time
            for (Type dependsOn : test.getDependsOn()) {
                constructType(dependsOn, test.getTypeContext());
            }
            for (Type type : test.getTypeToTest()) {
                getType(type, test.getTypeContext());
            }
            if (test instanceof FunctionTest && info.bench.options.firstMatchSignaturePolicy) {
                if (!((FunctionTest) test).getPrecedingSignatures().isEmpty()) {
                    InterfaceType precedingSignaturesInterface = TypesUtil.signaturesToInterface(((FunctionTest) test).getPrecedingSignatures(), info.typeNames);
                    constructType(precedingSignaturesInterface, test.getTypeContext());
                    precedingSignaturesType.put((FunctionTest) test, getTypeIndex(precedingSignaturesInterface, test.getTypeContext()));
                }
            }
        }
        finish();
    }

    private final Map<FunctionTest, Integer> precedingSignaturesType = new IdentityHashMap<>();

    public int getPrecedingSignaturesType(FunctionTest test) {
        return precedingSignaturesType.get(test);
    }

    public List<Integer> getTestProducesIndexes(Test test) {
        return testValueLocations.get(test);
    }


    public Collection<Integer> getValueIndex(Type type, TypeContext context) {
        return valueLocations.get(new TypeWithContext(type, context));
    }

    private int valueCounter = 0;

    private int createProducedValueVariable(Type type, TypeContext typeContext) {
        int index = valueCounter++;
        valueVariableDeclarationList.add(variable(VALUE_VARIABLE_PREFIX + index, identifier(VARIABLE_NO_VALUE)));

        putProducedValueIndex(index, type, typeContext);
        return index;
    }

    public Collection<Statement> getValueVariableDeclarationList() {
        return valueVariableDeclarationList;
    }

    private void putProducedValueIndex(int index, Type type, TypeContext typeContext) {
        putProducedValueIndex(index, type, typeContext, false);
    }


    private final Set<Tuple4<Integer, Type, TypeContext, Boolean>> seenPutValue = new HashSet<>();
    private void putProducedValueIndex(int index, Type type, TypeContext typeContext, boolean touchedThisTypes) {
        Tuple4<Integer, Type, TypeContext, Boolean> seenKey = new Tuple4<>(index, type, typeContext, touchedThisTypes);
        if (seenPutValue.contains(seenKey)) {
            return;
        }
        seenPutValue.add(seenKey);

        valueLocations.put(new TypeWithContext(type, typeContext), index);

        if (!touchedThisTypes) {
            if (typeContext.getThisType() != null) {
                putProducedValueIndex(index, type, typeContext.withThisType(null), true);
            }
            if (typeContext.getThisType() == null) {
                if (info.freeGenericsFinder.hasThisTypes(type) && !(type instanceof ClassType)) {
                    putProducedValueIndex(index, type, typeContext.withThisType(type), true);
                }
            }
        }

        TypeContext newContext = typeContext.optimizeTypeParameters(type);
        if (!newContext.equals(typeContext)) {
            putProducedValueIndex(index, type, newContext);
        }

        if (type instanceof InterfaceType) {
            List<Type> baseTypes = ((InterfaceType) type).getBaseTypes();
            baseTypes.forEach(baseType -> putProducedValueIndex(index, baseType, typeContext));
        } else if (type instanceof IntersectionType) {
            List<Type> baseTypes = ((IntersectionType) type).getElements();
            baseTypes.forEach(baseType -> putProducedValueIndex(index, baseType, typeContext));
        } else if (type instanceof ReferenceType) {
            putProducedValueIndex(index, ((ReferenceType) type).getTarget(), new TypesUtil(info).generateParameterMap((ReferenceType) type, typeContext));
        } else if (type instanceof GenericType) {
            putProducedValueIndex(index, ((GenericType) type).toInterface(), typeContext);
        } else if (type instanceof ClassType) {
            valueLocations.put(new TypeWithContext(type, typeContext.withThisType(((ClassType) type).getInstanceType())), index);
        } else if (type instanceof ClassInstanceType) {
            ClassInstanceType instanceType = (ClassInstanceType) type;

            if (instanceType != ((ClassType) instanceType.getClassType()).getInstance()) {
                putProducedValueIndex(index, ((ClassType) instanceType.getClassType()).getInstance(), typeContext, touchedThisTypes);
            }

            putProducedValueIndex(index, ((ClassType) instanceType.getClassType()).getInstanceType(), typeContext);
            if (info.freeGenericsFinder.hasThisTypes(instanceType.getClassType())) {
                putProducedValueIndex(index, ((ClassType) instanceType.getClassType()).getInstanceType(), typeContext.withThisType(instanceType));
            }

            for (Type baseClass : ((ClassType) instanceType.getClassType()).getBaseTypes()) {
                TypeContext subTypeContext = typeContext;
                if (baseClass instanceof ReferenceType) {
                    subTypeContext = new TypesUtil(info).generateParameterMap((ReferenceType) baseClass, typeContext);
                    baseClass = ((ReferenceType) baseClass).getTarget();
                } else if (baseClass instanceof ClassInstanceType) {
                    baseClass = ((ClassInstanceType) baseClass).getClassType();
                }
                if (baseClass instanceof ClassType) {
                    baseClass = ((ClassType) baseClass).getInstance();
                } else if (!(baseClass instanceof InterfaceType || baseClass instanceof GenericType || baseClass instanceof ClassInstanceType)) {
                    throw new RuntimeException("Not sure about: " + baseClass.getClass().getSimpleName());
                }
                putProducedValueIndex(index, baseClass, subTypeContext, touchedThisTypes);
            }

        } else if (type instanceof ThisType) {
            Type thisType = typeContext.getThisType();
            putProducedValueIndex(index, thisType != null ? thisType : ((ThisType) type).getConstraint(), typeContext);
        } else if (type instanceof TypeParameterType) {
            if (typeContext.get((TypeParameterType) type) != null) {
                TypeWithContext lookup = typeContext.get((TypeParameterType) type);
                putProducedValueIndex(index, lookup.getType(), lookup.getTypeContext());
            } else {
                // Do nothing
            }
        } else if (type instanceof SimpleType || type instanceof NumberLiteral || type instanceof StringLiteral || type instanceof BooleanLiteral || type instanceof UnionType || type instanceof TupleType || type instanceof IndexedAccessType) {
            // Do nothing.
        } else {
            throw new RuntimeException(type.getClass().getName());
        }

    }


    private Statement constructUnion(List<Type> types, TypeContext typeContext) {
        List<Integer> elements = types.stream().distinct().map((type) -> getTypeIndex(type, typeContext)).collect(Collectors.toList());

        if (elements.size() == 1) {
            return Return(constructType(elements.iterator().next()));
        }

        List<Pair<Expression, Statement>> cases = Util.withIndex(elements).map(pair -> {
            return new Pair<Expression, Statement>(number(pair.getRight()), Return(constructType(pair.getLeft())));
        }).collect(Collectors.toList());

        return block(
                switchCase(
                        binary(binary(call(identifier("random")), Operator.MULT, number(elements.size())), Operator.BITWISE_OR, number(0)),
                        cases,
                        block(
                                comment("Unreachable"),
                                Return(constructType(elements.iterator().next()))
                        )
                )
        );
    }

    private Statement returnOneOfExistingValues(Collection<Integer> elementsCollection) {
        List<Integer> elements = new ArrayList<>(elementsCollection);
        if (elements.size() == 1) {
            Integer index = elements.iterator().next();
            return Return(identifier(VALUE_VARIABLE_PREFIX + index));
        }

        List<Pair<Expression, Statement>> cases = Util.withIndex(elements).map(pair -> {
            Expression getValue = identifier(VALUE_VARIABLE_PREFIX + pair.getLeft());
            return new Pair<Expression, Statement>(number(pair.getRight()), block(
                    variable("result", getValue),
                    ifThen(binary(identifier("result"), Operator.NOT_EQUAL_EQUAL, identifier(VARIABLE_NO_VALUE)), Return(identifier("result")))
            ));
        }).collect(Collectors.toList());

        return block(
                switchCase(
                        binary(binary(call(identifier("random")), Operator.MULT, number(elements.size())), Operator.BITWISE_OR, number(0)),
                        cases
                ),
                // If the switch fails to return, check if anything can be returned.
                block(cases.stream().map(Pair::getRight).collect(Collectors.toList())),
                // If nothing has been returned, return the NO_VALUE object.
                Return(identifier(DriverProgramBuilder.VARIABLE_NO_VALUE))
        );
    }

    private final class ConstructNewInstanceVisitor implements TypeVisitorWithArgument<Statement, TypeContext> {
        @Override
        public Statement visit(AnonymousType t, TypeContext typeContext) {
            throw new RuntimeException();
        }

        @Override
        public Statement visit(ClassType t, TypeContext typeContext) {
            if (info.freeGenericsFinder.hasThisTypes(t)) {
                typeContext = typeContext.withThisType(t.getInstanceType());
            }

            assert t.getSignatures().size() > 0;

            List<Statement> addProperties = new ArrayList<>();

            List<Signature> signatures = t.getSignatures().stream().map(sig -> TypesUtil.createConstructorSignature(t, sig)).collect(Collectors.toList());

            Pair<InterfaceType, TypeContext> pair = new TypesUtil(info).constructSyntheticInterfaceWithBaseTypes(TypesUtil.classToInterface(t, info.freeGenericsFinder), info.typeNames, info.freeGenericsFinder);
            InterfaceType inter = pair.getLeft();
            typeContext = typeContext.append(pair.getRight());

            if (inter.getDeclaredNumberIndexType() != null) {
                addProperties.addAll(addNumberIndexerType(inter.getDeclaredNumberIndexType(), typeContext, identifier("result"), info.typeNames.get(t).hashCode()));
            }

            if (inter.getDeclaredStringIndexType() != null) {
                addProperties.addAll(addStringIndexerType(inter.getDeclaredStringIndexType(), typeContext, identifier("result"), inter.getDeclaredProperties().keySet(), info.typeNames.get(t).hashCode()));
            }

            List<Pair<String, Type>> properties = inter.getDeclaredProperties().entrySet().stream().map(entry -> new Pair<>(entry.getKey(), entry.getValue())).collect(Collectors.toList());

            addProperties.addAll(addProperties(typeContext, properties, identifier("result")));

            return createCachedConstruction(
                    Return(createFunction(signatures, typeContext, info.typeNames.get(t))),
                    block(addProperties),
                    1
            );
        }

        int propertyCounter = 0;
        private List<Statement> addProperties(TypeContext typeContext, List<Pair<String, Type>> properties, Expression exp) {
            List<Statement> addProperties = new ArrayList<>();
            for (Pair<String, Type> property : properties) {
                int count = propertyCounter++;
                addProperties.add(block(
                        variable("prop_" + count, constructType(property.getRight(), typeContext)),
                        ifThen(
                                binary(unary(Operator.TYPEOF, identifier("prop_" + count)), Operator.NOT_EQUAL_EQUAL, string("undefined")),
                                statement(binary(member(exp, property.getLeft()), Operator.EQUAL, identifier("prop_" + count)))
                        )
                ));
            }
            return addProperties;
        }

        @Override
        public Statement visit(GenericType type, TypeContext typeContext) {
            assert type.getTypeParameters().equals(type.getTypeArguments());
            if (info.nativeTypes.contains(type)) {
                try {
                    return constructTypeFromName(info.typeNames.get(type), typeContext, type.getTypeParameters());
                } catch (ProduceManuallyException e) {
                    // continue
                }
            }

            return type.toInterface().accept(this, typeContext);
        }

        @Override
        public Statement visit(InterfaceType type, TypeContext typeContext) {
            if (info.nativeTypes.contains(type) && !TypesUtil.isEmptyInterface(type) && info.typeNames.get(type) != null &&  !info.typeNames.get(type).startsWith("window.")) {
                try {
                    return constructTypeFromName(info.typeNames.get(type), typeContext, type.getTypeParameters());
                } catch (ProduceManuallyException e) {
                    // continue
                }
            }

            if (info.freeGenericsFinder.hasThisTypes(type)) {
                typeContext = typeContext.withThisType(type);
            }

            Pair<InterfaceType, TypeContext> pair = new TypesUtil(info).constructSyntheticInterfaceWithBaseTypes(type, info.typeNames, info.freeGenericsFinder);
            InterfaceType inter = pair.getLeft();
            typeContext = typeContext.append(pair.getRight());
            assert inter.getBaseTypes().isEmpty();

            int numberOfSignatures = inter.getDeclaredCallSignatures().size() + inter.getDeclaredConstructSignatures().size();

            Expression constructInitial;
            if (numberOfSignatures == 0) {
                Type nativebase = TypesUtil.getNativeBase(type, info.nativeTypes, info.typeNames);
                if (nativebase != null) {
                    constructInitial = constructType(nativebase, typeContext);
                } else {
                    constructInitial = object();
                }
            } else {
                constructInitial = createFunction(inter, typeContext);
            }

            List<Statement> addProperties = new ArrayList<>();

            if (inter.getDeclaredNumberIndexType() != null) {
                addProperties.addAll(addNumberIndexerType(inter.getDeclaredNumberIndexType(), typeContext, identifier("result"), info.typeNames.get(type).hashCode()));
            }

            if (inter.getDeclaredStringIndexType() != null) {
                addProperties.addAll(addStringIndexerType(inter.getDeclaredStringIndexType(), typeContext, identifier("result"), inter.getDeclaredProperties().keySet(), info.typeNames.get(type).hashCode()));
            }

            List<Pair<String, Type>> properties = inter.getDeclaredProperties().entrySet().stream().map(entry -> new Pair<>(entry.getKey(), entry.getValue())).collect(Collectors.toList());

            addProperties.addAll(addProperties(typeContext, properties, identifier("result")));


            return createCachedConstruction(
                    Return(constructInitial),
                    block(addProperties),
                    0.2
            );
        }

        private Collection<Statement> addStringIndexerType(Type type, TypeContext context, Expression exp, Set<String> existingKeys, int seed) {
            char[] chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
            Random random = new Random(seed); // I need this to be completely deterministic.

            int keys = random.nextInt(10) + 1;

            List<Pair<String, Type>> properties = new ArrayList<>();


            for (int i = 0; i < keys; i++) {
                String key;
                //noinspection StatementWithEmptyBody
                while (existingKeys.contains(key = RandomStringUtils.random(random.nextInt(10) + 1, chars))) {
                    // do nothing.
                }

                properties.add(new Pair<>(key, type));
            }

            return addProperties(context, properties, exp);
        }

        private Collection<Statement> addNumberIndexerType(Type type, TypeContext context, Expression exp, int seed) {
            Random random = new Random(seed);

            List<Pair<String, Type>> properties = new ArrayList<>();

            int keys = random.nextInt(10) + 1;
            for (int i = 0; i < keys; i++) {
                properties.add(new Pair<>(Integer.toString(random.nextInt(100)), type));
            }

            return addProperties(context, properties, exp);
        }

        @Override
        public Statement visit(ReferenceType type, TypeContext typeContext) {
            if ("Array".equals(info.typeNames.get(type.getTarget()))) {
                Type indexType = type.getTypeArguments().iterator().next();
                return constructArray(typeContext, indexType);
            }

            return Return(constructType(type.getTarget(), new TypesUtil(info).generateParameterMap(type, typeContext)));
        }

        @Override
        public Statement visit(SimpleType simple, TypeContext typeContext) {
            switch (simple.getKind()) {
                case String:
                    return AstBuilder.stmtFromString(
                            "var result = \"\";\n" +
                            "var prop = 1;\n" +
                            "while((0.97 * prop) > Math.random()) {\n" +
                            "    if (Math.random() > 0.1) {\n" +
                            "        result += Math.random().toString(26).substring(3, 4); // A random alpha-numeric char.\n" +
                            "    } else {\n" +
                            "        if (Math.random() > 0.1) {\n" +
                            "            result += String.fromCharCode(Math.random() * 128 | 0); // Random ASCII char\n" +
                            "        } else {\n" +
                            "            result += String.fromCharCode(Math.random() * (Math.pow(2, 16)) | 0); // Random ANY char.\n" +
                            "        }\n" +
                            "    }\n" +
                            "}\n" +
                            "return result;"
                    );
                case Number:
                    return AstBuilder.stmtFromString(
                            "    switch(Math.random() * 3 | 0) {\n" +
                            "        case 0: return (Math.random() * 20) - 10;\n" +
                            "        case 1: return (Math.random() * 200) - 100 | 0;\n" +
                            "        case 2: {\n" +
                            "            switch(Math.random() * 3 | 0) {\n" +
                            "                case 0: return NaN;\n" +
                            "                case 1: return Infinity;\n" +
                            "                case 2: return -Infinity;\n" +
                            "            }\n" +
                            "        }\n" +
                            "    }");
                case Boolean:
                    return stmtFromString("return Math.random() > 0.5");
                case Any:
                    return stmtFromString("return {_any: {}}");
                case Undefined:
                case Void:
                    return stmtFromString("return void(0);");
                case Null:
                    return Return(nullLiteral());
                case Enum:
                    return Return(expFromString("(Math.random() * 10) | 0"));
                case Symbol:
                    return Return(newCall(identifier("Symbol")));
                case Never:
                    return throwStatement(newCall(identifier("Error")));
                case Object:
                    return Return(object());
                default:
                    throw new RuntimeException("Cannot yet produce a simple: " + simple.getKind());
            }
        }

        @Override
        public Statement visit(TupleType t, TypeContext typeContext) {
            return Return(array(t.getElementTypes().stream().map(element -> constructType(element, typeContext)).collect(Collectors.toList())));
        }

        @Override
        public Statement visit(UnionType t, TypeContext typeContext) {
            return constructUnion(t.getElements(), typeContext);
        }

        @Override
        public Statement visit(TypeParameterType type, TypeContext typeContext) {
            if (typeContext.containsKey(type)) {
                TypeWithContext lookup = typeContext.get(type);
                return Return(constructType(lookup.getType(), lookup.getTypeContext()));
            }
            String markerField = info.typeParameterIndexer.getMarkerField(type);
            if (type.getConstraint() != null && type.getConstraint() instanceof InterfaceType && ((InterfaceType)type.getConstraint()).getDeclaredStringIndexType() != null) {
                markerField = null;
            }

            return block(
                    variable("result", type.getConstraint() != null ? constructType(type.getConstraint(), typeContext) : object()),
                    ifThen(
                            binary(
                                    binary(unary(Operator.TYPEOF, identifier("result")), Operator.NOT_EQUAL_EQUAL, string("object")),
                                    Operator.AND,
                                    binary(unary(Operator.TYPEOF, identifier("result")), Operator.NOT_EQUAL_EQUAL, string("function"))
                            ),
                            throwStatement(newCall(identifier(RUNTIME_ERROR_NAME),
                                    binary(
                                            string("could not construct a TypeParameter that extends a "),
                                            Operator.PLUS,
                                            unary(Operator.TYPEOF, identifier("result"))
                                    )
                            ))
                    ),
                    markerField == null ? block() : statement(
                            binary(member(identifier("result"), markerField), Operator.EQUAL, bool(true))
                    ),
                    Return(identifier("result"))
            );
        }

        @Override
        public Statement visit(StringLiteral str, TypeContext typeContext) {
            return Return(string(str.getText()));
        }

        @Override
        public Statement visit(BooleanLiteral t, TypeContext typeContext) {
            return Return(bool(t.getValue()));
        }

        @Override
        public Statement visit(NumberLiteral t, TypeContext typeContext) {
            return Return(number(t.getValue()));
        }

        @Override
        public Statement visit(IntersectionType t, TypeContext typeContext) {
            assert !t.getElements().isEmpty();
            if (t.getElements().size() == 1) {
                return Return(constructType(t.getElements().iterator().next(), typeContext));
            }
            List<Expression> constructSubTypes = t.getElements().stream().map(element -> constructType(element, typeContext)).collect(Collectors.toList());

            // This can theoretically break stuff (that i save to result again, instead of always putting everything in the existing result), but it most likely won't (the soundness-test should detect if we ever get a type that could break this thing). Also, I didn't manage to find a type that breaks this, but such a type most likely exists.
            Statement populateResult = block(
                    expressionStatement(binary(identifier("result"), Operator.EQUAL, call(identifier("extend"), Util.concat(Collections.singletonList(identifier("result")), constructSubTypes))))
            );

            return createCachedConstruction(
                    Return(object()), populateResult,
                    0.2
            );
        }

        private Statement createCachedConstruction(Statement createInitialResult, Statement populateResult, double probability) {
            List<Statement> program = new ArrayList<>();
            Identifier cacheId = identifier(CONSTRUCTED_VARIABLE_CACHE_PREFIX + constructed_value_cache_counter++);
            valueVariableDeclarationList.add(variable(cacheId, identifier(VARIABLE_NO_VALUE)));
            program.add(
                    ifThen(
                            binary(
                                    binary(cacheId, Operator.NOT_EQUAL_EQUAL, identifier(VARIABLE_NO_VALUE)),
                                    Operator.AND,
                                    binary(call(identifier("random")), Operator.LESS_THAN_EQUAL, number(probability))
                            ),
                            Return(cacheId)
                    )
            );

            program.add(variable("result", call(function(createInitialResult))));

            program.add(variable("prevCacheValue", cacheId));
            program.add(statement(binary(cacheId, Operator.EQUAL, identifier("result"))));

            program.add(tryCatch(
                    block(
                            populateResult,
                            statement(binary(cacheId, Operator.EQUAL, identifier("result"))),
                            Return(identifier("result"))
                    ),
                    catchBlock(identifier("e"),
                            block(
                                    statement(binary(cacheId, Operator.EQUAL, identifier("prevCacheValue"))),
                                    throwStatement(identifier("e"))
                            )
                    )
            ));
            return block(program);
        }

        @Override
        public Statement visit(ClassInstanceType t, TypeContext typeContext) {
            return ((ClassType) t.getClassType()).getInstanceType().accept(this, typeContext);
        }

        @Override
        public Statement visit(ThisType t, TypeContext typeContext) {
            return typeContext.getThisType().accept(this, typeContext);
        }

        @Override
        public Statement visit(IndexType t, TypeContext typeContext) {
            throw new RuntimeException();
        }

        @Override
        public Statement visit(IndexedAccessType t, TypeContext typeContext) {
            throw new RuntimeException();
        }
    }

    private Statement constructArray(TypeContext typeContext, Type indexType) {
        Expression constructElement = constructType(indexType, typeContext);


        // An expression that returns an array with the correct type, with either 0, 1, 3, 4 or 5 elements in the array.
        return Return(conditional(
                binary(call(identifier("random")), Operator.GREATER_THAN, number(0.8)),
                array(),
                conditional(
                        binary(call(identifier("random")), Operator.GREATER_THAN, number(0.75)),
                        array(constructElement),
                        conditional(
                                binary(call(identifier("random")), Operator.GREATER_THAN, number(0.67)),
                                array(constructElement, constructElement, constructElement),
                                conditional(
                                        binary(call(identifier("random")), Operator.GREATER_THAN, number(0.5)),
                                        array(constructElement, constructElement, constructElement, constructElement),
                                        array(constructElement, constructElement, constructElement, constructElement, constructElement)
                                )
                        )
                )
        ));
    }


    private FunctionExpression createFunction(InterfaceType inter, TypeContext typeContext) {
        List<Signature> signatures = TypesUtil.removeDuplicateSignatures(Util.concat(inter.getDeclaredCallSignatures(), inter.getDeclaredConstructSignatures()));

        String interName = info.typeNames.get(inter);
        assert interName != null;

        return createFunction(signatures, typeContext, interName);
    }

    private FunctionExpression createFunction(List<Signature> signatures, TypeContext typeContext, String path) {
        assert signatures.size() > 0;

        int maxArgs = signatures.stream().map(Signature::getParameters).map(List::size).reduce(0, Math::max);

        List<String> args = new ArrayList<>();
        for (int i = 0; i < maxArgs; i++) {
            args.add("arg" + i);
        }

        assert !signatures.isEmpty();

        if (signatures.size() == 1) {
            Signature signature = signatures.iterator().next();

            List<Signature.Parameter> parameters = signature.getParameters();

            List<Statement> typeChecks = new ArrayList<>();

            if (signature.isHasRestParameter() && parameters.size() > 0) {
                Type restType = getArrayType(parameters.get(parameters.size() - 1).getType());

                // The check used to see if an error should be reported.
                if (options.checkDepthUseValue != options.checkDepthReport) {
                    typeChecks.add(block(
                            comment("This is just the call to see if an error should be reported, the check to see if the signature is valid is below. "),
                            statement(call(function(statement(call(
                            identifier("assert"),
                            call(identifier("checkRestArgs"), identifier("args"), number(parameters.size() - 1),
                                    function(block(
                                            Return(typeChecker.checkResultingType(new TypeWithContext(restType, typeContext), identifier("exp"), path + ".[restArgs]", options.checkDepthReport))
                                    ), "exp")
                            ),
                            string(path + ".[restArgs]"),
                            string("valid rest-args"),
                            AstBuilder.expFromString("Array.prototype.slice.call(args)"),
                            identifier("i"),
                            string("rest args")
                    )))))));
                }

                // The check used to see if the value should be used.
                typeChecks.add(ifThen(unary(Operator.NOT, call(
                        identifier("assert"),
                        call(identifier("checkRestArgs"), identifier("args"), number(parameters.size() - 1),
                                function(block(
                                        Return(typeChecker.checkResultingType(new TypeWithContext(restType, typeContext), identifier("exp"), path + ".[restArgs]", options.checkDepthUseValue))
                                ), "exp")
                        ),
                        string(path + ".[restArgs]"),
                        string("valid rest-args"),
                        AstBuilder.expFromString("Array.prototype.slice.call(args)"),
                        identifier("i"),
                        string("rest args")
                )), Return(bool(false))));

                parameters = parameters.subList(0, parameters.size() - 1);
            }

            Util.zip(args.stream(), parameters.stream(), (argName, par) ->
                    block(
                            info.options.makeSeparateReportAssertions() ? block() : expressionStatement(call(function(
                                    block(
                                            comment("There warnings are just reported, not used to see if the value should be used (that comes below). "),
                                            typeChecker.assertResultingType(new TypeWithContext(par.getType(), typeContext), identifier(argName), path + ".[" + argName + "]", info.options.checkDepthReport, "argument")
                                    )
                            ))),
                            typeChecker.assertResultingType(new TypeWithContext(par.getType(), typeContext), identifier(argName), path + ".[" + argName + "]", options.checkDepthUseValue, "argument")
                    )
            ).forEach(typeChecks::add);

            typeChecks.add(checkNumberOfArgs(signature));

            BlockStatement functionBody = block(
                    variable(identifier("args"), identifier("arguments")),
                    // Currently not using the information whether or not the signature was correct. The assertion-errors has already been reported anyway.
                    variable(identifier("signatureCorrect"), call(function(
                            block(
                                    block(typeChecks),
                                    Return(bool(true))
                            )
                    ))),
                    ifThenElse(
                            identifier("signatureCorrect"),
                            saveArgsAndReturnValue(signature, typeContext, path), // Saving the arguments, and returning something
                            Return(constructType(signature.getResolvedReturnType(), typeContext)) // Just returning the correct type, no saving arguments.
                    )

            );


            return function(
                    block(functionBody),
                    args
            );
        } else {
            Statement functionBody = block(
                    variable(identifier("args"), identifier("arguments")),
                    variable("foundSignatures", array()),
                    // Checking each signature, to see if correct.
                    block(Util.withIndex(signatures).map(signaturePair -> {
                        int signatureIndex = signaturePair.getRight();
                        Signature signature = signaturePair.getLeft();
                        List<Signature.Parameter> parameters = signature.getParameters();
                        Statement checkRestArgs = block();

                        if (signature.isHasRestParameter()) {
                            Type restType = getArrayType(parameters.get(parameters.size() - 1).getType());

                            checkRestArgs = ifThen(unary(Operator.NOT,
                                    call(identifier("checkRestArgs"), identifier("args"), number(parameters.size() - 1),
                                            function(block(
                                                    Return(typeChecker.checkResultingType(new TypeWithContext(restType, typeContext), identifier("exp"), path + ".[restArgs]", options.checkDepthForUnions))
                                            ), "exp")
                                    )),
                                    Return(bool(false))
                            );

                            parameters = parameters.subList(0, parameters.size() - 1);
                        }

                        return block(
                                variable("signatureCorrect" + signatureIndex, call(function(block(
                                        checkNumberOfArgs(signature),
                                        checkRestArgs,
                                        block(Util.withIndex(parameters).map(parameterPair -> {
                                            Integer argIndex = parameterPair.getRight();
                                            Signature.Parameter arg = parameterPair.getLeft();

                                            return block(
                                                    variable(identifier("arg" + argIndex + "Correct"), typeChecker.checkResultingType(new TypeWithContext(arg.getType(), typeContext), identifier("arg" + argIndex), path + ".[arg" + argIndex + "]", options.checkDepthForUnions)),
                                                    ifThen(
                                                            unary(Operator.NOT, identifier("arg" + argIndex + "Correct")),
                                                            Return(bool(false))
                                                    )
                                            );
                                        }).collect(Collectors.toList())),
                                        Return(bool(true))
                                )))),
                                ifThen(
                                        identifier("signatureCorrect" + signatureIndex),
                                        statement(methodCall(identifier("foundSignatures"), "push", number(signatureIndex)))
                                )
                        );
                    }).collect(Collectors.toList())),
                    ifThen(
                            binary(
                                    member(identifier("foundSignatures"), "length"),
                                    Operator.EQUAL_EQUAL_EQUAL,
                                    number(0)
                            ),
                            block(
                                    comment("Call assert, no valid overload found, the application was called in a wrong way."),
                                    statement(call(
                                            identifier("assert"),
                                            binary(
                                                    member(identifier("foundSignatures"), "length"),
                                                    Operator.NOT_EQUAL_EQUAL,
                                                    number(0)
                                            ),
                                            string(path),
                                            string("A valid overload"),
                                            AstBuilder.expFromString("Array.prototype.slice.call(args)"),
                                            identifier("i"),
                                            string("overload check")
                                    )),
                                    throwStatement(newCall(identifier("Error"), string("No valid overload found!")))
                            )
                    ),
                    ifThen(
                            binary(
                                    member(identifier("foundSignatures"), "length"),
                                    Operator.GREATER_THAN_EQUAL,
                                    number(2)
                            ),
                            block(
                                    comment("Call error, the application was imprecise, and couldn't identity the correct overload"),
                                    throwStatement(newCall(identifier(RUNTIME_ERROR_NAME), binary(string("Could not find correct overload for function: " + path + " results: "), Operator.PLUS, methodCall(identifier("foundSignatures"), "toString"))))
                            )
                    ),
                    comment("Save the arguments, and returns the value, of the correct overload. "),
                    switchCase(
                            arrayAccess(identifier("foundSignatures"), number(0)),
                            Util.withIndex(signatures).map(pair -> {
                                Integer signatureIndex = pair.getRight();
                                Signature signature = pair.getLeft();
                                return new Pair<Expression, Statement>(
                                        number(signatureIndex),
                                        saveArgsAndReturnValue(signature, typeContext, path)
                                );
                            }).collect(Collectors.toList())
                    )
            );

            return function(
                    block(functionBody),
                    args
            );
        }
    }

    private Statement checkNumberOfArgs(Signature signature) {
        BinaryExpression condition = binary(
                member(identifier("args"), "length"),
                Operator.GREATER_THAN_EQUAL,
                number(signature.getMinArgumentCount()));
        if (!signature.isHasRestParameter()) {
            condition = binary(
                    condition,
                    Operator.AND,
                    binary(
                            member(identifier("args"), "length"),
                            Operator.LESS_THAN_EQUAL,
                            number(signature.getParameters().size())
                    )
            );
        }
        return block(
                ifThen(
                        unary(Operator.NOT, condition
                        ),
                        Return(bool(false))
                )
        );
    }

    private BlockStatement saveArgsAndReturnValue(Signature signature, TypeContext typeContext, String path) {
        if (path.equals("mockFunctionForFirstMatchPolicy")) {
            return block(Return());
        }

        List<Signature.Parameter> parameters = signature.getParameters();

        List<Statement> saveArgumentValues = new ArrayList<>();

        if (signature.isHasRestParameter() && parameters.size() > 0) {
            Type restType = getArrayType(parameters.get(parameters.size() - 1).getType());

            parameters = parameters.subList(0, parameters.size() - 1);

            int valueIndex = createProducedValueVariable(restType, typeContext);

            int saveFromIndex = parameters.size(); // inclusive

            Expression indexToSave = AstBuilder.expFromString(saveFromIndex + " + (Math.random() * (arguments.length - " + saveFromIndex + ") | 0)");

            saveArgumentValues.add(
                    ifThen(
                            binary(AstBuilder.expFromString("arguments.length"), Operator.GREATER_THAN, number(saveFromIndex)),
                            statement(binary(identifier(VALUE_VARIABLE_PREFIX + valueIndex), Operator.EQUAL, arrayAccess(identifier("arguments"), indexToSave)))
                    )
            );
        }

        Util.withIndex(
                parameters.stream().map(par -> createProducedValueVariable(par.getType(), typeContext)),
                (valueIndex, argIndex) -> {
                    return block(
                            statement(binary(identifier(VALUE_VARIABLE_PREFIX + valueIndex), Operator.EQUAL, identifier("arg" + argIndex))),
                            statement(call(identifier("registerValue"), number(valueIndex)))
                    );
                }
        ).forEach(saveArgumentValues::add);

        Type returnType = signature.getResolvedReturnType();
        return block(
                block(saveArgumentValues),
                variable("result", constructType(returnType, typeContext)),
                ifThenElse(
                        binary(identifier("result"), Operator.NOT_EQUAL_EQUAL, identifier(VARIABLE_NO_VALUE)),
                        Return(identifier("result")),
                        throwStatement(newCall(identifier(RUNTIME_ERROR_NAME), string("Could not get an instance of the correct return-type, returning exceptionally instead.")))
                ),
                Return(identifier("result"))
        );
    }

    private Type getArrayType(Type array) {
        Type restType;
        if (array instanceof ReferenceType) {
            ReferenceType restTypeArr = (ReferenceType) array;

            assert "Array".equals(info.typeNames.get(restTypeArr.getTarget()));
            assert restTypeArr.getTypeArguments().size() == 1;

            restType = restTypeArr.getTypeArguments().iterator().next();
        } else {
            assert array instanceof GenericType;

            GenericType restTypeArr = (GenericType) array;

            assert "Array".equals(info.typeNames.get(restTypeArr.getTarget()));
            assert restTypeArr.getTypeArguments().size() == 1;

            restType = restTypeArr.getTypeArguments().iterator().next();
        }
        return restType;
    }


    private Statement constructTypeFromName(String name, TypeContext typeContext, List<Type> typeParameters) throws ProduceManuallyException {
        if (name == null) {
            throw new NullPointerException();
        }
        if (name.startsWith("global.")) {
            name = name.substring("global.".length(), name.length());
        }

        switch (name) {
            case "Array":
                assert typeParameters.size() == 1;

                return constructArray(typeContext, typeParameters.get(0));
            case "Object":
                return Return(constructType(SpecReader.makeEmptySyntheticInterfaceType(), typeContext));
            case "Number":
                return Return(newCall(identifier("Number"), constructType(new SimpleType(SimpleTypeKind.Number), typeContext)));
            case "Boolean":
                return Return(newCall(identifier("Boolean"), constructType(new SimpleType(SimpleTypeKind.Boolean), typeContext)));
            case "Function":
                InterfaceType interfaceWithSimpleFunction = SpecReader.makeEmptySyntheticInterfaceType();
                Signature callSignature = new Signature();
                callSignature.setParameters(new ArrayList<>());
                callSignature.setMinArgumentCount(0);
                callSignature.setHasRestParameter(true);
                callSignature.setResolvedReturnType(new SimpleType(SimpleTypeKind.Any));
                interfaceWithSimpleFunction.getDeclaredCallSignatures().add(callSignature);
                info.typeNames.put(interfaceWithSimpleFunction, "Function");
                return Return(constructType(interfaceWithSimpleFunction, typeContext));
            case "RegExp":
                Expression constructString = constructType(new SimpleType(SimpleTypeKind.String), TypeContext.create(info));
                return Return(newCall(identifier("RegExp"), constructString));
            case "String":
                return Return(newCall(identifier("String"), constructType(new SimpleType(SimpleTypeKind.String), typeContext)));
            case "HTMLCanvasElement":
                return AstBuilder.stmtFromString("return document.createElement('canvas')");
            case "HTMLVideoElement":
                return AstBuilder.stmtFromString("return document.createElement('video')");
            case "HTMLImageElement":
                return AstBuilder.stmtFromString("return document.createElement('img')");
            case "HTMLLinkElement":
                return AstBuilder.stmtFromString("return document.createElement('link')");
            case "WebGLRenderingContext":
                return AstBuilder.stmtFromString("return document.createElement(\"canvas\").getContext(\"webgl\")");
            case "WebGLTexture":
                return AstBuilder.stmtFromString("return document.createElement(\"canvas\").getContext(\"webgl\").createTexture()");
            case "WebGLFramebuffer":
                return AstBuilder.stmtFromString("return document.createElement(\"canvas\").getContext(\"webgl\").createFramebuffer()");
            case "WebGLRenderbuffer":
                return AstBuilder.stmtFromString("return document.createElement(\"canvas\").getContext(\"webgl\").createRenderbuffer()");
            case "CanvasRenderingContext2D":
            case "CanvasPathMethods":
                return AstBuilder.stmtFromString("return document.createElement(\"canvas\").getContext(\"2d\")");
            case "MouseEvent":
            case "Event":
                throw new ProduceManuallyException();
            case "WebGLProgram":
                return AstBuilder.stmtFromString("return document.createElement(\"canvas\").getContext(\"webgl\").createProgram()");
            case "WebGLBuffer":
                return AstBuilder.stmtFromString("return document.createElement(\"canvas\").getContext(\"webgl\").createBuffer()");
            case "ImageData":
                return AstBuilder.stmtFromString("return new ImageData(10, 10)");
            case "TouchEvent":
                return AstBuilder.stmtFromString("return new TouchEvent(null)");
            case "WebGLContextEvent":
                return AstBuilder.stmtFromString("return new WebGLContextEvent(null)");
            case "PointerEvent":
                return AstBuilder.stmtFromString("return new PointerEvent(\"pointermove\")");
            case "CanvasGradient":
                return AstBuilder.stmtFromString("return document.createElement(\"canvas\").getContext(\"2d\").createLinearGradient()");
            case "HTMLElement":
                return AstBuilder.stmtFromString("return document.createElement('div')");
            case "CanvasPattern":
                return AstBuilder.stmtFromString("return document.createElement(\"canvas\").getContext(\"2d\").createPattern()");
            case "EventTarget":
                return AstBuilder.stmtFromString("return document"); // Not good, but good enough.
            case "Element":
            case "Node":
                return AstBuilder.stmtFromString("return document.createElement(\"div\")");
            case "ErrorEvent":
                return AstBuilder.stmtFromString("return new ErrorEvent(\"foo\")");
            case "XMLHttpRequestUpload":
                return AstBuilder.stmtFromString("return new XMLHttpRequest().upload");
            case "Text":
                return AstBuilder.stmtFromString("return document.createTextNode(\"foo\")");
            case "DocumentFragment":
                return AstBuilder.stmtFromString("return new DocumentFragment()");
            case "XMLDocument":
                return AstBuilder.stmtFromString("return XMLDocument.load()");
            case "Document":
            case "HTMLDocument":
                return AstBuilder.stmtFromString("return document");
            case "Window":
                return AstBuilder.stmtFromString("return window");
            case "DragEvent":
                return AstBuilder.stmtFromString("return new DragEvent(12)");
            case "Navigator":
                return AstBuilder.stmtFromString("return window.navigator");
            case "MSCredentials":
                return AstBuilder.stmtFromString("return new MSCredentials()");
            case "Storage":
                return AstBuilder.stmtFromString("return window.localStorage");
            case "ApplicationCache":
                return AstBuilder.stmtFromString("return window.applicationCache");
            case "BarProp":
                return AstBuilder.stmtFromString("return window.locationbar");
            case "IDBFactory":
                return AstBuilder.stmtFromString("return window.indexedDB");
            case "Location":
                return AstBuilder.stmtFromString("return window.location");
            case "MediaQueryList":
                return AstBuilder.stmtFromString("return window.matchMedia(123)");
            case "URL":
                return AstBuilder.stmtFromString("return new URL(\"http://google.com\")");
            case "Screen":
                return AstBuilder.stmtFromString("return window.screen");
            case "Blob":
                return AstBuilder.stmtFromString("return new Blob()");
            case "History":
                return AstBuilder.stmtFromString("return window.history");
            case "Crypto":
                return AstBuilder.stmtFromString("return window.crypto");
            case "Console":
                return AstBuilder.stmtFromString("return console");
            case "StyleMedia":
                return AstBuilder.stmtFromString("return window.styleMedia");
            case "Selection":
                return AstBuilder.stmtFromString("return window.getSelection()");
            case "Performance":
                return AstBuilder.stmtFromString("return window.performance");
            case "SVGElement":
            case "SVGGElement":
                return AstBuilder.stmtFromString("return document.createElementNS(\"http://www.w3.org/2000/svg\", \"g\")");
            case "SVGSVGElement":
                return AstBuilder.stmtFromString("return document.createElementNS(\"http://www.w3.org/2000/svg\", \"svg\")");
            case "ProgressEvent":
                return AstBuilder.stmtFromString("return new ProgressEvent(1)");
            case "NodeList":
                return AstBuilder.stmtFromString("return document.childNodes");
            case "NodeListOf":
                return AstBuilder.stmtFromString("return document.querySelectorAll(\"foo bar baz\")"); // <- returns an empty node-list.
            case "HTMLScriptElement":
                return AstBuilder.stmtFromString("return document.createElement(\"script\")");
            case "HTMLAudioElement":
                return AstBuilder.stmtFromString("return document.createElement(\"audio\")");
            case "AudioContext":
                return AstBuilder.stmtFromString("return new AudioContext()");
            case "PannerNode":
                return AstBuilder.stmtFromString("return new AudioContext().createPanner()");
            case "Promise":
                return AstBuilder.stmtFromString("return new Promise(function(){})");
            case "CSSStyleSheet":
                return AstBuilder.stmtFromString("return document.styleSheets[0]");
            case "GainNode":
            case "AudioNode":
                return AstBuilder.stmtFromString("return new (window.AudioContext || window.webkitAudioContext)().createGain()");
            case "DynamicsCompressorNode":
                return AstBuilder.stmtFromString("return new (window.AudioContext || window.webkitAudioContext)().createDynamicsCompressor()");
            case "AudioBufferSourceNode":
                return AstBuilder.stmtFromString("return new AudioBufferSourceNode(new AudioContext())");
            case "HTMLTrackElement":
                return AstBuilder.stmtFromString("return document.createElement(\"track\")");
            case "HTMLInputElement":
                return AstBuilder.stmtFromString("return document.createElement(\"input\")");
            case "TimeRanges":
                return AstBuilder.stmtFromString("return document.createElement(\"video\").buffered");
            case "HTMLTextAreaElement":
                return AstBuilder.stmtFromString("return document.createElement(\"textarea\")");
            case "CustomEvent":
                return AstBuilder.stmtFromString("return new CustomEvent(\"123\")");
            case "ObjectConstructor":
                return AstBuilder.stmtFromString("return Object");
            case "FunctionConstructor":
                return AstBuilder.stmtFromString("return Function");
            case "RegExpConstructor":
                return AstBuilder.stmtFromString("return RegExp");
            case "ErrorConstructor":
                return AstBuilder.stmtFromString("return Error");
            case "NumberConstructor":
                return AstBuilder.stmtFromString("return Number");
            case "BooleanConstructor":
                return AstBuilder.stmtFromString("return Boolean");
            case "ArrayConstructor":
                return AstBuilder.stmtFromString("return Array");
            case "DateConstructor":
                return AstBuilder.stmtFromString("return Date");
            case "StringConstructor":
                return AstBuilder.stmtFromString("return String");
            case "HTMLCollection":
                return AstBuilder.stmtFromString("return document.forms");
            case "WheelEvent":
                return AstBuilder.stmtFromString("return new WheelEvent(1)");
            case "HTMLSourceElement":
                return AstBuilder.stmtFromString("return document.createElement(\"source\")");
            case "FocusEvent":
                return AstBuilder.stmtFromString("return new FocusEvent(\"iunb\")");
            case "MessageEvent":
                return AstBuilder.stmtFromString("return new MessageEvent(\"iunb\")");
            case "UIEvent":
                return AstBuilder.stmtFromString("return new UIEvent(1)");
            case "KeyboardEvent":
                return AstBuilder.stmtFromString("return new KeyboardEvent(1)");
            case "PageTransitionEvent":
                return AstBuilder.stmtFromString("return new PageTransitionEvent(1)");
            case "StorageEvent":
                return AstBuilder.stmtFromString("return new StorageEvent(1)");
            case "HashChangeEvent":
                return AstBuilder.stmtFromString("return new HashChangeEvent(1)");
            case "PopStateEvent":
                return AstBuilder.stmtFromString("return new PopStateEvent(1)");
            case "Headers":
                return AstBuilder.stmtFromString("return new PopStateEvent()");
            case "CustomElementRegistry":
                return AstBuilder.stmtFromString("return customElements");
            case "CacheStorage":
                return AstBuilder.stmtFromString("return caches");
            case "Request":
                return AstBuilder.stmtFromString("return new Request(1);");
            case "URLSearchParams":
                return AstBuilder.stmtFromString("return new URLSearchParams();");
            case "Int8Array":
            case "Uint8Array":
            case "Uint32Array":
            case "Int32Array":
            case "Float32Array":
            case "Uint16Array":
            case "Uint8ClampedArray":
            case "Int16Array":
            case "Float64Array":
            case "Range":
            case "XMLHttpRequest":
            case "ArrayBuffer":
            case "Date":
            case "Error":
                return AstBuilder.stmtFromString("return new " + name + "()");
            case "CSSRuleList":
            case "CSSStyleDeclaration":
            case "TouchList":
            case "DataTransfer":
                // Hacky, i know.
                return AstBuilder.stmtFromString("return (function () {var tmp = {}; tmp.__proto__  = " + name + ".prototype; return tmp})();");
            case "EventListener":
            case "EventListenerObject":
            case "WebKitPoint":
            case "ErrorEventHandler":
            case "Intl.CollatorOptions":
            case "Intl.ResolvedCollatorOptions":
            case "Intl.NumberFormatOptions":
            case "Intl.ResolvedNumberFormatOptions":
            case "Intl.DateTimeFormatOptions":
            case "Intl.ResolvedDateTimeFormatOptions":
            case "RTCIceServer":
            case "FrameRequestCallback":
            case "MSPointerEvent":
            case "MSGestureEvent":
            case "BeforeUnloadEvent":
            case "DeviceOrientationEvent":
            case "FocusNavigationOrigin":
            case "MediaStreamErrorEvent":
            case "DeviceMotionEvent":
            case "DeviceLightEvent":
            case "RequestInit":
            case "ScrollToOptions":
            case "BlobPropertyBag":
            case "DeviceAccelerationDict":
            case "MediaStreamError":
            case "DeviceAcceleration":
            case "DeviceRotationRate":
            case "ExtensionScriptApis":
            case "SpeechSynthesis":
            case "SpeechSynthesisVoice":
            case "ArrayLike":
            case "IterableIterator":
            case "IteratorResult":
            case "Iterator":
                throw new ProduceManuallyException();
            default:
                throw new RuntimeException("Unknown: " + name);
        }

    }

    private final class ProduceManuallyException extends Exception {
    }

    public CallExpression getType(Type type, TypeContext typeContext) {
        int index = getTypeIndex(type, typeContext);

        return getType(index);
    }

    public CallExpression getType(int index) {
        return call(identifier(GET_TYPE_PREFIX + index));
    }

    public Expression constructType(Type type, TypeContext typeContext) {
        int index = getTypeIndex(type, typeContext);

        if (!hasCreateTypeFunction.contains(index)) {
            hasCreateTypeFunction.add(index);
            constructTypeQueue.add(new TypeWithContext(type, typeContext));
        }

        return call(identifier(CONSTRUCT_TYPE_PREFIX + index));
    }

    public Expression constructType(int index) {
        TypeWithContext typeWithParameters = typeIndexes.inverse().get(index);
        return constructType(typeWithParameters.getType(), typeWithParameters.getTypeContext());
    }

    private int getTypeIndex(Type type, TypeContext typeContext) {
        TypeWithContext key = new TypeWithContext(type, typeContext);
        return getTypeIndex(key);
    }

    private int getTypeIndex(TypeWithContext key) {
        if (typeIndexes.containsKey(key)) {
            return typeIndexes.get(key);
        } else {
            int value = typeIndexes.size();
            typeIndexes.put(key, value);

            getTypeQueue.add(key);

            return value;
        }
    }

    private final List<TypeWithContext> getTypeQueue = new ArrayList<>();
    private final List<TypeWithContext> constructTypeQueue = new ArrayList<>();

    private void finish() {
        while (!constructTypeQueue.isEmpty()) {
            ArrayList<TypeWithContext> clone = new ArrayList<>(constructTypeQueue);
            constructTypeQueue.clear();
            for (TypeWithContext typeWithContext : clone) {
                addConstructInstanceFunction(typeWithContext, getTypeIndex(typeWithContext)); // This is hopefully only ever called here.
            }
        }

        for (TypeWithContext key : getTypeQueue) {
            int value = typeIndexes.get(key);

            List<Integer> values = new ArrayList<>(valueLocations.get(key));
            while (key.getType() instanceof ReferenceType) {
                ReferenceType ref = (ReferenceType) key.getType();
                Type target = ref.getTarget();
                TypeContext typeContext = new TypesUtil(info).generateParameterMap(ref, key.getTypeContext()).optimizeTypeParameters(target);
                key = new TypeWithContext(target, typeContext);
                values.addAll(valueLocations.get(key));
            }

            values = values.stream().distinct().collect(Collectors.toList());

            Statement returnTypeStatement;

            if (values.size() == 1) {
                returnTypeStatement = Return(identifier(VALUE_VARIABLE_PREFIX + values.iterator().next()));
            } else if (values.isEmpty()) {
                returnTypeStatement = Return(identifier(VARIABLE_NO_VALUE));
            } else {
                returnTypeStatement = returnOneOfExistingValues(values);
            }

            ExpressionStatement getTypeFunction = statement(
                    function(
                            GET_TYPE_PREFIX + value,
                            block(returnTypeStatement)
                    )
            );
            functions.add(getTypeFunction);
        }

    }

    private final Set<Integer> hasCreateTypeFunction = new HashSet<>();

    private static final String CONSTRUCTED_VARIABLE_CACHE_PREFIX = "constructed_cache_";
    private int constructed_value_cache_counter = 0;
    private void addConstructInstanceFunction(TypeWithContext typeWithParameters, int index) {
        Type type = typeWithParameters.getType();
        TypeContext typeContext = typeWithParameters.getTypeContext();

        if (!info.shouldConstructType(type)) {
            functions.add(statement(
                    function(
                            CONSTRUCT_TYPE_PREFIX + index,
                            block(
                                    variable("existingValue", getType(index)),
                                    ifThenElse(
                                            binary(identifier("existingValue"), Operator.NOT_EQUAL_EQUAL, identifier(VARIABLE_NO_VALUE)),
                                            Return(identifier("existingValue")),
                                            throwStatement(newCall(identifier(RUNTIME_ERROR_NAME), string("I will not construct this type, " + type.getClass().getSimpleName())))
                                    )
                            )
                    )
            ));
        } else {
            functions.add(statement(
                    function(
                            CONSTRUCT_TYPE_PREFIX + index,
                            block(
                                    variable("existingValue", getType(index)),
                                    ifThenElse(
                                            binary(
                                                    binary(identifier("existingValue"), Operator.NOT_EQUAL_EQUAL, identifier(VARIABLE_NO_VALUE)),
                                                    Operator.AND,
                                                    binary(call(identifier("random")), Operator.GREATER_THAN, number(0.5))
                                            ),
                                            Return(identifier("existingValue")),
                                            type.accept(new ConstructNewInstanceVisitor(), typeContext)
                                    )
                            )
                    )
            ));
        }


    }

    public BlockStatement getBlockStatementWithTypeFunctions() {
        return block(functions);
    }
}
