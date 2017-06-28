package dk.webbies.tajscheck.typeutil;

import dk.au.cs.casa.typescript.SpecReader;
import dk.au.cs.casa.typescript.types.*;
import dk.webbies.tajscheck.TypeWithContext;
import dk.webbies.tajscheck.benchmark.BenchmarkInfo;
import dk.webbies.tajscheck.benchmark.FreeGenericsFinder;
import dk.webbies.tajscheck.benchmark.TypeParameterIndexer;
import dk.webbies.tajscheck.parsespec.ParseDeclaration;
import dk.webbies.tajscheck.typeutil.typeContext.TypeContext;
import dk.webbies.tajscheck.util.ArrayListMultiMap;
import dk.webbies.tajscheck.util.Pair;
import dk.webbies.tajscheck.util.Util;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Created by erik1 on 01-11-2016.
 */
public class TypesUtil {

    private BenchmarkInfo info;

    public TypesUtil(BenchmarkInfo info) {
        this.info = info;
    }

    public static InterfaceType classToInterface(ClassType t, FreeGenericsFinder freeGenericsFinder) {
        InterfaceType interfaceType = SpecReader.makeEmptySyntheticInterfaceType();

        for (Signature signature : t.getSignatures()) {
            Signature constructor = createConstructorSignature(t, signature);
            interfaceType.getDeclaredConstructSignatures().add(constructor);
        }
        if (t.getSignatures().isEmpty()) {
            Signature constructor = new Signature();
            constructor.setHasRestParameter(false);
            constructor.setIsolatedSignatureType(null);
            constructor.setMinArgumentCount(0);
            constructor.setParameters(Collections.emptyList());
            constructor.setTarget(constructor);
            constructor.setTypeParameters(Collections.emptyList());
            constructor.setUnionSignatures(Collections.emptyList());
            constructor.setResolvedReturnType(t.getInstanceType());
            interfaceType.getDeclaredConstructSignatures().add(constructor);
        }

        interfaceType.setBaseTypes(
                t.getBaseTypes().stream().map(base -> {
                    if (base instanceof ClassType) {
                        return classToInterface((ClassType) base, freeGenericsFinder);
                    } else {
                        return base;
                    }
                }).collect(Collectors.toList())
        );

        interfaceType.setDeclaredProperties(t.getStaticProperties());

        interfaceType.setDeclaredNumberIndexType(t.getDeclaredNumberIndexType());

        interfaceType.setDeclaredStringIndexType(t.getDeclaredStringIndexType());

        // Target and typeArguments are out. But they are pretty much ignored in GenericType anyway (which is similar).

        interfaceType.setTypeParameters(t.getTypeParameters());

        if (freeGenericsFinder.hasThisTypes(t)) {
            freeGenericsFinder.addHasThisTypes(interfaceType);
        }

        return interfaceType;
    }

    public static InterfaceType signaturesToInterface(List<Signature> signatures, Map<Type, String> typeNames) {
        if (signatures.isEmpty()) {
            throw new RuntimeException();
        }
        InterfaceType result = SpecReader.makeEmptySyntheticInterfaceType();
        ArrayList<Signature> clonedSignatures = new ArrayList<>(signatures);
        if (clonedSignatures.size() == 1) {
            // To force it into being a type-overloaded signature
            Signature neverSignature = emptySignature();
            neverSignature.setMinArgumentCount(1);
            Signature.Parameter parameter = new Signature.Parameter();
            parameter.setName("x");
            parameter.setType(new SimpleType(SimpleTypeKind.Never));
            neverSignature.setParameters(Collections.singletonList(parameter));
            clonedSignatures.add(neverSignature);
        }
        result.setDeclaredCallSignatures(clonedSignatures);
        typeNames.put(result, "mockFunctionForFirstMatchPolicy");
        return result;
    }

