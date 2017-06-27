package dk.webbies.tajscheck;

import dk.webbies.tajscheck.util.Pair;
import dk.webbies.tajscheck.util.Util;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by erik1 on 09-01-2017.
 */
public class CoverageResult {
    private final Map<SourceLocation, Integer> statements;
    private final Map<SourceLocation, Collection<Integer>> branches;
    private final Map<SourceLocation, Integer> functions;

    public CoverageResult(Map<SourceLocation, Integer> statements, Map<SourceLocation, Collection<Integer>> branches, Map<SourceLocation, Integer> functions) {
        this.statements = statements;
        this.branches = branches;
        this.functions = functions;
    }

    public double statementCoverage() {
        return (statementCount() / (statements.size() * 1.0));
    }

    private long statementCount() {
        return statements.values().stream().filter(n -> n > 0).count();
    }

    public double functionCoverage() {
        return (functionCount() / (functions.size() * 1.0));
    }

    private long functionCount() {
        return functions.values().stream().filter(n -> n > 0).count();
    }

    public double branchCoverage() {
        return (branchCount() * 1.0) / branchTotal();
    }

    public int branchTotal() {
        int total = 0;
        for (Collection<Integer> coverageList : branches.values()) {
            total += coverageList.size();
        }
        return total;
    }

    public int branchCount() {
        int count = 0;
        for (Collection<Integer> coverageList : branches.values()) {
            for (Integer integer : coverageList) {
                if (integer > 0) {
                    count++;
                }
            }

        }

        return count;
    }


    @Override
    public String toString() {
        return "CoverageResult{" +
                "statements=" + statementCount() + "/" + statements.size() + "(" + Util.toFixed(statementCoverage(), 4) + ")" +
                ", branches=" + branchCount() + "/" + branchTotal() + "(" + Util.toFixed(branchCoverage(), 4) + ")" +
                ", functions=" + functionCount() + "/" + functions.size() + "(" + Util.toFixed(functionCoverage(), 4) + ")" +
                '}';
    }

