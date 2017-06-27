package dk.webbies.tajscheck.typeutil.typeContext;

import dk.au.cs.casa.typescript.types.*;
import dk.webbies.tajscheck.TypeWithContext;
import dk.webbies.tajscheck.benchmark.BenchmarkInfo;

import java.util.*;

/**
 * Created by erik1 on 14-11-2016.
 */
public interface TypeContext {
    public TypeContext append(Map<TypeParameterType, Type> newParameters);

    public TypeContext withThisType(Type thisType);

    public boolean containsKey(TypeParameterType parameter);

    public TypeWithContext get(TypeParameterType parameter);

    public Map<TypeParameterType, Type> getMap();

    public Type getThisType();

    public TypeContext append(TypeContext other);

    public TypeContext optimizeTypeParameters(Type baseType);

    public static TypeContext create(BenchmarkInfo info) {
        if (info.bench.options.disableGenerics) {
            return new NullTypeContext(info.bench);
        } else {
            return OptimizingTypeContext.create(info);
        }
    }
}