    private static Signature emptySignature() {
        Signature signature = new Signature();
        signature.setHasRestParameter(false);
        signature.setIsolatedSignatureType(null);
        signature.setMinArgumentCount(0);
        signature.setParameters(new ArrayList<>());
        signature.setTarget(null);
        signature.setTypeParameters(new ArrayList<>());
        signature.setUnionSignatures(new ArrayList<>());
        signature.setResolvedReturnType(new SimpleType(SimpleTypeKind.Any));
        return signature;
    }

    public static Signature createConstructorSignature(ClassType t, Signature signature) {
        Signature constructor = new Signature();
        constructor.setHasRestParameter(signature.isHasRestParameter());
        constructor.setIsolatedSignatureType(signature.getIsolatedSignatureType());
        constructor.setMinArgumentCount(signature.getMinArgumentCount());
        constructor.setParameters(signature.getParameters());
        constructor.setTarget(signature.getTarget());
        constructor.setTypeParameters(signature.getTypeParameters());
        constructor.setUnionSignatures(signature.getUnionSignatures());
        constructor.setResolvedReturnType(t.getInstanceType());
        return constructor;
    }

    public TypeContext generateParameterMap(ReferenceType ref) {
        List<Type> arguments = ref.getTypeArguments();
        Map<TypeParameterType, Type> parameterMap = new HashMap<>();

        List<Type> typeParameters = getTypeParameters(ref.getTarget());
        assert typeParameters.size() == arguments.size();
        List<TypeParameterType> parameters = Util.cast(TypeParameterType.class, typeParameters);
        parameterMap = new HashMap<>(parameterMap);
        for (int i = 0; i < arguments.size(); i++) {
            parameterMap.put(parameters.get(i), arguments.get(i));
        }
        return TypeContext.create(info).append(parameterMap);
    }

    public static List<Type> getTypeParameters(Type target) {
        if (target instanceof GenericType) {
            return ((GenericType) target).getTypeParameters();
        } else if (target instanceof InterfaceType) {
            return ((InterfaceType) target).getTypeParameters();
        } else if (target instanceof ClassType) {
            return ((ClassType) target).getTypeParameters();
        } else if (target instanceof ClassInstanceType) {
            return ((ClassType) ((ClassInstanceType) target).getClassType()).getTypeParameters();
        } else if (target instanceof TupleType) {
            return ((TupleType) target).getElementTypes();
        } else {
            throw new RuntimeException(target.getClass().getName());
        }
    }

    public TypeContext generateParameterMap(ReferenceType type, TypeContext typeContext) {
        return typeContext.append(generateParameterMap(type));
    }

    public static Set<Type> collectAllTypes(Collection<Type> types) {
        CollectAllTypesVisitor visitor = new CollectAllTypesVisitor();
        for (Type type : types) {
            visitor.accept(type);
        }
        return visitor.getSeen();
    }

    public static Set<Type> collectAllTypes(Type type) {
        return collectAllTypes(Collections.singletonList(type));
    }

    public static Set<Type> collectNativeTypes(SpecReader spec, SpecReader emptySpec) {
        Map<Type, String> specNames = ParseDeclaration.getTypeNamesMap(spec);

        Map<Type, String> nativeNameMap = ParseDeclaration.getTypeNamesMap(emptySpec);
        Set<String> nativeNames = new HashSet<>(nativeNameMap.values());

        Map<String, Type> inverseNativeNameMap = new HashMap<>();
        for (Map.Entry<Type, String> entry : nativeNameMap.entrySet()) {
            if (inverseNativeNameMap.containsKey(entry.getValue())) {
                inverseNativeNameMap.put(entry.getValue(), null);
            } else {
                inverseNativeNameMap.put(entry.getValue(), entry.getKey());
            }
        }

        return specNames.entrySet().stream().filter(entry -> {
            if (!nativeNames.contains(entry.getValue())) {
                return false;
            }
            Type nativeType = inverseNativeNameMap.get(entry.getValue());
            if (nativeType == null) {
                return false;
            }
            Type type = entry.getKey();
            return pseudoEquals(nativeType, type);
        }).map(Map.Entry::getKey).collect(Collectors.toSet());
    }