    static Map<String, CoverageResult> parse(String string) {
        try {
            JSONObject obj = new JSONObject(string);

            Map<String, CoverageResult> result = new HashMap<>();

            for (Map.Entry<String, JSONObject> entry : toMap(obj, JSONObject.class).entrySet()) {
                String str = entry.getKey();

                JSONObject subObj = entry.getValue();

                String name = str.substring(str.lastIndexOf("\\") + 1, str.length());
                result.put(name, parseResult(subObj));
            }

            return result;
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    private static <B> Map<String, B> toMap(JSONObject obj, Class<B> valueClass) throws JSONException {
        Map<String, B> result = new HashMap<>();
        for (String key : Util.toTypedList(obj.keys(), String.class)) {
            result.put(key, valueClass.cast(obj.get(key)));
        }

        return result;
    }

    public Map<String, CoverageResult> split(int split, String jsName, String testFileName) {
        Map<SourceLocation, Integer> firstStatements = new HashMap<>();
        Map<SourceLocation, Collection<Integer>> firstBranches = new HashMap<>();
        Map<SourceLocation, Integer> firstFunctions = new HashMap<>();
        CoverageResult first = new CoverageResult(firstStatements, firstBranches, firstFunctions);
        Map<SourceLocation, Integer> secondStatements = new HashMap<>();
        Map<SourceLocation, Collection<Integer>> secondBranches = new HashMap<>();
        Map<SourceLocation, Integer> secondFunctions = new HashMap<>();
        CoverageResult second = new CoverageResult(secondStatements, secondBranches, secondFunctions);

        for (Map.Entry<SourceLocation, Integer> entry : this.statements.entrySet()) {
            if (entry.getKey().start.line < split) {
                firstStatements.put(entry.getKey(), entry.getValue());
            } else {
                secondStatements.put(entry.getKey(), entry.getValue());
            }
        }

        for (Map.Entry<SourceLocation, Collection<Integer>> entry : this.branches.entrySet()) {
            if (entry.getKey().start.line < split) {
                firstBranches.put(entry.getKey(), entry.getValue());
            } else {
                secondBranches.put(entry.getKey(), entry.getValue());
            }
        }

        for (Map.Entry<SourceLocation, Integer> entry : this.functions.entrySet()) {
            if (entry.getKey().start.line < split) {
                firstFunctions.put(entry.getKey(), entry.getValue());
            } else {
                secondFunctions.put(entry.getKey(), entry.getValue());
            }
        }


        Map<String, CoverageResult> resultMap = new HashMap<>();
        resultMap.put(jsName, first);
        resultMap.put(testFileName, second);
        return resultMap;
    }

    public Map<String, CoverageResult> split(Map<String, Pair<Integer, Integer>> splitRules) {
        HashMap<String, CoverageResult> result = new HashMap<>();
        for (Map.Entry<String, Pair<Integer, Integer>> ruleEntry : splitRules.entrySet()) {
            String name = ruleEntry.getKey();
            int start = ruleEntry.getValue().getLeft();
            int end = ruleEntry.getValue().getRight();

            Map<SourceLocation, Integer> statements = new HashMap<>();
            Map<SourceLocation, Collection<Integer>> branches = new HashMap<>();
            Map<SourceLocation, Integer> functions = new HashMap<>();

            for (Map.Entry<SourceLocation, Integer> entry : this.statements.entrySet()) {
                if (entry.getKey().start.line >= start && entry.getKey().start.line < end) {
                    statements.put(entry.getKey(), entry.getValue());
                }
            }

            for (Map.Entry<SourceLocation, Collection<Integer>> entry : this.branches.entrySet()) {
                if (entry.getKey().start.line >= start && entry.getKey().start.line < end) {
                    branches.put(entry.getKey(), entry.getValue());
                }
            }

            for (Map.Entry<SourceLocation, Integer> entry : this.functions.entrySet()) {
                if (entry.getKey().start.line >= start && entry.getKey().start.line < end) {
                    functions.put(entry.getKey(), entry.getValue());
                }
            }

            result.put(name, new CoverageResult(statements, branches, functions));
        }

        return result;
    }

    public static final class SourceLocation {
        public final SourcePosition start;
        public final SourcePosition end;

        public SourceLocation(SourcePosition start, SourcePosition end) {
            this.start = start;
            this.end = end;
        }

        @Override
        public String toString() {
            return "SourceLocation{" +
                    "start=" + start +
                    ", end=" + end +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            SourceLocation that = (SourceLocation) o;
            return (start != null ? start.equals(that.start) : that.start == null) && (end != null ? end.equals(that.end) : that.end == null);
        }

        @Override
        public int hashCode() {
            return 31 * (start != null ? start.hashCode() : 0) + (end != null ? end.hashCode() : 0);
        }
    }

    public static final class SourcePosition {
        public final int line;
        public final int column;

        public SourcePosition(int line, int column) {
            this.line = line;
            this.column = column;
        }

        @Override
        public String toString() {
            return "SourcePosition{" +
                    "line=" + line +
                    ", column=" + column +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            SourcePosition that = (SourcePosition) o;
            return line == that.line && column == that.column;
        }

        @Override
        public int hashCode() {
            return 31 * line + column;
        }
    }

    private static CoverageResult parseResult(JSONObject obj) throws JSONException {
        Map<Integer, Collection<Integer>> branchesCovered = new HashMap<>();
        {
            JSONObject branchObject = obj.getJSONObject("b");
            for (Map.Entry<String, JSONArray> entry : toMap(branchObject, JSONArray.class).entrySet()) {
                JSONArray value = entry.getValue();
                ArrayList<Integer> thisBranchCoverage = new ArrayList<>();
                for (int i = 0; i < value.length(); i++) {
                    thisBranchCoverage.add(value.getInt(i));
                }

                branchesCovered.put(Integer.parseInt(entry.getKey()), thisBranchCoverage);
            }
        }

        Map<String, JSONObject> branchesObject = toMap(obj.getJSONObject("branchMap"), JSONObject.class);

        Map<SourceLocation, Collection<Integer>> branches = branchesCovered.entrySet().stream().map(Util.mapKey(key -> toSourceLocation(branchesObject.get(key.toString())))).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));


        Map<Integer, Integer> statementsCovered = new HashMap<>();
        JSONObject statementObject = obj.getJSONObject("s");
        for (Map.Entry<String, Integer> entry : toMap(statementObject, Integer.class).entrySet()) {
            statementsCovered.put(Integer.parseInt(entry.getKey()), entry.getValue());
        }

        Map<String, JSONObject> statementObjects = toMap(obj.getJSONObject("statementMap"), JSONObject.class);

        Map<SourceLocation, Integer> statements = statementsCovered.entrySet().stream().map(Util.mapKey(key -> toSourceLocation(statementObjects.get(key.toString())))).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));


        Map<Integer, Integer> functionsCovered = new HashMap<>();
        {
            JSONObject functionObject = obj.getJSONObject("f");
            for (Map.Entry<String, Integer> entry : toMap(functionObject, Integer.class).entrySet()) {
                functionsCovered.put(Integer.parseInt(entry.getKey()), entry.getValue());
            }
        }

        Map<String, JSONObject> functionsObject = toMap(obj.getJSONObject("fnMap"), JSONObject.class);

        Map<SourceLocation, Integer> functions = functionsCovered.entrySet().stream().map(Util.mapKey(key -> toSourceLocation(functionsObject.get(key.toString())))).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));


        return new CoverageResult(statements, branches, functions);
    }

    private static SourceLocation toSourceLocation(JSONObject object) {
        try {
            if (object.has("loc")) {
                return toSourceLocation(object.getJSONObject("loc"));
            }
            if (object.has("line")) {
                JSONArray arr = object.getJSONArray("locations");
                List<SourceLocation> locations = new ArrayList<>();
                for (int i = 0; i < arr.length(); i++) {
                    locations.add(toSourceLocation(arr.getJSONObject(i)));
                }

                Comparator<SourcePosition> comparePositions = (a, b) -> {
                    if (a.line < b.line) {
                        return -1;
                    } else if (a.line > b.line) {
                        return 1;
                    }
                    if (a.column < b.column) {
                        return -1;
                    } else if (a.column > b.column) {
                        return 1;
                    }
                    return 0;
                };
                SourcePosition start = locations.stream().min(Comparator.comparing(a -> a.start, comparePositions)).get().start;

                SourcePosition end = locations.stream().min(Comparator.comparing(a -> a.end, comparePositions)).get().end;
                return new SourceLocation(start, end);
            } else {
                return new SourceLocation(toSourcePosition(object.getJSONObject("start")), toSourcePosition(object.getJSONObject("end")));
            }
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

    }

    private static SourcePosition toSourcePosition(JSONObject obj) throws JSONException {
        return new SourcePosition(obj.getInt("line"), obj.getInt("column"));
    }

    public static Map<String, CoverageResult> combine(Map<String, CoverageResult> one, Map<String, CoverageResult> two) {
        Map<String, CoverageResult> result = new HashMap<>();

        for (String key : Util.union(one.keySet(), two.keySet())) {
            CoverageResult oneValue = one.get(key);
            CoverageResult twoValue = two.get(key);
            if (twoValue == null) {
                result.put(key, oneValue);
                continue;
            }
            if (oneValue == null) {
                result.put(key, twoValue);
                continue;
            }
            result.put(key, combine(oneValue, twoValue));
        }

        return result;
    }

    public static CoverageResult combine(CoverageResult... results) {
        return combine(Arrays.asList(results));
    }

    public static CoverageResult combine(List<CoverageResult> results) {
        results = results.stream().filter(Objects::nonNull).collect(Collectors.toList());
        if (results.isEmpty()) {
            return null;
        }
        CoverageResult combined = results.get(0);
        for (CoverageResult coverageResult : results.subList(1, results.size())) {
            combined = combine(combined, coverageResult);
        }

        return combined;
    }

    private static CoverageResult combine(CoverageResult one, CoverageResult two) {
        assert one.statements.keySet().equals(two.statements.keySet());
        assert one.branches.keySet().equals(two.branches.keySet());
        assert one.functions.keySet().equals(two.functions.keySet());

        Map<SourceLocation, Integer> statements = new HashMap<>();
        Map<SourceLocation, Collection<Integer>> branches = new HashMap<>();
        Map<SourceLocation, Integer> functions = new HashMap<>();

        for (SourceLocation location : one.statements.keySet()) {
            statements.put(location, one.statements.get(location) + two.statements.get(location));
        }
        for (SourceLocation location : one.functions.keySet()) {
            functions.put(location, one.functions.get(location) + two.functions.get(location));
        }
        for (SourceLocation location : one.branches.keySet()) {
            assert one.branches.get(location).size() == two.branches.get(location).size();
            List<Integer> subResult = Util.zip(one.branches.get(location), two.branches.get(location)).stream().map((pair) -> pair.getLeft() + pair.getRight()).collect(Collectors.toList());
            branches.put(location, subResult);
        }

        return new CoverageResult(statements, branches, functions);
    }
}
