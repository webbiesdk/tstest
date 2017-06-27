package dk.webbies.tajscheck.testcreator.test.check;

import dk.webbies.tajscheck.paser.AST.Expression;

/**
 * Created by erik1 on 14-11-2016.
 */
public class EqualityCheck implements Check {
    private final Expression expression;

    EqualityCheck(Expression expression) {
        this.expression = expression;
    }

    public Expression getExpression() {
        return expression;
    }

    @Override
    public <T, A> T accept(CheckVisitorWithArgument<T, A> visitor, A a) {
        return visitor.visit(this, a);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        EqualityCheck that = (EqualityCheck) o;

        return expression != null ? expression.equals(that.expression) : that.expression == null;
    }

    @Override
    public int hashCode() {
        return expression != null ? expression.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "equals(" + expression +")";
    }
}