    private static boolean pseudoEquals(Type nativeType, Type type) {
        if (!type.getClass().equals(nativeType.getClass())) {
            return false;
        }
        if (nativeType instanceof ReferenceType) {
            nativeType = ((ReferenceType) nativeType).getTarget();
            type = ((ReferenceType) type).getTarget();
        }
        if (nativeType instanceof GenericType) {
            nativeType = ((GenericType) nativeType).toInterface();
            type = ((GenericType) type).toInterface();
        }
        if (nativeType instanceof InterfaceType) {
            Set<String> nativeProps = ((InterfaceType) nativeType).getDeclaredProperties().keySet();
            Set<String> typeProps = ((InterfaceType) type).getDeclaredProperties().keySet();
            return Util.intersection(nativeProps, typeProps).size() == Math.min(nativeProps.size(), typeProps.size());
        }
        if (nativeType instanceof IndexedAccessType || nativeType instanceof TypeParameterType || nativeType instanceof ThisType) {
            return true;
        }
        if (nativeType instanceof UnionType) {
            return ((UnionType) nativeType).getElements().size() == ((UnionType) type).getElements().size();
        }
        if (nativeType instanceof IntersectionType) {
            return ((IntersectionType) nativeType).getElements().size() == ((IntersectionType) type).getElements().size();
        }
        if (nativeType instanceof TupleType) {
            return ((TupleType) nativeType).getElementTypes().size() == ((TupleType) type).getElementTypes().size();
        }
        if (nativeType instanceof IntersectionType) {
            return ((IntersectionType) nativeType).getElements().size() == ((IntersectionType) type).getElements().size();
        }
        throw new RuntimeException(type.getClass().getSimpleName());
    }

    public static boolean isEmptyInterface(Type type) {
        if (!(type instanceof InterfaceType)) {
            return false;
        }
        InterfaceType inter = (InterfaceType) type;
        return inter.getDeclaredProperties().isEmpty() &&
                inter.getBaseTypes().isEmpty() &&
                inter.getTypeParameters().isEmpty() &&
                inter.getDeclaredCallSignatures().isEmpty() &&
                inter.getDeclaredConstructSignatures().isEmpty() &&
                inter.getDeclaredStringIndexType() == null &&
                inter.getDeclaredNumberIndexType() == null;
    }

    public static List<Signature> splitOptionalSignatures(List<Signature> signatures) {
        return signatures.stream().map(TypesUtil::makeSureOptionalArgumentsHaveUnionUndef).map(TypesUtil::splitOptionalSignature).reduce(new ArrayList<>(), Util::reduceList);
    }

    private static Signature makeSureOptionalArgumentsHaveUnionUndef(Signature signature) {
        signature = cloneSignature(signature);
        int end = signature.getParameters().size();
        if (signature.isHasRestParameter()) {
            end--;
        }
        for (int i = signature.getMinArgumentCount(); i < end; i++) {
            Signature.Parameter parameter = signature.getParameters().get(i);
            Type type = parameter.getType();
            if (!(type instanceof UnionType)) {
                UnionType union = new UnionType();
                union.setElements(Arrays.asList(type, new SimpleType(SimpleTypeKind.Undefined)));
                parameter.setType(union);
            } else {
                UnionType union = (UnionType) type;
                boolean hasUndef = union.getElements().stream().anyMatch(sub -> sub instanceof SimpleType && ((SimpleType) sub).getKind() == SimpleTypeKind.Undefined);
                if (!hasUndef) {
                    UnionType newUnion = new UnionType();
                    parameter.setType(newUnion);
                    newUnion.setElements(Util.concat(union.getElements(), Collections.singletonList(new SimpleType(SimpleTypeKind.Undefined))));
                }
            }
        }
        return signature;
    }

