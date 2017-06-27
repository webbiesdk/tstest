package dk.webbies.tajscheck.testcreator.test.check;

import dk.webbies.tajscheck.paser.AST.Expression;

public class InstanceOfCheck implements Check {
    private Expression exp;

    public InstanceOfCheck(Expression exp) {
        this.exp = exp;
    }

    public Expression getExp() {
        return exp;
    }

    @Override
    public <T, A> T accept(CheckVisitorWithArgument<T, A> visitor, A a) {
        return visitor.visit(this, a);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        InstanceOfCheck that = (InstanceOfCheck) o;

        return exp != null ? exp.equals(that.exp) : that.exp == null;
    }

    @Override
    public int hashCode() {
        return exp != null ? exp.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "instanceof(" + exp +")";
    }
}
