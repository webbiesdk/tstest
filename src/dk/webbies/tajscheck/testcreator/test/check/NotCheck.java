package dk.webbies.tajscheck.testcreator.test.check;

public class NotCheck implements Check {
    private final Check check;

    NotCheck(Check check) {
        this.check = check;
    }

    public Check getCheck() {
        return check;
    }

    @Override
    public <T, A> T accept(CheckVisitorWithArgument<T, A> visitor, A a) {
        return visitor.visit(this, a);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        NotCheck notCheck = (NotCheck) o;

        return check != null ? check.equals(notCheck.check) : notCheck.check == null;
    }

    @Override
    public int hashCode() {
        return check != null ? check.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "not(" + check +")";
    }
}
