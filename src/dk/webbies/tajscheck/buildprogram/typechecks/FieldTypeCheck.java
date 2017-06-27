package dk.webbies.tajscheck.buildprogram.typechecks;

import dk.webbies.tajscheck.buildprogram.TypeChecker;
import dk.webbies.tajscheck.testcreator.test.check.Check;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by erik1 on 21-11-2016.
 */
public class FieldTypeCheck implements TypeCheck {
    private final String field;
    private final List<TypeCheck> fieldChecks;

    public FieldTypeCheck(String field, List<TypeCheck> fieldChecks) {
        this.field = field;
        this.fieldChecks = fieldChecks;
    }

    public String getField() {
        return field;
    }

    public List<TypeCheck> getFieldChecks() {
        return fieldChecks;
    }

    @Override
    public String getExpected() {
        return "field[" + field + "]:(" + TypeChecker.createIntersectionDescription(fieldChecks) + ")";
    }

    @Override
    public Check getCheck() {
        return Check.field(this.field, fieldChecks.stream().map(TypeCheck::getCheck).collect(Collectors.toList()));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FieldTypeCheck that = (FieldTypeCheck) o;

        if (field != null ? !field.equals(that.field) : that.field != null) return false;
        return fieldChecks != null ? fieldChecks.equals(that.fieldChecks) : that.fieldChecks == null;
    }

    @Override
    public int hashCode() {
        int result = field != null ? field.hashCode() : 0;
        result = 31 * result + (fieldChecks != null ? fieldChecks.hashCode() : 0);
        return result;
    }
}
