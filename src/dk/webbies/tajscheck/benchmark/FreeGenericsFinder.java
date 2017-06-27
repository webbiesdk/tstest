package dk.webbies.tajscheck.benchmark;

import dk.au.cs.casa.typescript.types.*;
import dk.webbies.tajscheck.typeutil.TypesUtil;
import dk.webbies.tajscheck.util.ArrayListMultiMap;
import dk.webbies.tajscheck.util.HashSetMultiMap;
import dk.webbies.tajscheck.util.MultiMap;
import dk.webbies.tajscheck.util.Util;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by erik1 on 04-01-2017.
 */
public class FreeGenericsFinder {
    private final Set<Type> hasThisTypes;

    public FreeGenericsFinder(Type global) {
        this.hasThisTypes = findHasThisTypes(global);
    }

    public boolean hasThisTypes(Type type) {
        return hasThisTypes.contains(type);
    }

    public void addHasThisTypes(Type type) {
        this.hasThisTypes.add(type);
    }

    private static Set<Type> findHasThisTypes(Type global) {
        Set<Type> allTypes = TypesUtil.collectAllTypes(global);

        MultiMap<Type, Type> reverseBaseTypeMap = new ArrayListMultiMap<>();

        for (Type type : allTypes) {
            if (type instanceof GenericType) {
                for (Type baseType : ((GenericType) type).getBaseTypes()) {
                    reverseBaseTypeMap.put(baseType, type);
                }
                reverseBaseTypeMap.put(((GenericType) type).toInterface(), type);
            } else if (type instanceof InterfaceType) {
                for (Type baseType : ((InterfaceType) type).getBaseTypes()) {
                    reverseBaseTypeMap.put(baseType, type);
                }
            } else if (type instanceof ClassType) {
                for (Type baseType : ((ClassType) type).getBaseTypes()) {
                    reverseBaseTypeMap.put(baseType, type);
                }
            } else if (type instanceof ReferenceType) {
                reverseBaseTypeMap.put(((ReferenceType) type).getTarget(), type);
            } else if (type instanceof ClassInstanceType) {
                InterfaceType instanceType = ((ClassType) ((ClassInstanceType) type).getClassType()).getInstanceType();
                reverseBaseTypeMap.put(instanceType, type);
            }
        }

        Set<Type> result = new HashSet<>();

        List<Type> addQueue = allTypes.stream().filter(ThisType.class::isInstance).map(type -> ((ThisType)type).getConstraint()).collect(Collectors.toList());

        while (!addQueue.isEmpty()) {
            List<Type> copy = new ArrayList<>(addQueue);
            addQueue.clear();
            for (Type type : copy) {
                if (result.contains(type)) {
                    continue;
                }
                if (type instanceof ClassInstanceType) {
                    addQueue.add(((ClassInstanceType) type).getClassType());
                } else if (type instanceof ReferenceType) {
                    addQueue.add(((ReferenceType) type).getTarget());
                } else if (type instanceof ClassType) {
                    addQueue.add(((ClassType) type).getInstanceType());
                } else if (type instanceof GenericType) {
                    addQueue.add(((GenericType) type).toInterface());
                }
                result.add(type);
                addQueue.addAll(reverseBaseTypeMap.get(type));
            }
        }


        return result;
    }

    private final MultiMap<Type, TypeParameterType> freeGenerics = new HashSetMultiMap<>();
    private final Set<Type> calculatedTypes = new HashSet<>();

    public Collection<TypeParameterType> findFreeGenerics(Type type) {
        if (calculatedTypes.contains(type)) {
            return freeGenerics.get(type);
        } else {
            type.accept(new FindReachableTypeParameters(type, freeGenerics), Collections.emptySet());
            calculatedTypes.add(type);
            return freeGenerics.get(type);
        }
    }

    private static final class FindReachableTypeParameters implements TypeVisitorWithArgument<Void, Set<TypeParameterType>> {
        final Set<Type> seen = new HashSet<>();
        private Type baseType;
        private MultiMap<Type, TypeParameterType> result;

        private FindReachableTypeParameters(Type baseType, MultiMap<Type, TypeParameterType> result) {
            this.baseType = baseType;
            this.result = result;
        }

        @Override
        public Void visit(AnonymousType t, Set<TypeParameterType> mapped) {
            return null;
        }

