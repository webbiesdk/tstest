package dk.webbies.tajscheck.testcreator.test.check;

import dk.webbies.tajscheck.paser.AST.Expression;
import dk.webbies.tajscheck.paser.AstBuilder;
import dk.webbies.tajscheck.paser.AstToStringVisitor;

import java.util.function.Function;

/**
 * Created by erik1 on 04-01-2017.
 */
public class ExpressionCheck implements Check {
    private Function<Expression, Expression> generator;

    public ExpressionCheck(Function<Expression, Expression> generator) {
        this.generator = generator;
    }

    public Function<Expression, Expression> getGenerator() {
        return generator;
    }

    @Override
    public <T, A> T accept(CheckVisitorWithArgument<T, A> visitor, A a) {
        return visitor.visit(this, a);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        return genRepresentative().equals(((ExpressionCheck) o).genRepresentative());
    }

    @Override
    public int hashCode() {
        return genRepresentative().hashCode();
    }

    private String representative = null;
    private String genRepresentative() {
        if (representative == null) {
            return representative = AstToStringVisitor.toString(generator.apply(AstBuilder.identifier("foo")));
        } else {
            return representative;
        }
    }

    @Override
    public String toString() {
        return "expression(" + generator +")";
    }
}
