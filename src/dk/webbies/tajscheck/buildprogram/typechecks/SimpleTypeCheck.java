package dk.webbies.tajscheck.buildprogram.typechecks;

import dk.webbies.tajscheck.testcreator.test.check.Check;

/**
 * Created by erik1 on 21-11-2016.
 */
public class SimpleTypeCheck implements TypeCheck {
    private final Check check;
    private final String expected;

    public SimpleTypeCheck(Check check, String expected) {
        this.check = check;
        this.expected = expected;
    }

    public String getExpected() {
        return expected;
    }

    public Check getCheck() {
        return check;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SimpleTypeCheck that = (SimpleTypeCheck) o;

        if (check != null ? !check.equals(that.check) : that.check != null) return false;
        return expected != null ? expected.equals(that.expected) : that.expected == null;
    }

    @Override
    public int hashCode() {
        int result = check != null ? check.hashCode() : 0;
        result = 31 * result + (expected != null ? expected.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName()+"(" + check + ", " + expected + ")";
    }
}
