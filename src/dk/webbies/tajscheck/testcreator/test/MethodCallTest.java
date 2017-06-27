package dk.webbies.tajscheck.testcreator.test;

import dk.au.cs.casa.typescript.types.Signature;
import dk.au.cs.casa.typescript.types.Type;
import dk.webbies.tajscheck.typeutil.typeContext.TypeContext;

import java.util.Arrays;
import java.util.List;

/**
 * Created by erik1 on 02-11-2016.
 */
public class MethodCallTest extends FunctionTest {
    private Type object;
    private final String propertyName;

    public MethodCallTest(Type object, Type function, String propertyName, List<Type> parameters, Type returnType, String path, TypeContext typeContext, boolean restArgs, List<Signature> precedingSignatures) {
        super(Arrays.asList(object, function), parameters, returnType, path, typeContext, precedingSignatures, restArgs);
        this.object = object;
        this.propertyName = propertyName;
    }

    public Type getObject() {
        return object;
    }

    public String getPropertyName() {
        return propertyName;
    }

    @Override
    public boolean equalsNoPath(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MethodCallTest test = (MethodCallTest) o;
        if (!test.propertyName.equals(this.propertyName)) return false;
        return super.equalsNoPathBase(test);
    }

    @Override
    public int hashCodeNoPath() {
        return super.hashCodeNoPathBase() + this.propertyName.hashCode();
    }

    @Override
    public String getTestType() {
        return "method call";
    }

    @Override
    public <T> T accept(TestVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName()+"(" + object + "." + propertyName + "())";
    }
}
