package dk.webbies.tajscheck.testcreator.test;

import dk.au.cs.casa.typescript.SpecReader;
import dk.au.cs.casa.typescript.types.Type;
import dk.webbies.tajscheck.typeutil.typeContext.TypeContext;

import java.util.Collections;

/**
 * Created by erik1 on 02-11-2016.
 */
public class PropertyWriteTest extends Test {
    private Type baseType;
    private Type toWrite;
    private final String key;

    private static final Type produces = SpecReader.makeEmptySyntheticInterfaceType();

    public PropertyWriteTest(Type baseType, Type toWrite, String key, String path, TypeContext typeContext) {
        super(Collections.singletonList(baseType), Collections.singletonList(toWrite), produces, path, typeContext);
        this.baseType = baseType;
        this.toWrite = toWrite;
        this.key = key;
    }

    public Type getToWrite() {
        return toWrite;
    }

    public String getProperty() {
        return key;
    }

    public Type getBaseType() {
        return baseType;
    }

    @Override
    public boolean equalsNoPath(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PropertyWriteTest test = (PropertyWriteTest) o;
        if (!test.key.equals(this.key)) return false;
        return super.equalsNoPathBase(test);
    }

    @Override
    public int hashCodeNoPath() {
        return super.hashCodeNoPathBase() + this.key.hashCode();
    }

    @Override
    public String getTestType() {
        return "property write";
    }

    @Override
    public <T> T accept(TestVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