    private static List<Signature> splitOptionalSignature(Signature signature) {
        if (signature.getMinArgumentCount() == signature.getParameters().size()) {
            return Collections.singletonList(signature);
        }

        if (signature.isHasRestParameter() && signature.getMinArgumentCount() + 1 == signature.getParameters().size()) {
            return Collections.singletonList(signature);
        }

        List<Signature> result = new ArrayList<>();

        Signature withNoOptional = cloneSignature(signature);
        withNoOptional.setHasRestParameter(false);
        result.add(withNoOptional);
        for (int i = signature.getParameters().size() - 1; i >= signature.getMinArgumentCount(); i--) {
            withNoOptional.getParameters().remove(i);
        }

        assert withNoOptional.getParameters().size() == withNoOptional.getMinArgumentCount();

        Signature withOptional = cloneSignature(signature);

        Signature.Parameter optionalParameter = withOptional.getParameters().get(signature.getMinArgumentCount());

        optionalParameter.setType(removeUndef((UnionType) optionalParameter.getType()));

        withOptional.setMinArgumentCount(withOptional.getMinArgumentCount() + 1);

        result.addAll(splitOptionalSignature(withOptional));

        return result;
    }

    private static Type removeUndef(UnionType union) {
        ArrayList<Type> elements = new ArrayList<>(union.getElements());
        union = new UnionType();
        union.setElements(elements);

        assert union.getElements().stream().anyMatch(sub -> sub instanceof SimpleType && ((SimpleType) sub).getKind() == SimpleTypeKind.Undefined);

        union.setElements(union.getElements().stream().filter(sub -> !(sub instanceof SimpleType && ((SimpleType) sub).getKind() == SimpleTypeKind.Undefined)).collect(Collectors.toList()));

        if (union.getElements().size() == 1) {
            return union.getElements().iterator().next();
        }
        if (union.getElements().isEmpty() && elements.size() >= 2) {
            return new SimpleType(SimpleTypeKind.Undefined);
        }

        assert !union.getElements().isEmpty();

        return union;
    }

    public static List<Signature> splitUnionsInSignatures(List<Signature> signatures) {
        return signatures.stream().map(TypesUtil::splitUnionsInSignature).reduce(new ArrayList<>(), Util::reduceList);
    }

    private static List<Signature> splitUnionsInSignature(Signature signature) {
        for (int i = 0; i < signature.getParameters().size(); i++) {
            Signature.Parameter parameter = signature.getParameters().get(i);
            if (parameter.getType() instanceof UnionType) {
                List<Type> elements = ((UnionType) parameter.getType()).getElements();

                List<Signature> result = new ArrayList<>();

                for (Type element : elements) {
                    Signature subSignature = cloneSignature(signature);
                    Signature.Parameter newParameter = new Signature.Parameter();
                    newParameter.setName(parameter.getName());
                    newParameter.setType(element);

                    subSignature.getParameters().set(i, newParameter);

                    result.addAll(splitUnionsInSignature(subSignature));
                }
                return result;
            }
        }
        return Collections.singletonList(signature);
    }

    private static Signature cloneSignature(Signature signature) {
        Signature result = new Signature();
        result.setResolvedReturnType(signature.getResolvedReturnType());
        result.setUnionSignatures(signature.getUnionSignatures());
        result.setTarget(signature.getTarget());
        result.setParameters(new ArrayList<>(signature.getParameters()));
        result.setHasRestParameter(signature.isHasRestParameter());
        result.setIsolatedSignatureType(signature.getIsolatedSignatureType());
        result.setMinArgumentCount(signature.getMinArgumentCount());
        result.setTypeParameters(signature.getTypeParameters());
        return result;
    }

    public static List<Signature> removeDuplicateSignatures(List<Signature> signatures) {
        return signatures.stream().map(SignatureComparisonContainer::new).distinct().map(SignatureComparisonContainer::getSignature).collect(Collectors.toList());
    }

    public static Type getNativeBase(Type type, Set<Type> nativeTypes, Map<Type, String> typeNames) {
        Predicate<Type> isNativeType = t -> {
            if (t instanceof InterfaceType && TypesUtil.isEmptyInterface((InterfaceType) t)) {
                return false;
            }
            if (nativeTypes.contains(t)) {
                return true;
            }
            if (t instanceof ReferenceType && nativeTypes.contains(((ReferenceType) t).getTarget())) {
                return true;
            }
            return false;
        };
        List<Type> nativeBaseTypes = getAllBaseTypes(type, new HashSet<>(), Util.not(isNativeType)).stream().filter(isNativeType).collect(Collectors.toList());

        if (nativeBaseTypes.size() == 0) {
            return null;
        } else if (nativeBaseTypes.size() == 1) {
            return nativeBaseTypes.iterator().next();
        }
        return null; // Hard to do better.
    }


