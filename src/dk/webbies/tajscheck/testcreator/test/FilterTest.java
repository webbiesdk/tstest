package dk.webbies.tajscheck.testcreator.test;

import dk.au.cs.casa.typescript.types.Type;
import dk.webbies.tajscheck.typeutil.typeContext.TypeContext;
import dk.webbies.tajscheck.paser.AstBuilder;
import dk.webbies.tajscheck.testcreator.test.check.Check;
import dk.webbies.tajscheck.testcreator.test.check.CheckToExpression;

import java.util.Collections;

/**
 * The simplest test there can be, it just loads the type to test, and spits it out the other end.
 */
public class FilterTest extends Test {
    private Type type;
    private Check check;

    public FilterTest(Type type, Type produces, String path, TypeContext typeContext, Check check) {
        super(Collections.singletonList(type), Collections.emptyList(), produces, path, typeContext);
        this.type = type;
        this.check = check;
    }

    public Type getType() {
        return type;
    }

    public Check getCheck() {
        return check;
    }

    @Override
    public <T> T accept(TestVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public boolean equalsNoPath(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!CheckToExpression.generate(check, AstBuilder.nullLiteral()).equals(CheckToExpression.generate(((FilterTest)o).check, AstBuilder.nullLiteral()))) return false;
        return super.equalsNoPathBase((Test) o);
    }

    @Override
    public int hashCodeNoPath() {
        return super.hashCodeNoPathBase();
    }

    @Override
    public String getTestType() {
        return "filter test(never fails)";
    }


    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (type != null ? type.hashCode() : 0);
        return result;
    }

}
