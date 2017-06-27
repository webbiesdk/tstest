package dk.webbies.tajscheck.testcreator.test.check;

import java.util.List;

import static dk.webbies.tajscheck.util.Util.mkString;

public class FieldCheck implements Check {
    private final List<Check> checks;
    private final String field;

    public FieldCheck(List<Check> checks, String field) {
        this.checks = checks;
        this.field = field;
    }

    public List<Check> getChecks() {
        return checks;
    }

    public String getField() {
        return field;
    }

    @Override
    public <T, A> T accept(CheckVisitorWithArgument<T, A> visitor, A a) {
        return visitor.visit(this, a);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FieldCheck that = (FieldCheck) o;

        if (checks != null ? !checks.equals(that.checks) : that.checks != null) return false;
        return field != null ? field.equals(that.field) : that.field == null;
    }

    @Override
    public int hashCode() {
        int result = checks != null ? checks.hashCode() : 0;
        result = 31 * result + (field != null ? field.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "field(" + field +", " + mkString(checks.stream(), ", ") +")";
    }
}
