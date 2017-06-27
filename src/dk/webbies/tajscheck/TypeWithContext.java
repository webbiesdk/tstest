package dk.webbies.tajscheck;

import dk.au.cs.casa.typescript.types.Type;
import dk.webbies.tajscheck.typeutil.typeContext.TypeContext;

public class TypeWithContext {
    private final Type type;
    private final TypeContext typeContext;

    public TypeWithContext(Type type, TypeContext typeContext) {
        if (type == null) {
            throw new NullPointerException();
        }
        this.type = type;
        this.typeContext = typeContext;
    }

    public Type getType() {
        return type;
    }

    public TypeContext getTypeContext() {
        return typeContext;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TypeWithContext that = (TypeWithContext) o;

        if (!type.equals(that.type)) return false;
        return typeContext.equals(that.typeContext);

    }

    @Override
    public int hashCode() {
        int result = type.hashCode();
        result = 31 * result + typeContext.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "<" + type + ", " + typeContext + ">";
    }
}