    public static Set<Type> getAllBaseTypes(Type type) {
        return getAllBaseTypes(type, new HashSet<>());
    }

    public static Set<Type> getAllBaseTypes(Type type, Set<Type> acc) {
        return getAllBaseTypes(type, acc, (subType) -> true);
    }

    public static Type normalize(Type type) {
        if (type instanceof ClassInstanceType) {
            return normalize(((ClassType) ((ClassInstanceType) type).getClassType()).getInstanceType());
        }
        if (type instanceof GenericType) {
            return normalize(((GenericType) type).toInterface());
        }
        if (type instanceof ReferenceType) {
            return normalize(((ReferenceType) type).getTarget());
        }
        return type;
    }

    private static Set<Type> getAllBaseTypes(Type type, Set<Type> acc, Predicate<Type> shouldContinue) {
        if (acc.contains(type)) {
            return acc;
        }
        acc.add(type);
        if (type instanceof ReferenceType) {
            type = ((ReferenceType) type).getTarget();
        }
        if (type instanceof GenericType) {
            if (shouldContinue.test(type)) {
                for (Type base : ((GenericType) type).getBaseTypes()) {
                    getAllBaseTypes(base, acc, shouldContinue);
                }
            }
        }
        if (type instanceof ClassInstanceType) {
            type = ((ClassType) ((ClassInstanceType) type).getClassType()).getInstanceType();
        }
        if (type instanceof InterfaceType) {
            if (shouldContinue.test(type)) {
                for (Type base : ((InterfaceType) type).getBaseTypes()) {
                    getAllBaseTypes(base, acc, shouldContinue);
                }
            }
        }
        if (type instanceof ClassType) {
            if (shouldContinue.test(type)) {
                for (Type base : ((ClassType) type).getBaseTypes()) {
                    getAllBaseTypes(base, acc, shouldContinue);
                }
            }
        }

        return acc;
    }

    public Set<TypeWithContext> getAllStringIndexerTypes(Type t, TypeContext context) {
        Set<TypeWithContext> res = new HashSet<>();
        getAllStringIndexerTypes(t, context, res);
        return res.stream().filter(Objects::nonNull).collect(Collectors.toSet());
    }

    private void getAllStringIndexerTypes(Type t, TypeContext context, Set<TypeWithContext> acc) {
        if (info.nativeTypes.contains(t)) {
            return;
        }
        if (t instanceof InterfaceType) {
            if (((InterfaceType) t).getDeclaredStringIndexType() != null) {
                acc.add(new TypeWithContext(((InterfaceType) t).getDeclaredStringIndexType(), context));
            }
            ((InterfaceType) t).getBaseTypes().forEach(type ->
                    getAllStringIndexerTypes(type, context, acc)
            );
        } else if (t instanceof ClassInstanceType) {
            InterfaceType instanceType = ((ClassType) ((ClassInstanceType) t).getClassType()).getInstanceType();
            getAllStringIndexerTypes(instanceType, context, acc);
        } else if (t instanceof ClassType) {
            if (((ClassType) t).getDeclaredStringIndexType() != null) {
                acc.add(new TypeWithContext(((ClassType) t).getDeclaredStringIndexType(), context));
            }
            ((ClassType) t).getBaseTypes().forEach(type -> {
                getAllStringIndexerTypes(type, context, acc);
            });
        } else if (t instanceof GenericType) {
            getAllStringIndexerTypes(((GenericType) t).toInterface(), context, acc);
        } else if (t instanceof ReferenceType) {
            getAllStringIndexerTypes(((ReferenceType) t).getTarget(), generateParameterMap((ReferenceType) t, context), acc);
        } else {
            throw new RuntimeException(t.getClass().getSimpleName());
        }
    }


