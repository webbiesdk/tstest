package dk.webbies.tajscheck.test;

import dk.webbies.tajscheck.testcreator.TestCreator;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 * Created by erik1 on 16-01-2017.
 */
public class TestVarious {
    @Test
    public void simplifyPath() throws Exception {
        assertThat(TestCreator.simplifyPath("moment(obj, number)"), is(equalTo("moment()")));

        assertThat(TestCreator.simplifyPath("moment(obj, number).stuff"), is(equalTo("moment().stuff")));

        assertThat(TestCreator.simplifyPath("moment(obj, number).stuff(obj, number).new(foo).blaa"), is(equalTo("moment().stuff().new().blaa")));
    }
}
