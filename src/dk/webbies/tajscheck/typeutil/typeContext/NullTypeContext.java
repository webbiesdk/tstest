package dk.webbies.tajscheck.typeutil.typeContext;

import dk.au.cs.casa.typescript.types.SimpleType;
import dk.au.cs.casa.typescript.types.SimpleTypeKind;
import dk.au.cs.casa.typescript.types.Type;
import dk.au.cs.casa.typescript.types.TypeParameterType;
import dk.webbies.tajscheck.TypeWithContext;
import dk.webbies.tajscheck.benchmark.Benchmark;

import java.util.Collections;
import java.util.Map;

/**
 * Created by erik1 on 12-01-2017.
 */
public class NullTypeContext implements TypeContext {
    private static final Type any = new SimpleType(SimpleTypeKind.Any);
    private final Benchmark benchmark;

    public NullTypeContext(Benchmark benchmark) {
        this.benchmark = benchmark;
    }

    @Override
    public TypeContext append(Map<TypeParameterType, Type> newParameters) {
        return this;
    }

    @Override
    public TypeContext withThisType(Type thisType) {
        return this;
    }

    @Override
    public boolean containsKey(TypeParameterType parameter) {
        return true;
    }

    @Override
    public TypeWithContext get(TypeParameterType parameter) {
        return new TypeWithContext(any, this);
    }

    @Override
    public Map<TypeParameterType, Type> getMap() {
        return Collections.emptyMap();
    }

    @Override
    public Type getThisType() {
        return any;
    }

    @Override
    public TypeContext append(TypeContext other) {
        return this;
    }

    @Override
    public TypeContext optimizeTypeParameters(Type baseType) {
        return this;
    }

    @Override
    public int hashCode() {
        return 1;
    }

    @Override
    public boolean equals(Object obj) {
        return obj.getClass().equals(this.getClass());
    }
}