    public Set<TypeWithContext> getAllNumberIndexerTypes(Type t, TypeContext context) {
        Set<TypeWithContext> res = new HashSet<>();
        getAllNumberIndexerTypes(t, context, res);
        return res.stream().filter(Objects::nonNull).collect(Collectors.toSet());
    }

    private void getAllNumberIndexerTypes(Type t, TypeContext context, Set<TypeWithContext> acc) {
        if (info.nativeTypes.contains(t)) {
            return;
        }
        if (t instanceof InterfaceType) {
            if (((InterfaceType) t).getDeclaredNumberIndexType() != null) {
                acc.add(new TypeWithContext(((InterfaceType) t).getDeclaredNumberIndexType(), context));
            }
            ((InterfaceType) t).getBaseTypes().forEach(type ->
                    getAllNumberIndexerTypes(type, context, acc)
            );
        } else if (t instanceof ClassInstanceType) {
            InterfaceType instanceType = ((ClassType) ((ClassInstanceType) t).getClassType()).getInstanceType();
            getAllNumberIndexerTypes(instanceType, context, acc);
        } else if (t instanceof ClassType) {
            if (((ClassType) t).getDeclaredStringIndexType() != null) {
                acc.add(new TypeWithContext(((ClassType) t).getDeclaredStringIndexType(), context));
            }
            ((ClassType) t).getBaseTypes().forEach(type -> {
                getAllNumberIndexerTypes(type, context, acc);
            });
        } else if (t instanceof GenericType) {
            getAllNumberIndexerTypes(((GenericType) t).toInterface(), context, acc);
        } else if (t instanceof ReferenceType) {
            getAllNumberIndexerTypes(((ReferenceType) t).getTarget(), generateParameterMap((ReferenceType) t, context), acc);
        } else {
            throw new RuntimeException(t.getClass().getSimpleName());
        }
    }


    public Set<Pair<TypeContext, Map<String, Type>>> getAllPropertyDeclarations(Type t, TypeContext context) {
        Set<Pair<TypeContext, Map<String, Type>>> res = new HashSet<>();
        getAllPropertyDeclarations(t, context, res);
        return res.stream().filter(Objects::nonNull).collect(Collectors.toSet());
    }

    private void getAllPropertyDeclarations(Type t, TypeContext context, Set<Pair<TypeContext, Map<String, Type>>> acc) {
        if (info.nativeTypes.contains(t)) {
            return;
        }
        if (t instanceof InterfaceType) {
            acc.add(new Pair<>(context, ((InterfaceType) t).getDeclaredProperties()));
            ((InterfaceType) t).getBaseTypes().forEach(type ->
                    getAllPropertyDeclarations(type, context, acc)
            );
        } else if (t instanceof ClassInstanceType) {
            InterfaceType instanceType = ((ClassType) ((ClassInstanceType) t).getClassType()).getInstanceType();
            getAllPropertyDeclarations(instanceType, context, acc);
        } else if (t instanceof ClassType) {
            acc.add(new Pair<>(context, ((ClassType) t).getStaticProperties()));
            ((ClassType) t).getBaseTypes().forEach(type -> {
                getAllPropertyDeclarations(type, context, acc);
            });
        } else if (t instanceof GenericType) {
            getAllPropertyDeclarations(((GenericType) t).toInterface(), context, acc);
        } else if (t instanceof ReferenceType) {
            getAllPropertyDeclarations(((ReferenceType) t).getTarget(), generateParameterMap((ReferenceType) t, context), acc);
        } else {
            throw new RuntimeException(t.getClass().getSimpleName());
        }
    }


    private static final class SignatureComparisonContainer {
        private final Signature signature;

        private SignatureComparisonContainer(Signature signature) {
            this.signature = signature;
        }

