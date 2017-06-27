package dk.webbies.tajscheck;

import dk.webbies.tajscheck.util.Util;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by erik1 on 23-11-2016.
 */
public class OutputParser {
    public static final class TypeError {
        public String path;
        public String expected;
        public String typeof;
        public String toString;
        public String JSON;
        public String type;
        public Set<Integer> testsCalled;

        public TypeError(String path, String expected, String typeof, String toString, String JSON, String type, Set<Integer> testsCalled) {
            this.path = path;
            this.expected = expected;
            this.typeof = typeof;
            this.toString = toString;
            this.JSON = JSON;
            this.type = type;
            this.testsCalled = testsCalled;
        }

        @Override
        public String toString() {
            return "*** Type error\n" +
                    "  " + this.type + ": " + this.path + "\n" +
                    "  expected: " + expected + "\n" +
                    "  observed(typeof): " + typeof + "\n" +
                    "  observed(string): " + toString + "\n" +
                    "  observed(JSON): " + JSON + "\n";
        }

        public String getPath() {
            return path;
        }
    }

    public static final class RunResult {
        public final List<TypeError> typeErrors;
        public final List<String> errors;
        private final Integer totalTests;
        private final Set<Integer> testsCalled;

        public RunResult(List<TypeError> typeErrors, List<String> errors, Integer totalTests, Set<Integer> testsCalled) {
            this.typeErrors = typeErrors;
            this.errors = errors;
            this.totalTests = totalTests;
            this.testsCalled = testsCalled;
        }

        public List<TypeError> getTypeErrors() {
            return typeErrors;
        }

        public List<String> getErrors() {
            return errors;
        }

        public Integer getTotalTests() {
            return totalTests;
        }

        public Set<Integer> getTestsCalled() {
            return testsCalled;
        }
    }

    public static RunResult parseDriverResult(String output) {
        return parseDriverResult(output, false, null);
    }

    public static RunResult parseDriverResult(String output, boolean collectCalledTests, String filterPath) {
        if (output.isEmpty()) {
            return new RunResult(new ArrayList<>(), new ArrayList<>(), -1, Collections.emptySet());
        }
        List<String> split = Arrays.stream(output.split("\n")).filter(line -> !line.trim().isEmpty()).collect(Collectors.toList());

        int totalTests = -1;
        Set<Integer> testsCalled = new HashSet<>();

        List<String> errors = new ArrayList<>();

        ArrayList<String> splitCopy = new ArrayList<>(split);
        split.clear();
        for (String msg : splitCopy) {
            if (msg.startsWith("error: ")) {
                errors.add(msg.substring("error: ".length(), msg.length()));
            } else {
                split.add(msg);
            }
        }


        List<TypeError> typeErrors = new ArrayList<>();

        assert split.get(0).startsWith("Initial random: ");

        split = split.stream().filter(str -> !str.startsWith("Initial random: ")).collect(Collectors.toList());

        if (split.size() == 0) {
            return new RunResult(new ArrayList<>(), errors, totalTests, testsCalled);
        }

        List<String> singleResultCollector = new ArrayList<>();

        for (int i = 0; i < split.size(); i++) {
            String line = split.get(i);

            if (line.startsWith("Test called: ")) {
                int testCalled = Integer.parseInt(Util.removePrefix(line, "Test called: ").trim());
                testsCalled.add(testCalled);
            } else if (line.startsWith("total number of tests: ")) {
                assert totalTests == -1;
                totalTests = Integer.parseInt(Util.removePrefix(line, "total number of tests: ").trim());
            } else {
                if (!line.startsWith(" ")) {
                    if (!singleResultCollector.isEmpty()) {
                        typeErrors.add(parseSingleResult(singleResultCollector, testsCalled, collectCalledTests, filterPath));
                    }
                    singleResultCollector.clear();
                }
                singleResultCollector.add(line);
            }
        }

        if (!singleResultCollector.isEmpty()) {
            typeErrors.add(parseSingleResult(singleResultCollector, testsCalled, collectCalledTests, filterPath));
        }

        typeErrors = typeErrors.stream().filter(Objects::nonNull).collect(Collectors.toList());

        return new RunResult(typeErrors, errors, totalTests, testsCalled);
    }

    private static TypeError parseSingleResult(List<String> lines, Set<Integer> testsCalled, boolean collectCalledTests, String filterPath) {
        if (!(lines.size() == 6 || lines.size() == 5)) {
            System.out.println("Lines: ");
            lines.forEach(System.out::println);
            return null;
        }
        assert lines.size() == 6 || lines.size() == 5;

        String header = lines.get(0);
        assert header.substring(header.lastIndexOf('('), header.length()).startsWith("(iteration: ");
        if (header.lastIndexOf('(') == -1) {
            throw new RuntimeException();
        }
        header = header.substring(0, header.lastIndexOf('(') - 1);
        assert header.lastIndexOf(':') == header.length() - 1;
        String path = header.substring(0, header.length() - 1);

        String expectStart = "    Here I expected: ";
        assert lines.get(1).startsWith(expectStart);
        String expectFinish = ", but instead I got: ";
        assert lines.get(1).endsWith(expectFinish);

        String expected = lines.get(1).substring(expectStart.length(), lines.get(1).length() - expectFinish.length());

        String describStart = "        descrip: ";
        assert lines.get(2).startsWith(describStart);
        String type = Util.removePrefix(lines.get(2), describStart);

        String typeofPrefix = "        typeof: ";
        assert lines.get(3).startsWith(typeofPrefix);
        String typeof = lines.get(3).substring(typeofPrefix.length(), lines.get(3).length());

        String toStringPrefix = "        toString: ";
        assert lines.get(4).startsWith(toStringPrefix);
        String toString = lines.get(4).substring(toStringPrefix.length(), lines.get(4).length());

        String JSON = null;
        if (lines.size() >= 6) {
            String jsonPrefix = "        JSON: ";
            assert lines.get(5).startsWith(jsonPrefix);
            JSON = lines.get(5).substring(jsonPrefix.length(), lines.get(5).length());
        }

        if (filterPath != null && !filterPath.equals(path)) {
            return null;
        }

        Set<Integer> typeErrorTestsCalled = null;
        if (collectCalledTests) {
            typeErrorTestsCalled = new HashSet<>(testsCalled);
        }
        return new TypeError(path, expected, typeof, toString, JSON, type, typeErrorTestsCalled);
    }

    public static RunResult combine(List<RunResult> results) {
        results = results.stream().filter(Objects::nonNull).collect(Collectors.toList());

        List<String> errors = results.stream().map(res -> res == null ? new RunResult(Collections.emptyList(), Collections.emptyList(), -1, Collections.emptySet()) : res).map(RunResult::getErrors).reduce(new ArrayList<>(), Util::reduceList).stream().distinct().collect(Collectors.toList());

        List<TypeError> typeErrors = results.stream().map(RunResult::getTypeErrors).reduce(new ArrayList<>(), Util::reduceList).stream().distinct().collect(Collectors.toList());

        int totalTests = results.stream().map(RunResult::getTotalTests).reduce(-1, Math::max);

        Set<Integer> testsCalled = results.stream().map(RunResult::getTestsCalled).reduce(new HashSet<>(), Util::reduceSet);

        return new RunResult(typeErrors, errors, totalTests, testsCalled);
    }
}
