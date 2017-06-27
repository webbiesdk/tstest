package dk.webbies.tajscheck.buildprogram.typechecks;

import dk.webbies.tajscheck.testcreator.test.check.Check;

/**
 * Created by erik1 on 21-11-2016.
 */
public interface TypeCheck {
    String getExpected();

    Check getCheck();
}