        @Override
        public Void visit(ClassType t, Set<TypeParameterType> mapped) {
            if (seen.contains(t)) {
                return null;
            }
            seen.add(t);
            if (addExisting(t, mapped)) return null;

            t.getInstanceType().accept(this, mapped);

            for (Signature signature : t.getSignatures()) {
                assert signature.getResolvedReturnType() == null;
                for (Signature.Parameter parameter : signature.getParameters()) {
                    parameter.getType().accept(this, mapped);
                }
            }

            t.getBaseTypes().forEach(base -> base.accept(this, mapped));
            t.getStaticProperties().values().forEach(prop -> prop.accept(this, mapped));
            t.getTarget().accept(this, mapped);
            t.getTypeArguments().forEach(arg -> arg.accept(this, mapped));

            return null;
        }

        @Override
        public Void visit(GenericType t, Set<TypeParameterType> mapped) {
            if (seen.contains(t)) {
                return null;
            }
            seen.add(t);

            if (addExisting(t, mapped)) return null;

            t.toInterface().accept(this, mapped);

            t.getTarget().accept(this, mapped);

            t.getTypeArguments().forEach(arg -> arg.accept(this, mapped));

            return null;
        }

        @Override
        public Void visit(InterfaceType t, Set<TypeParameterType> mapped) {
            if (seen.contains(t)) {
                return null;
            }
            seen.add(t);
            if (addExisting(t, mapped)) return null;

            t.getTypeParameters().forEach(par -> par.accept(this, mapped));
            t.getBaseTypes().forEach(base -> base.accept(this, mapped));
            t.getDeclaredProperties().values().forEach(prop -> prop.accept(this, mapped));
            for (Signature signature : Util.concat(t.getDeclaredConstructSignatures(), t.getDeclaredCallSignatures())) {
                if (signature.getResolvedReturnType() != null) {
                    signature.getResolvedReturnType().accept(this, mapped);
                }
                signature.getParameters().forEach(par -> par.getType().accept(this, mapped));
            }

            if (t.getDeclaredNumberIndexType() != null) {
                t.getDeclaredNumberIndexType().accept(this, mapped);
            }
            if (t.getDeclaredStringIndexType() != null) {
                t.getDeclaredStringIndexType().accept(this, mapped);
            }

            return null;
        }

        private boolean addExisting(Type t, Set<TypeParameterType> mapped) {
            if (!result.containsKey(t)) {
                return false;
            }
            for (TypeParameterType parameterType : result.get(t)) {
                if (!mapped.contains(parameterType)) {
                    result.put(baseType, parameterType);
                }
            }

            return true;
        }

        @Override
        public Void visit(ReferenceType t, Set<TypeParameterType> orgMapped) {
            if (seen.contains(t)) {
                return null;
            }
            seen.add(t);

            Set<TypeParameterType> mapped = Util.concatSet(orgMapped, Util.cast(TypeParameterType.class, TypesUtil.getTypeParameters(t.getTarget())));

            if (addExisting(t, mapped)) return null;

            t.getTarget().accept(this, mapped);

            t.getTypeArguments().forEach(arg -> arg.accept(this, mapped));

            return null;
        }

        @Override
        public Void visit(SimpleType t, Set<TypeParameterType> mapped) {
            return null;
        }

        @Override
        public Void visit(TupleType t, Set<TypeParameterType> mapped) {
            if (seen.contains(t)) {
                return null;
            }
            seen.add(t);
            if (addExisting(t, mapped)) return null;

            t.getElementTypes().forEach(type -> type.accept(this, mapped));

            return null;
        }

        @SuppressWarnings("Duplicates")
        @Override
        public Void visit(UnionType t, Set<TypeParameterType> mapped) {
            if (seen.contains(t)) {
                return null;
            }
            seen.add(t);
            if (addExisting(t, mapped)) return null;

            t.getElements().forEach(element -> element.accept(this, mapped));

            return null;
        }

        @Override
        public Void visit(TypeParameterType t, Set<TypeParameterType> mapped) {
            if (seen.contains(t)) {
                return null;
            }
            seen.add(t);
            if (addExisting(t, mapped)) return null;


            if (!mapped.contains(t)) {
                result.put(baseType, t);
            }

            if (t.getConstraint() != null) {
                t.getConstraint().accept(this, mapped);
            }

            return null;
        }

        @Override
        public Void visit(StringLiteral t, Set<TypeParameterType> mapped) {
            return null;
        }

        @Override
        public Void visit(BooleanLiteral t, Set<TypeParameterType> mapped) {
            return null;
        }

        @Override
        public Void visit(NumberLiteral t, Set<TypeParameterType> mapped) {
            return null;
        }

        @SuppressWarnings("Duplicates")
        @Override
        public Void visit(IntersectionType t, Set<TypeParameterType> mapped) {
            if (seen.contains(t)) {
                return null;
            }
            seen.add(t);
            if (addExisting(t, mapped)) return null;

            t.getElements().forEach(element -> element.accept(this, mapped));

            return null;
        }

