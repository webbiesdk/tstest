package dk.webbies.tajscheck.testcreator.test.check;

import java.util.List;

import static dk.webbies.tajscheck.util.Util.mkString;

/**
 * Created by erik1 on 14-11-2016.
 */
public class OrCheck implements Check {
    private final List<Check> checks;

    OrCheck(List<Check> checks) {
        this.checks = checks;
    }

    public List<Check> getChecks() {
        return checks;
    }

    @Override
    public <T, A> T accept(CheckVisitorWithArgument<T, A> visitor, A a) {
        return visitor.visit(this, a);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        OrCheck orCheck = (OrCheck) o;

        return checks != null ? checks.equals(orCheck.checks) : orCheck.checks == null;
    }

    @Override
    public int hashCode() {
        return checks != null ? checks.hashCode() : 0;
    }

    @Override
    public String toString() {
        return mkString(checks.stream(), " or ");
    }
}
