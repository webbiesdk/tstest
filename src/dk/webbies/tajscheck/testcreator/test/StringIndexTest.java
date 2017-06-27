package dk.webbies.tajscheck.testcreator.test;

import dk.au.cs.casa.typescript.types.Type;
import dk.webbies.tajscheck.typeutil.typeContext.TypeContext;

import java.util.Collections;

/**
 * Created by erik1 on 02-11-2016.
 */
public class StringIndexTest extends Test {
    private final Type obj;
    private final Type returnType;

    public StringIndexTest(Type obj, Type returnType, String path, TypeContext typeContext) {
        super(Collections.singletonList(obj), Collections.emptyList(), returnType, path + ".[stringIndexer]", typeContext);
        this.obj = obj;
        this.returnType = returnType;
    }

    public Type getObj() {
        return obj;
    }

    public Type getReturnType() {
        return returnType;
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
        return "string-indexer access";
    }

    @Override
    public <T> T accept(TestVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