        public Signature getSignature() {
            return signature;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            SignatureComparisonContainer that = (SignatureComparisonContainer) o;

            Signature sig1 = this.signature;
            Signature sig2 = that.signature;
            if (sig1.getTypeParameters().size() != sig2.getTypeParameters().size()) {
                return false;
            }
            for (int i = 0; i < sig1.getTypeParameters().size(); i++) {
                if (!sig1.getTypeParameters().get(i).equals(sig2.getTypeParameters().get(i))) {
                    return false;
                }
            }

            if (sig1.getParameters().size() != sig2.getParameters().size()) {
                return false;
            }
            for (int i = 0; i < sig1.getParameters().size(); i++) {
                if (!sig1.getParameters().get(i).equals(sig2.getParameters().get(i))) {
                    return false;
                }
            }

            if (!Objects.equals(sig1.getResolvedReturnType(), sig2.getResolvedReturnType())) {
                return false;
            }

            if (sig1.getMinArgumentCount() != sig2.getMinArgumentCount()) {
                return false;
            }

            if (sig1.isHasRestParameter() != sig2.isHasRestParameter()) {
                return false;
            }

            if (!Objects.equals(sig1.getTarget(), sig2.getTarget())) {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode() {
            return 1337;
        }
    }


    private static class CollectAllTypesVisitor extends RecursiveTypeVisitor<Void> {

        Set<Type> getSeen() {
            return seen;
        }
    }

    public Pair<InterfaceType, TypeContext> constructSyntheticInterfaceWithBaseTypes(InterfaceType inter, Map<Type, String> typeNames, FreeGenericsFinder freeGenericsFinder) {
        if (inter.getBaseTypes().isEmpty()) {
            return new Pair<>(inter, TypeContext.create(info));
        }
//        assert inter.getTypeParameters().isEmpty(); // This should only happen when constructed from a generic/reference type, and in that case we have handled the TypeParameters.
        Map<TypeParameterType, Type> newParameters = new HashMap<>();
        InterfaceType result = SpecReader.makeEmptySyntheticInterfaceType();

        result.getDeclaredCallSignatures().addAll(inter.getDeclaredCallSignatures());
        result.getDeclaredConstructSignatures().addAll(inter.getDeclaredConstructSignatures());
        result.setDeclaredNumberIndexType(inter.getDeclaredNumberIndexType());
        result.setDeclaredStringIndexType(inter.getDeclaredStringIndexType());

        typeNames.put(result, typeNames.get(inter));
        inter.getBaseTypes().forEach(subType -> {
            if (subType instanceof ReferenceType) {
                newParameters.putAll(generateParameterMap((ReferenceType) subType).getMap());
                subType = ((ReferenceType) subType).getTarget();
            }
            if (subType instanceof ClassType) {
                subType = TypesUtil.classToInterface((ClassType) subType, freeGenericsFinder);
            }
            if (subType instanceof GenericType) {
                subType = ((GenericType) subType).toInterface();
            }
            if (subType instanceof ClassInstanceType) {
                subType = ((ClassType) ((ClassInstanceType) subType).getClassType()).getInstanceType();
            }
            Pair<InterfaceType, TypeContext> pair = constructSyntheticInterfaceWithBaseTypes((InterfaceType) subType, typeNames, freeGenericsFinder);
            newParameters.putAll(pair.getRight().getMap());
            InterfaceType type = pair.getLeft();
            result.getDeclaredCallSignatures().addAll((type.getDeclaredCallSignatures()));
            result.getDeclaredConstructSignatures().addAll(type.getDeclaredConstructSignatures());
            if (result.getDeclaredNumberIndexType() == null) {
                result.setDeclaredNumberIndexType(type.getDeclaredNumberIndexType());
            }
            if (result.getDeclaredStringIndexType() == null) {
                result.setDeclaredStringIndexType(type.getDeclaredStringIndexType());
            }
            result.getDeclaredProperties().putAll(inter.getDeclaredProperties());
            for (Map.Entry<String, Type> entry : type.getDeclaredProperties().entrySet()) {
                if (result.getDeclaredProperties().containsKey(entry.getKey())) {
                    continue;
                }
                result.getDeclaredProperties().put(entry.getKey(), entry.getValue());
            }
        });
        return new Pair<>(result, TypeContext.create(info).append(newParameters));
    }
}
