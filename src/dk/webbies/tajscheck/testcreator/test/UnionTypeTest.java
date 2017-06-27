package dk.webbies.tajscheck.testcreator.test;

import dk.au.cs.casa.typescript.types.Type;
import dk.au.cs.casa.typescript.types.UnionType;
import dk.webbies.tajscheck.typeutil.typeContext.TypeContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class UnionTypeTest extends Test {
    private UnionType unionType;

    public UnionTypeTest(UnionType unionType, List<Type> elements, String path, TypeContext typeContext) {
        super(Collections.singletonList(unionType), new ArrayList<>(), elements, path, typeContext);
        this.unionType = unionType;
    }

    public UnionType getGetUnionType() {
        return unionType;
    }

    @Override
    public <T> T accept(TestVisitor<T> visitor) {
        return visitor.visit(this);
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
        return "union type test";
    }
}
