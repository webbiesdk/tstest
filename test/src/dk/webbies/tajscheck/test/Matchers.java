package dk.webbies.tajscheck.test;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;

import java.util.Map;

/**
 * Created by erik1 on 22-03-2017.
 */
public class Matchers {
    public static Matcher<Map<?, ?>> emptyMap() {
        return new BaseMatcher<Map<?, ?>>() {
            @Override
            public boolean matches(Object o) {
                return o instanceof Map && ((Map) o).isEmpty();
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("a non-empty map");
            }
        };
    }
}
