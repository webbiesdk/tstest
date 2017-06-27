package dk.webbies.tajscheck.testcreator.test;

import dk.au.cs.casa.typescript.types.Type;
import dk.webbies.tajscheck.typeutil.typeContext.TypeContext;

import java.util.Collection;
import java.util.Collections;

public abstract class Test {
    private final Collection<Type> typeToTest;
    private final Collection<Type> dependsOn;
    private final Collection<Type> produces;
    private String path;
    private final TypeContext typeContext;

    protected Test(Collection<Type> typeToTest, Collection<Type> dependsOn, Type produces, String path, TypeContext typeContext) {
        this(typeToTest, dependsOn, Collections.singletonList(produces), path, typeContext);
    }

    protected Test(Collection<Type> typeToTest, Collection<Type> dependsOn, Collection<Type> produces, String path, TypeContext typeContext) {
        this.typeToTest = typeToTest;
        this.dependsOn = dependsOn;
        this.produces = produces;
        this.path = path;
        this.typeContext = typeContext;
    }

    public Collection<Type> getDependsOn() {
        return dependsOn;
    }

    public Collection<Type> getTypeToTest() {
        return typeToTest;
    }

    public Collection<Type> getProduces() {
        return produces;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public abstract <T> T accept(TestVisitor<T> visitor);

    public boolean equalsNoPathBase(Test test) {
        if (!typeToTest.equals(test.typeToTest)) return false;
        if (!dependsOn.equals(test.dependsOn)) return false;
        if (!produces.equals(test.produces)) return false;
        return typeContext.equals(test.typeContext);
    }

    public abstract boolean equalsNoPath(Object o);

    public int hashCodeNoPathBase() {
        int result = typeToTest.hashCode();
        result = 31 * result + dependsOn.hashCode();
        result = 31 * result + produces.hashCode();
        result = 31 * result + typeContext.hashCode();
        return result;
    }

    public abstract int hashCodeNoPath();

    public TypeContext getTypeContext() {
        return typeContext;
    }

    public abstract String getTestType();

    @Override
    public String toString() {
        return this.getClass().getSimpleName()+"(" + getPath() + ")";
    }
}
