package dk.webbies.tajscheck.typeutil.typeContext;

import dk.au.cs.casa.typescript.types.*;
import dk.webbies.tajscheck.TypeWithContext;
import dk.webbies.tajscheck.benchmark.BenchmarkInfo;
import dk.webbies.tajscheck.typeutil.TypesUtil;
import dk.webbies.tajscheck.util.Pair;
import dk.webbies.tajscheck.util.Util;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by erik1 on 12-01-2017.
 */
public class OptimizingTypeContext implements TypeContext {
    private final Map<TypeParameterType, Type> map;
    private final Type thisType;
    private final BenchmarkInfo info;
    private final Map<OptimizingTypeContext, OptimizingTypeContext> cache;
    private final Map<Pair<Type, OptimizingTypeContext>, OptimizingTypeContext> optimizationCache;

    private OptimizingTypeContext cannonicalize() {
        OptimizingTypeContext hit = cache.get(this);
        if (hit != null) {
            return hit;
        }
        cache.put(this, this);
        return this;
    }

    static OptimizingTypeContext create(BenchmarkInfo info) {
        return new OptimizingTypeContext(info).cannonicalize();
    }

    private OptimizingTypeContext(BenchmarkInfo info) {
        this(
                Collections.emptyMap(),
                null,
                info,
                info.getAttribute(OptimizingTypeContext.class, "cache", HashMap::new),
                info.getAttribute(OptimizingTypeContext.class, "optimizationCache", HashMap::new)
        );
    }

    private OptimizingTypeContext(Map<TypeParameterType, Type> map, Type thisType, BenchmarkInfo info, Map<OptimizingTypeContext, OptimizingTypeContext> cache, Map<Pair<Type, OptimizingTypeContext>, OptimizingTypeContext> optimizationCache) {
        this.map = map;
        this.thisType = thisType;
        this.info = info;
        this.cache = cache;
        this.optimizationCache = optimizationCache;
    }

    @Override
    public OptimizingTypeContext append(Map<TypeParameterType, Type> newParameters) {
        if (newParameters.isEmpty()) {
            return this;
        }
        newParameters = newParameters.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, entry -> {
            Type value = entry.getValue();
            if (!this.map.containsKey(entry.getKey())) {
                return value;
            }
            int counter = 0;
            while (value instanceof TypeParameterType && this.map.containsKey(value)) {
                if (counter++ > 10) {
                    break;
                }
                value = this.map.get(value);
            }
            return value;
        }));
        Map<TypeParameterType, Type> newMap = new HashMap<>(this.map);
        newMap.putAll(newParameters);
        return new OptimizingTypeContext(newMap, this.thisType, info, cache, optimizationCache).cannonicalize();
    }

    @Override
    public OptimizingTypeContext withThisType(Type thisType) {
        if (thisType == null || this.thisType == null) {
            return new OptimizingTypeContext(this.map, thisType, info, cache, optimizationCache);
        }
        Set<Type> baseTypes = info.typesUtil.getAllBaseTypes(this.thisType, new HashSet<>());

        if (baseTypes.contains(thisType)) {
            return this;
        } else {
            return new OptimizingTypeContext(this.map, thisType, info, cache, optimizationCache).cannonicalize();
        }
    }

    @Override
    public boolean containsKey(TypeParameterType parameter) {
        return map.containsKey(parameter);
    }

    @Override
    public TypeWithContext get(TypeParameterType parameter) {
        Type type = map.get(parameter);
        if (type == null) {
            return null;
        }
        OptimizingTypeContext context = this;
        if (info.freeGenericsFinder.findFreeGenerics(type).contains(parameter)) {
            HashMap<TypeParameterType, Type> appendMap = new HashMap<>();
            appendMap.put(parameter, ANY);
            context = context.append(appendMap);
        }

        return new TypeWithContext(type, context.optimizeTypeParameters(type));
    }

    public static final SimpleType ANY = new SimpleType(SimpleTypeKind.Any);


    @Override
    public Map<TypeParameterType, Type> getMap() {
        return map;
    }

    @Override
    public Type getThisType() {
        return thisType;
    }

    @Override
    public TypeContext append(TypeContext other) {
        if (other instanceof OptimizingTypeContext) {
            return append(other.getMap());
        } else {
            throw new RuntimeException();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        OptimizingTypeContext that = (OptimizingTypeContext) o;

        if (map != null ? !map.equals(that.map) : that.map != null) return false;
        if (thisType != null ? !thisType.equals(that.thisType) : that.thisType != null) return false;
        return info != null ? info.equals(that.info) : that.info == null;
    }

    @Override
    public int hashCode() {
        int result = map != null ? map.hashCode() : 0;
        result = 31 * result + (thisType != null ? thisType.hashCode() : 0);
        result = 31 * result + (info != null ? info.hashCode() : 0);
        return result;
    }

    @Override
    public OptimizingTypeContext optimizeTypeParameters(Type baseType) {
        Pair<Type, OptimizingTypeContext> key = new Pair<>(baseType, this);
        if (optimizationCache.containsKey(key)) {
            return optimizationCache.get(key);
        }
        OptimizingTypeContext result = uncannocilizatingOptimizeTypeParameters(baseType).cannonicalize();
        optimizationCache.put(key, result);
        return result;
    }

    private OptimizingTypeContext uncannocilizatingOptimizeTypeParameters(Type baseType) {
        info.freeGenericsFinder.isThisTypeVisible(baseType, this.thisType);

        if (info.bench.options.disableSizeOptimization) {
            return this;
        }
        OptimizingTypeContext clone = new OptimizingTypeContext(new HashMap<>(this.map), this.thisType, this.info, this.cache, optimizationCache);

        Set<TypeParameterType> reachable = new HashSet<>(info.freeGenericsFinder.findFreeGenerics(baseType));

        clone.map.keySet().retainAll(Util.concat(reachable, this.map.values()));

        boolean progress = true;
        while (progress) {
            progress = false;
            Set<TypeParameterType> extraReachable = new HashSet<>();
            for (Type type : clone.map.values()) {
                extraReachable.addAll(info.freeGenericsFinder.findFreeGenerics(type));
            }
            for (TypeParameterType parameterType : extraReachable) {
                if (!reachable.contains(parameterType) && this.map.containsKey(parameterType)) {
                    reachable.add(parameterType);
                    clone.map.put(parameterType, this.map.get(parameterType));
                    progress = true;
                }
            }
        }

        if (clone.thisType != null) {
            OptimizingTypeContext finalClone = clone;
            if (!info.freeGenericsFinder.isThisTypeVisible(baseType, clone.thisType) && clone.map.values().stream().noneMatch(value -> info.freeGenericsFinder.isThisTypeVisible(value, finalClone.thisType))) {
                clone = clone.withThisType(null);
            } else if ((baseType instanceof InterfaceType || baseType instanceof GenericType || baseType instanceof ClassInstanceType) && !info.freeGenericsFinder.isThisTypeVisible(baseType, finalClone.thisType) && clone.map.values().stream().noneMatch(value -> info.freeGenericsFinder.isThisTypeVisible(value, finalClone.thisType))) {
                Set<Type> allBaseTypes = info.typesUtil.getAllBaseTypes(clone.thisType, new HashSet<>());
                if (!allBaseTypes.contains(baseType)) {
                    clone = clone.withThisType(null);
                }
            }
        }
        return clone;
    }

    @Override
    public String toString() {
        if (map.isEmpty() && thisType == null) {
            return "TypeContext{}";
        }

        return "TypeContext{" +
                "map=" + map +
                ", thisType=" + thisType +
                '}';
    }
}
