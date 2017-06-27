package dk.webbies.tajscheck.testcreator.test;

import dk.au.cs.casa.typescript.types.Signature;
import dk.au.cs.casa.typescript.types.Type;
import dk.webbies.tajscheck.typeutil.typeContext.TypeContext;

import java.util.Collections;
import java.util.List;

/**
 * Created by erik1 on 02-11-2016.
 */
public class ConstructorCallTest extends FunctionTest {
    private final Type function;

    public ConstructorCallTest(Type function, List<Type> parameters, Type returnType, String path, TypeContext typeContext, boolean restArgs, List<Signature> precedingSignatures) {
        super(Collections.singletonList(function), parameters, returnType, path + ".new", typeContext, precedingSignatures, restArgs);
        this.function = function;
    }

    public Type getFunction() {
        return function;
    }

    @Override
    public boolean equalsNoPath(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return super.equalsNoPathBase((Test) o);
    }

    @Override
    public int hashCodeNoPath() {
        return super.hashCodeNoPathBase();
    }

    @Override
    public String getTestType() {
        return "constructor call";
    }

    @Override
    public <T> T accept(TestVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