        @Override
        public Void visit(ClassInstanceType t, Set<TypeParameterType> mapped) {
            ((ClassType)t.getClassType()).getInstanceType().accept(this, mapped);

            return null;
        }

        @Override
        public Void visit(ThisType t, Set<TypeParameterType> mapped) {
            if (seen.contains(t)) {
                return null;
            }
            seen.add(t);
            if (addExisting(t, mapped)) return null;

            t.getConstraint().accept(this, mapped);

            return null;
        }

        @Override
        public Void visit(IndexType t, Set<TypeParameterType> mapped) {
            throw new RuntimeException();
        }

        @Override
        public Void visit(IndexedAccessType t, Set<TypeParameterType> mapped) {
            if (seen.contains(t)) {
                return null;
            }
            seen.add(t);
            if (addExisting(t, mapped)) return null;

            t.getIndexType().accept(this, mapped);
            t.getObjectType().accept(this, mapped);

            return null;
        }
    }

    private Map<Type, Boolean> thisTypeVisibleCache = new HashMap<>();
    public boolean isThisTypeVisible(Type baseType, Type thisType) {
        if (thisTypeVisibleCache.containsKey(baseType)) {
            return thisTypeVisibleCache.get(baseType);
        }
        boolean result = isThisTypeVisible(baseType, true, thisType, new HashSet<>());
        thisTypeVisibleCache.put(baseType, result);
        return result;
    }

    private boolean isThisTypeVisible(Type baseType, boolean deep, Type thisType, Set<Type> orgSeen) {
        if (baseType == null) {
            throw new NullPointerException();
        }
        if (orgSeen.contains(baseType)) {
            return false;
        }
        Set<Type> seen = Util.concatSet(orgSeen, Collections.singletonList(baseType));

        if (baseType instanceof ReferenceType) {
            if (((ReferenceType) baseType).getTypeArguments().stream().anyMatch(arg -> isThisTypeVisible(arg, deep, thisType, seen))) {
                return true;
            }
            baseType = ((ReferenceType) baseType).getTarget();
        }
        if (baseType instanceof ClassType) {
            return false; // A classType is the "static" context, and not an "instance" context, therefore no this-types are visible.
        }
        if (baseType instanceof GenericType) {
            baseType = ((GenericType) baseType).toInterface();
        }
        if (baseType instanceof ClassInstanceType) {
            baseType = ((ClassType) ((ClassInstanceType) baseType).getClassType()).getInstanceType();
        }
        if (baseType instanceof InterfaceType) {
            InterfaceType inter = (InterfaceType) baseType;

            for (Signature signature : Util.concat(inter.getDeclaredCallSignatures(), inter.getDeclaredConstructSignatures())) {
                if (signature.getParameters().stream().map(Signature.Parameter::getType).anyMatch(par -> isThisTypeVisible(par, false, thisType, seen))) {
                    return true;
                }
                if (signature.getResolvedReturnType() != null && isThisTypeVisible(signature.getResolvedReturnType(), false, thisType, seen)) {
                    return true;
                }
            }


            if (!deep) {
                return false;
            }
            for (Type type : inter.getDeclaredProperties().values()) {
                if (isThisTypeVisible(type, false, thisType, seen)) {
                    return true;
                }
            }
            return false;
        }
        if (baseType instanceof SimpleType || baseType instanceof BooleanLiteral || baseType instanceof StringLiteral || baseType instanceof NumberLiteral || baseType instanceof TypeParameterType || baseType instanceof AnonymousType) {
            return false;
        }

        if (baseType instanceof ThisType) {
            Type constraint = TypesUtil.normalize(((ThisType) baseType).getConstraint());
            return TypesUtil.getAllBaseTypes(thisType).stream().anyMatch(base -> constraint.equals(TypesUtil.normalize(base)));
        }
        if (baseType instanceof UnionType) {
            return ((UnionType) baseType).getElements().stream().anyMatch(element -> isThisTypeVisible(element, deep, thisType, seen));
        }
        if (baseType instanceof IntersectionType) {
            return ((IntersectionType) baseType).getElements().stream().anyMatch(element -> isThisTypeVisible(element, deep, thisType, seen));
        }
        if (baseType instanceof TupleType) {
            return ((TupleType) baseType).getElementTypes().stream().anyMatch(element -> isThisTypeVisible(element, deep, thisType, seen));
        }
        if (baseType instanceof IndexedAccessType) {
            return isThisTypeVisible(((IndexedAccessType) baseType).getIndexType(), deep, thisType, seen) || isThisTypeVisible(((IndexedAccessType) baseType).getObjectType(), deep, thisType, seen);
        }
        throw new RuntimeException(baseType.getClass().getSimpleName());
    }

    static int counter = 0;
}
