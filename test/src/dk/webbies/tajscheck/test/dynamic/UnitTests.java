package dk.webbies.tajscheck.test.dynamic;

import dk.au.cs.casa.typescript.SpecReader;
import dk.au.cs.casa.typescript.types.BooleanLiteral;
import dk.au.cs.casa.typescript.types.InterfaceType;
import dk.au.cs.casa.typescript.types.NumberLiteral;
import dk.au.cs.casa.typescript.types.Type;
import dk.webbies.tajscheck.CoverageResult;
import dk.webbies.tajscheck.ExecutionRecording;
import dk.webbies.tajscheck.Main;
import dk.webbies.tajscheck.OutputParser;
import dk.webbies.tajscheck.benchmark.Benchmark;
import dk.webbies.tajscheck.benchmark.BenchmarkInfo;
import dk.webbies.tajscheck.benchmark.CheckOptions;
import dk.webbies.tajscheck.benchmark.TypeParameterIndexer;
import dk.webbies.tajscheck.parsespec.ParseDeclaration;
import dk.webbies.tajscheck.util.Util;
import org.apache.commons.lang3.StringUtils;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matcher;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static dk.webbies.tajscheck.OutputParser.*;
import static dk.webbies.tajscheck.benchmark.Benchmark.RUN_METHOD.*;
import static dk.webbies.tajscheck.test.Matchers.emptyMap;
import static dk.webbies.tajscheck.test.dynamic.UnitTests.ParseResultTester.ExpectType.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Created by erik1 on 23-11-2016.
 */
public class UnitTests {
    private SpecReader parseDeclaration(String folderName) {
        Benchmark bench = benchFromFolder(folderName);

        // Only testing that i can parse it, without getting exceptions
        return ParseDeclaration.getTypeSpecification(bench.environment, Collections.singletonList(bench.dTSFile));
    }

    public static Benchmark benchFromFolder(String folderName) {
        return benchFromFolder(folderName, options().build());
    }

    private static Benchmark benchFromFolder(String folderName, CheckOptions options) {
        return benchFromFolder(folderName, options, Benchmark.RUN_METHOD.NODE);
    }

    private static Benchmark benchFromFolder(String folderName, Benchmark.RUN_METHOD run_method) {
        return benchFromFolder(folderName, options().build(), run_method);
    }

    private static Benchmark benchFromFolder(String folderName, CheckOptions options, Benchmark.RUN_METHOD run_method) {
        return new Benchmark("unit-" + folderName, ParseDeclaration.Environment.ES5Core, "test/unit/" + folderName + "/implementation.js", "test/unit/" + folderName + "/declaration.d.ts", run_method, options);
    }

    private String runDriver(String folderName, String seed) throws Exception {
        Benchmark bench = benchFromFolder(folderName);

        return runDriver(bench, seed);
    }

    private String runDriver(String folderName, CheckOptions options, String seed) throws Exception {
        Benchmark bench = benchFromFolder(folderName, options);

        return runDriver(bench, seed);
    }

    private String runDriver(Benchmark bench, String seed) throws Exception {
        return runDriver(bench, seed, false);
    }

    private String runDriver(Benchmark bench, String seed, boolean skipConsistencyCheck) throws Exception {
        if (!skipConsistencyCheck) {
            sanityCheck(bench);
        }

        Main.writeFullDriver(bench, new ExecutionRecording(null, seed));

        return Main.runBenchmark(bench);
    }

    private static void sanityCheck(Benchmark bench) throws Exception {
        sanityCheck(bench, NODE);
    }

    private static void sanityCheck(Benchmark bench, Benchmark.RUN_METHOD runMethod) throws Exception {
        bench = bench.withRunMethod(BOOTSTRAP).withOptions(options -> options.setConstructAllTypes(true).setFailOnAny(false));

        // Performing a soundness check of the benchmark.
        Main.writeFullDriver(bench);
        String output = Main.runBenchmark(bench.withRunMethod(runMethod));
        OutputParser.RunResult result = OutputParser.parseDriverResult(output);

        for (OutputParser.TypeError typeError : result.typeErrors) {
            System.out.println(typeError);
        }

        assertThat(result.typeErrors, is(empty()));
    }

    private static ParseResultTester expect(RunResult result) {
        return new ParseResultTester(result.typeErrors);
    }

    static final class ParseResultTester {
        private List<TypeError> results;

        private ParseResultTester(List<TypeError> result) {
            this.results = result;
        }

        ParseResultTester forPath(String path) {
            return forPath(is(path));
        }

        ParseResultTester forPath(Matcher<String> path) {
            return forPath(Collections.singletonList(path));
        }

        ParseResultTester forPath(String... paths) {
            return forPath(Arrays.stream(paths).map(CoreMatchers::containsString).collect(Collectors.toList()));
        }

        ParseResultTester forPath(List<Matcher<String>> paths) {
            results = results.stream().filter(candidate -> paths.stream().anyMatch(matcher -> matcher.matches(candidate.path))).collect(Collectors.toList());

            assertThat("expected something on path: " + paths, results.size(),is(not(equalTo(0))));

            return this;
        }

        private ParseResultTester got(ExpectType type, String str) {
            return got(type, is(str));
        }

        private ParseResultTester got(ExpectType type, Matcher<String> matcher) {
            for (TypeError result : results) {
                if (type == ExpectType.JSON) {
                    assertThat(result.JSON, matcher);
                } else if (type == ExpectType.STRING) {
                    assertThat(result.toString, matcher);
                } else if (type == TYPEOF) {
                    assertThat(result.typeof, matcher);
                } else {
                    throw new RuntimeException(type.toString());
                }
            }
            return this;
        }

        ParseResultTester expected(String type) {
            return expected(is(type));
        }

        ParseResultTester expected(Matcher<String> type) {
            boolean matched = false;
            for (TypeError result : results) {
                matched |= type.matches(result.expected);
            }
            this.results = this.results.stream().filter(res -> type.matches(res.expected)).collect(Collectors.toList());

            if (!matched) {
                assertThat("Expected to find something of " + type.toString(), false);
            }

            return this;
        }

        ParseResultTester type(String type) {
            results = results.stream().filter(candidate -> type.equals(candidate.type)).collect(Collectors.toList());

            assertThat("expected something with type: " + type, results.size(),is(not(equalTo(0))));

            return this;
        }

        enum ExpectType {
            TYPEOF,
            STRING,
            JSON
        }
    }

    private RunResult run(String name) throws Exception {
        return run(benchFromFolder(name));
    }

    private RunResult run(String name, String seed) throws Exception {
        return run(benchFromFolder(name), seed);
    }

    private RunResult run(String name, CheckOptions options) throws Exception {
        return run(benchFromFolder(name, options));
    }

    private RunResult run(Benchmark benchmark) throws Exception {
        return run(benchmark, "foo");
    }

    private RunResult run(String name, CheckOptions options, String seed) throws Exception {
        return run(benchFromFolder(name, options), seed);
    }

    private RunResult run(Benchmark benchmark, String seed) throws Exception {
        return parseDriverResult(runDriver(benchmark, seed));
    }

    @Test
    public void testMissingProperty() throws Exception {
        RunResult result = OutputParser.parseDriverResult(runDriver("missingProperty", "mySeed"));

        expect(result)
                .forPath("module.foo.missing")
                .got(TYPEOF, is("undefined"));
    }

    @Test
    public void wrongSimpleType() throws Exception {
        RunResult result = run("wrongSimpleType", "aSeed");

        assertThat(result.typeErrors.size(), is(2));

        expect(result)
                .forPath("module.foo.bar")
                .expected("boolean")
                .got(TYPEOF, is("string"))
                .got(STRING, is("value"))
                .got(JSON, is("\"value\""));
    }

    @Test
    public void everyThingGoesRight() throws Exception {
        RunResult result = run("everythingIsRight", "aSeed");

        assertThat(result.typeErrors, is(empty()));
    }

    // TODO: Some test that a rest-recording can actually play. (One recording finds an error, another recording doesn't, same declaration file and implementation).

    @Test
    public void simpleFunctionArg() throws Exception {
        RunResult result = run("simpleFunctionArg", "someSeed");

        expect(result)
                .forPath("module.foo.[arg0].[arg0].<>.value")
                .expected("string")
                .got(TYPEOF, is("number"));
    }

    @Test
    public void complexUnion() throws Exception {
        RunResult result = run("complexUnion");

        expect(result)
                .forPath("module.foo().[union2]()")
                .expected("boolean")
                .got(TYPEOF, is("string"));

    }

    @Test
    public void optionalParameters() throws Exception {
        RunResult result = run("optionalParameters");

        expect(result)
                .forPath("module.foo(boolean)", "module.foo(boolean, string)")
                .expected("number")
                .got(TYPEOF, "undefined");
    }

    @Test
    public void simpleOverloads() throws Exception {
        RunResult result = run("simpleOverloads");

        assertThat(result.typeErrors, is(empty()));
    }

    @Test
    public void genericClass() throws Exception {
        RunResult result = run("genericClass", "mySeed");

        assertThat(result.typeErrors.size(), is(2));

        expect(result)
                .forPath("module.Container.create().<>.value", "module.Container.new().value")
                .expected(startsWith("a generic type marker"))
                .got(JSON, is("\"foo\""));
    }

    @Test
    public void generics() throws Exception {
        RunResult result = run("generics", "someSeed");

        expect(result)
                .forPath("module.foo().<>.value.foo")
                .expected("string")
                .got(JSON, is("123"));
    }

    @Test
    public void genericClass2() throws Exception {
        RunResult result = run("genericClass2", "mySeed");

        expect(result)
                .forPath("module.Index.new().store.<>.value")
                .expected("string")
                .got(JSON, "123");
    }

    @Test
    public void tuple() throws Exception {
        RunResult result = run("tuple", "seed");

        expect(result)
                .forPath("module.foo().<>.2")
                .expected("3.0")
                .got(TYPEOF, "string")
                .got(STRING, "3");
    }

    @Test
    public void tupleLength() throws Exception {
        RunResult result = run("tupleLength", "seed");

        expect(result)
                .forPath("module.foo()")
                .expected("tuple of 3 elements")
                .got(STRING, "1,2");
    }

    @Test
    public void never() throws Exception {
        RunResult result = run("never", "seed");

        expect(result)
                .forPath("module.foo()")
                .expected("never")
                .got(STRING, "1");

    }

    @Test
    public void thisTypes() throws Exception {
        RunResult result = run("thisTypes", "seed");

        expect(result)
                .forPath("module.Bar.new().bar")
                .expected("function")
                .got(TYPEOF, "undefined");
    }

    @Test
    public void symbols() throws Exception {
        RunResult result = run("symbol", "seed");

        assertThat(result.typeErrors.size(), is(equalTo(1)));

        expect(result)
                .forPath("module.bar()")
                .expected("symbol")
                .got(TYPEOF, "string");
    }

    @Test
    public void constructClass() throws Exception {
        RunResult result = run("constructClass", options().setConstructAllTypes(true).build(), "seed");

        expect(result)
                .forPath("module.foo(class)")
                .expected("\"foo\"")
                .got(STRING, "fooBar");
    }

    private static CheckOptions.Builder options() {
        return CheckOptions.builder().setMaxIterationsToRun(10000);
    }

    @Test
    public void arrayType() throws Exception {
        RunResult result = run(benchFromFolder("arrayType", options().setCheckDepthUseValue(2).build()));

        expect(result)
                .forPath("module.foo()")
                .expected("(arrayIndex: number)")
                .got(JSON, "[1,2,3,\"4\"]");
    }

    @Test
    public void arrayTypeCorrect() throws Exception {
        RunResult result = run("correctArrayType", "bar");

        assertThat(result.typeErrors, is(empty()));
        assertThat(result.errors, is(empty()));

    }

    @Test
    public void valueCantBeTrueAndFalse() throws Exception {
        RunResult result = run("valueCantBeTrueAndFalse");

        assertThat(result.errors.size(), is(greaterThan(0)));
    }

    @Test
    public void canHaveError() throws Exception {
        RunResult result = run("canHaveError", "foo");

        assertThat(result.errors.size(), is(greaterThan(0)));
    }

    @Test
    public void canHaveErrorBrowser() throws Exception {
        RunResult result = run(benchFromFolder("canHaveErrorBrowser", BROWSER), "foo");

        assertThat(result.errors.size(), is(greaterThan(0)));
    }

    @Test
    public void numberIndexer() throws Exception {
        RunResult result = run("numberIndexer");

        assertThat(result.typeErrors.size(), is(2));

        expect(result)
                .forPath("module.foo().[numberIndexer]")
                .expected("number")
                .got(TYPEOF, "string");

        expect(result)
                .forPath("module.foo()")
                .expected("(numberIndexer: number)")
                .got(JSON, "{\"1\":1,\"3\":4,\"7\":1,\"10\":\"blah\"}");
    }

    @Test
    public void deepNumberIndexer() throws Exception {
        CheckOptions options = options()
                .setCheckDepthUseValue(1)
                .setCheckDepthReport(1)
                .build();

        RunResult result = run("numberIndexer", options);

        assertThat(result.typeErrors.size(), is(1));

        expect(result)
                .forPath("module.foo()")
                .expected("(numberIndexer: number)")
                .got(JSON, "{\"1\":1,\"3\":4,\"7\":1,\"10\":\"blah\"}");
    }

    @Test
    public void stringIndexer() throws Exception {
        RunResult result = run("stringIndexer", options().setCheckDepthReport(0).build());

        assertThat(result.typeErrors.size(), is(1));

        expect(result)
                .forPath("module.foo().[stringIndexer]")
                .expected("number")
                .got(TYPEOF, "string");
    }

    @Test
    public void createNumberIndexer() throws Exception {
        RunResult result = run("createNumberIndexer", "bar");

        expect(result)
                .forPath("module.foo().[numberIndexer]")
                .expected("number")
                .got(TYPEOF, "string");
    }

    @Test
    public void createStringIndexer() throws Exception {
        RunResult result = run("createStringIndexer", "bar");

        expect(result)
                .forPath("module.foo().[stringIndexer]")
                .expected("number")
                .got(TYPEOF, "string");
    }

    @Test
    public void simpleClass() throws Exception {
        run("simpleClass"); // Just pass the sanity check.
    }

    @Test
    public void keyOf() throws Exception {
        RunResult result = run("keyOf");

        assertThat(result.typeErrors.size(), is(1));

        expect(result)
                .forPath("module.foo()")
                .expected("(\"name\" or \"age\" or \"location\")")
                .got(STRING, "notAProp");
    }

    @Test
    public void indexedAccess() throws Exception {
        RunResult result = run("indexedAccess");

        assertThat(result.typeErrors.size(), is(1));

        expect(result)
                .forPath("module.foo()")
                .expected("(string or number)")
                .got(TYPEOF, "boolean");
    }

    @Test
    public void genericIndexedAccess() throws Exception {
        SpecReader spec = parseDeclaration("genericIndexedAccess");
        assertThat(spec, is(notNullValue()));
    }

    @Test
    public void mappedTypes() throws Exception {
        SpecReader spec = parseDeclaration("mappedTypes");
        assertThat(spec, is(notNullValue()));
    }

    @Test
    public void differentSizeOverloads() throws Exception {
        RunResult result = run("differentSizeOverloads");

        assertThat(result.typeErrors, is(empty()));
        assertThat(result.errors, is(empty()));
    }

    @Test
    public void complexOverloads() throws Exception {
        RunResult result = run("complexOverloads");

        assertThat(result.typeErrors, is(empty()));
        assertThat(result.errors, is(empty()));
    }

    @Test
    public void numberLiteral() throws Exception {
        SpecReader spec = BenchmarkInfo.create(benchFromFolder("numberLiteral")).getSpec();

        NumberLiteral number = (NumberLiteral) spec.getGlobal().getDeclaredProperties().get("module");

        assertThat(number.getValue(), is(equalTo(1337d)));
    }


    @Test
    public void booleanLiteral() throws Exception {
        SpecReader spec = BenchmarkInfo.create(benchFromFolder("booleanLiteral")).getSpec();

        InterfaceType module = (InterfaceType) spec.getGlobal().getDeclaredProperties().get("module");

        BooleanLiteral t = (BooleanLiteral) module.getDeclaredProperties().get("t");
        BooleanLiteral f = (BooleanLiteral) module.getDeclaredProperties().get("f");

        assertThat(t.getValue(), is(true));
        assertThat(f.getValue(), is(false));
    }

    @Test
    public void overloadsWithOptionalParameters() throws Exception {
        RunResult result = run("overloadsWithOptionalParameters");

        assertThat(result.typeErrors, is(empty()));
        assertThat(result.errors, is(empty()));
    }

    @Test
    public void deepUnions() throws Exception {
        CheckOptions options = options()
                .setCheckDepthForUnions(2)
                .build();

        RunResult result = run("deepUnion", options);

        expect(result)
                .forPath("module.foo()")
                .expected("(((function or object) and not null and field[foo]:(((function or object) and not null and field[bar]:(boolean)))) or ((function or object) and not null and field[foo]:(((function or object) and not null and field[bar]:(string)))))")
                .got(JSON, "{\"foo\":{\"bar\":123}}");
    }

    @Test
    public void typeInArray() throws Exception {
        RunResult result = run("typeInArray");

        expect(result)
                .forPath("module.foo().<>.[numberIndexer].bar.baz")
                .expected("true")
                .got(STRING, "false");
    }

    @Test
    public void genRestArgs() throws Exception {
        RunResult result = run("genRestArgs");

        expect(result)
                .forPath("Foo.[restArgs]")
                .expected("valid rest-args")
                .got(STRING, "string,1,4,7,false");
    }

    @Test
    public void genRestArgsWithOverloads() throws Exception {
        RunResult result = run("genRestArgsWithOverloads");

        assertThat(result.typeErrors.size(), is(1));

        expect(result)
                .forPath("Foo")
                .expected("A valid overload")
                .got(STRING, "string,1,4,7,false");
    }

    @Test
    public void testRestArgs() throws Exception {
        RunResult result = run("testRestArgs");

        assertThat(result.typeErrors, is(empty()));

    }

    @Test
    public void propertyWithUnderscore() throws Exception {
        RunResult result = run("propertyWithUnderscore");

        assertThat(result.typeErrors, is(empty()));
    }

    @Test
    public void testIfDriverIsTooBig() throws Exception {
        String driver = Main.generateFullDriver(benchFromFolder("unnecessaryBigDriver")).getRight();

        assertThat(driver, not(containsString("\"module.b2World.new().RayCastAll().<>.[numberIndexer].GetDensity()\"")));

    }

    @Test
    public void genericClass3() throws Exception {
        RunResult result = run("genericClass3");

        expect(result)
                .forPath("module.createNumberContainer().<>.value")
                .expected("number")
                .got(STRING, "a string");
    }

    @Test
    public void genericClass4() throws Exception {
        RunResult result = run("genericClass4");

        assertThat(result.errors, is(empty()));

    }

    @Test
    public void complexSanityCheck() throws Exception {
        sanityCheck(benchFromFolder("complexSanityCheck"));

    }

    @Test
    public void complexSanityCheck2() throws Exception {
        sanityCheck(benchFromFolder("complexSanityCheck2"));
    }

    @Test(expected = AssertionError.class)
    public void complexSanityCheck3() throws Exception {
        // The TypeScript type system is unsound, this is a test of that.
        sanityCheck(benchFromFolder("complexSanityCheck3"));
    }

    @Test(expected = AssertionError.class)
    public void complexSanityCheck9() throws Exception {
        // The TypeScript type system is unsound, this is a test of that.
        sanityCheck(benchFromFolder("complexSanityCheck9"));
    }

    @Test
    public void complexSanityCheck10() throws Exception {
        sanityCheck(benchFromFolder("complexSanityCheck10"));
    }

    @Test
    public void doesNotHaveUnboundGenerics() throws Exception {
        Benchmark bench = benchFromFolder("doesNotHaveUnboundGenerics").withRunMethod(BOOTSTRAP).withOptions(options -> options.setConstructAllTypes(true));
        Main.writeFullDriver(bench);
        String driver = Main.generateFullDriver(bench).getRight();

        assertThat(driver, not(containsString(TypeParameterIndexer.IS_UNSTRAINED_GENERIC_MARKER)));
    }

    @Test
    public void complexSanityCheck11() throws Exception {
        sanityCheck(benchFromFolder("complexSanityCheck11"));
    }

    @Test
    public void complexSanityCheck12() throws Exception {
        sanityCheck(benchFromFolder("complexSanityCheck12"), BROWSER);
    }

    @Test
    public void complexSanityCheck13() throws Exception {
        sanityCheck(benchFromFolder("complexSanityCheck13"), BROWSER);
    }

    @Test
    public void complexSanityCheck14() throws Exception {
        sanityCheck(benchFromFolder("complexSanityCheck14"), NODE);
    }

    @Test
    public void complexSanityCheck15() throws Exception {
        sanityCheck(benchFromFolder("complexSanityCheck15", options().setDisableSizeOptimization(true).build()), NODE);
    }

    @Test
    public void complexSanityCheck16() throws Exception {
        sanityCheck(benchFromFolder("complexSanityCheck16", options().setDisableGenerics(true).setSplitUnions(false).build()), NODE);
    }

    @Test
    public void complexSanityCheck17() throws Exception {
        sanityCheck(benchFromFolder("complexSanityCheck17"), BROWSER);
    }

    @Test
    public void complexSanityCheck18() throws Exception {
        sanityCheck(benchFromFolder("complexSanityCheck18"), BROWSER);
    }

    @Test
    public void complexSanityCheck19() throws Exception {
        sanityCheck(benchFromFolder("complexSanityCheck19"), NODE);
    }

    @Test
    public void complexSanityCheck20() throws Exception {
        sanityCheck(benchFromFolder("complexSanityCheck20").withOptions(options -> options.setDisableGenerics(true)), NODE);
    }

    @Test
    public void complexSanityCheck22() throws Exception {
        sanityCheck(benchFromFolder("complexSanityCheck22"), BROWSER);
    }

    @Test(expected = AssertionError.class) // TODO: make this not fail?
    public void complexSanityCheck23() throws Exception {
        sanityCheck(benchFromFolder("complexSanityCheck23"), NODE);
    }

    @Test
    public void nodeList() throws Exception {
        sanityCheck(benchFromFolder("nodeList", options().setCheckDepthReport(1).setCheckDepthUseValue(1).build()), BROWSER);
    }

    @Test
    public void extendingGenericClass() throws Exception {
        sanityCheck(benchFromFolder("extendingGenericClass"), NODE);
    }

    @Test
    public void extendsError() throws Exception {
        sanityCheck(benchFromFolder("extendsError"));
    }

    @Test
    public void extendsEvent() throws Exception {
        sanityCheck(benchFromFolder("extendsEvent"), BROWSER);
    }

    @Test
    public void extendsEvent2() throws Exception {
        sanityCheck(benchFromFolder("extendsEvent2"), BROWSER);
    }

    @Test
    public void extendsEvent3() throws Exception {
        sanityCheck(benchFromFolder("extendsEvent3"), BROWSER);
    }

    @Test(expected = AssertionError.class)
    public void overrideNumberOfArguments() throws Exception {
        // Actually just bivariance on the function arguments.
        // When a function (with e.g. 2 parameters) is overridden, with a function that takes 1 parameter.
        // Then the second parameter kind-of gets the bottom type.
        // TypeScript allows this, but it is unsound (just like complexSanityCheck9
        sanityCheck(benchFromFolder("overrideNumberOfArguments", options().setConstructAllTypes(false).build()), BROWSER);
    }

    @Test
    public void classesAndNamespaces() throws Exception {
        RunResult result = run("classesAndNamespaces");

        assertThat(result.typeErrors, is(empty()));
        assertThat(result.errors, is(empty()));
    }

    @Test
    public void complexSanityCheck4() throws Exception {
        sanityCheck(benchFromFolder("complexSanityCheck4"));
    }

    @Test
    public void complexSanityCheck5() throws Exception {
        sanityCheck(benchFromFolder("complexSanityCheck5"));
    }

    @Test
    public void complexSanityCheck6() throws Exception {
        sanityCheck(benchFromFolder("complexSanityCheck6"));
    }

    @Test
    public void thisTypes2() throws Exception {
        // This is just a test that it is able to generate an application, without crashing.
        String program = Main.generateFullDriver(benchFromFolder("thisTypes2")).getRight();

        assertThat(program, is(not(equalTo(""))));
    }

    @Test
    public void complexThisTypes() throws Exception {
        RunResult result = run("complexThisTypes");

        assertThat(result.typeErrors.size(), is(greaterThan(0)));
    }

    @Test
    public void thisTypesInInterfaces() throws Exception {
        RunResult result = run("thisTypesInInterfaces", options().setCheckDepthReport(0).setCheckDepthUseValue(0).build());

        expect(result)
                .forPath("module.baz().bar")
                .expected("(function or object)")
                .got(TYPEOF, "undefined");
    }

    @Test
    public void complexThisTypes2() throws Exception {
        // Actually just a test that i don't get a null-pointer while constructing the sanity-driver.
        sanityCheck(benchFromFolder("complexThisTypes2"));
    }

    @Test
    public void genericsAreOptimized() throws Exception {
        CheckOptions options = options().setDisableSizeOptimization(false).setCheckDepthReport(0).build();
        RunResult optimized = run(benchFromFolder("genericsAreOptimized", options), "seed");

        assertThat(optimized.typeErrors.size(), is(1));


        options = options().setDisableSizeOptimization(true).setCheckDepthReport(0).build();
        RunResult unOptimzed = run(benchFromFolder("genericsAreOptimized", options), "seed");

        assertThat(unOptimzed.typeErrors.size(), is(2));
    }

    @Test
    public void genericsWithNoOptimization2() throws Exception {
        CheckOptions options = options().setDisableSizeOptimization(true).build();
        RunResult result = run(benchFromFolder("genericsWithNoOptimization2", options), "seed");

        assertThat(result.typeErrors.size(), is(greaterThan(0)));
    }


    @Test
    public void genericsWithNoOptimization() throws Exception {
        CheckOptions options = options().setDisableSizeOptimization(true).build();
        RunResult result = run(benchFromFolder("genericsWithNoOptimization", options), "seed");

        assertThat(result.typeErrors.size(), is(2));
    }

    @Test
    public void thisTypesInInterfaces2() throws Exception {
        RunResult result = run("thisTypesInInterfaces2");

        assertThat(result.typeErrors.size(), is(greaterThan(0)));
    }

    @Test
    public void thisTypesInInterfaces3() throws Exception {
        Main.writeFullDriver(benchFromFolder("thisTypesInInterfaces3"));
    }

    @Test
    public void thisTypesAreOptimized() throws Exception {
        RunResult result = run("thisTypesAreOptimized", options().setCheckDepthUseValue(0).setCheckDepthReport(0).build());

        assertThat(result.typeErrors.size(), is(1));
    }

    @Test
    public void extendsArray() throws Exception {
        RunResult result = run("extendsArray");

        assertThat(result.typeErrors, is(empty()));
    }

    @Test
    public void extendsArray2() throws Exception {
        RunResult result = run("extendsArray2");

        assertThat(result.typeErrors, is(empty()));
    }

    @Test
    public void staticFieldsInheritedInClass() throws Exception {
        RunResult result = run("staticFieldsInheritedInClass");

        assertThat(result.typeErrors, is(empty())); // It actually contains an error, according to the TypeScript language, it is just an error we choose not to check for.
    }

    @Test
    public void intersectionTypes() throws Exception {
        RunResult result = run("intersectionTypes");

        expect(result)
                .forPath("module.foo(intersection)")
                .expected("false")
                .got(STRING, "true");
    }

    @Test
    public void complexSanityCheck7() throws Exception {
        sanityCheck(benchFromFolder("complexSanityCheck7"));
    }

    @Test
    public void complexSanityCheck8() throws Exception {
        sanityCheck(benchFromFolder("complexSanityCheck8"));
    }

    @Test
    public void genericsBustStack() throws Exception {
        Main.generateFullDriver(benchFromFolder("genericsBustStack"));
    }

    @Test
    public void genericsBustStack2() throws Exception {
        Main.generateFullDriver(benchFromFolder("genericsBustStack2"));
    }

    @Test
    public void genericsBustStack3() throws Exception {
        Main.generateFullDriver(benchFromFolder("genericsBustStack3").withRunMethod(BOOTSTRAP));
    }

    @Test
    public void genericsBustStack4() throws Exception {
        Main.generateFullDriver(benchFromFolder("genericsBustStack4").withRunMethod(BOOTSTRAP));
    }

    @Test
    public void genericsBustStackRuntime() throws Exception {
        RunResult result = run("genericsBustStackRuntime");

        assertThat(result.typeErrors.size(), is(greaterThan(0)));
    }

    @Test
    public void intersectionWithFunction() throws Exception {
        RunResult result = run(benchFromFolder("intersectionWithFunction", options().setConstructAllTypes(true).build()).withRunMethod(BOOTSTRAP));

        assertThat(result.typeErrors, is(empty()));
        assertThat(result.errors, everyItem(is(equalTo("RuntimeError: Cannot construct this IntersectionType")))); // <- this happens, it is ok, i cannot at runtime construct a type which is the intersection of two types.
    }

    @Test
    public void extendsArray3() throws Exception {
        RunResult result = run("extendsArray3");

        assertThat(result.typeErrors.size(), is(equalTo(3)));

        expect(result)
                .forPath("module.bar().<>.[numberIndexer].<>.[numberIndexer]")
                .expected("string")
                .got(TYPEOF, "number");
    }

    @Test
    public void extendsArray4() throws Exception {
        RunResult result = run("extendsArray4");

        assertThat(result.typeErrors, is(empty()));
    }

    @Test
    public void unboundGenericsAreNotDuplicated() throws Exception {
        RunResult result = run("unboundGenericsAreNotDuplicated", options().setCheckDepthReport(0).build());

        assertThat(result.typeErrors.size(), is(lessThanOrEqualTo(1)));

    }

    @Test
    public void complexGenerics() throws Exception {
        Main.generateFullDriver(benchFromFolder("complexGenerics")); // Just a test that no null-pointer.
    }

    @Test
    public void wrongSignaturePropagates() throws Exception {
        RunResult result = run("wrongSignaturePropagates");

        expect(result)
                .forPath("module.foo.[arg0].[arg0]")
                .expected("boolean")
                .got(TYPEOF, "undefined");

        assertThat(result.typeErrors.size(), is(1));
    }

    @Test
    public void thisTypesAreOptimized2() throws Exception {
        RunResult result = run("thisTypesAreOptimized2", options().setConstructAllTypes(false).build());

        assertThat(result.typeErrors.size(), is(1));
    }

    @Test
    public void genericsAreOptimized2() throws Exception {
        String driver = Main.generateFullDriver(benchFromFolder("genericsAreOptimized2")).getRight();

        assertThat(driver, not(containsString("module.CatmullRomCurve3.<>.getPoint().setFromSpherical().multiplyVector3Array(any)")));
        assertThat(driver, not(containsString("module.CatmullRomCurve3.<>.getPoint().setFromSpherical().multiplyVector3Array()")));

    }

    @Test
    public void exponentialComplexity() throws Exception {
        Main.writeFullDriver(benchFromFolder("exponentialComplexity"));
    }

    @Test
    public void veryComplexThisType() throws Exception {
        Main.generateFullDriver(benchFromFolder("veryComplexThisType"));
    }

    @Test
    public void genericsAreNotTooOptimized() throws Exception {
        Benchmark bench = benchFromFolder("genericsAreNotTooOptimized", options().setCombineAllUnboundGenerics(true).build());
        String driver = Main.generateFullDriver(bench).getRight();
        Main.writeFullDriver(bench);

        assertThat(driver, not(containsString(TypeParameterIndexer.IS_UNSTRAINED_GENERIC_MARKER)));
    }

    @Test
    public void classAndClassInstances() throws Exception {
        RunResult result = run("classAndClassInstances");

        assertThat(result.typeErrors, is(empty()));
    }

    @Test
    public void typeofParsing() throws Exception {
        String driver = Main.generateFullDriver(benchFromFolder("typeofParsing")).getRight();

        assertThat(driver, not(containsString("module.getNewLibraryCopy.prototype")));

    }

    @Test
    public void testClass() throws Exception {
        RunResult result = run("testClass");

        assertThat(result.typeErrors.size(), is(1));

    }

    @Test
    public void interfacesAndObjectsParsing() throws Exception {
        String driver = Main.generateFullDriver(benchFromFolder("interfacesAndObjectsParsing")).getRight();

        Main.writeFullDriver(benchFromFolder("interfacesAndObjectsParsing"));

        assertThat(driver, not(containsString("// path: module.Observable.selectMany")));

    }

    @Test
    public void undefinedOnObject() throws Exception {
        RunResult result = run("undefinedOnObject");

        assertThat(result.typeErrors, is(empty()));
    }

    @Test
    public void namespacesAndClassWithNestedClass() throws Exception {
        RunResult result = run("namespacesAndClassWithNestedClass");

        assertThat(result.typeErrors, is(empty()));

    }

    @Test
    public void complexGenerics2() throws Exception {
        RunResult result = run("complexGenerics2");

        assertThat(result.typeErrors, is(empty()));

    }

    @Test
    public void canFindErrorsEvenWhenTimeout() throws Exception {
        RunResult result = run(benchFromFolder("canFindErrorsEvenWhenTimeout", options().setMaxTime(5 * 1000).build()));

        assertThat(result.typeErrors.size(), is(1));

    }

    @Test
    public void canFindErrorsEvenWhenTimeoutChrome() throws Exception {
        RunResult result = run(benchFromFolder("canFindErrorsEvenWhenTimeoutChrome", options().setMaxTime(5 * 1000).build()).withRunMethod(BROWSER));

        assertThat(result.typeErrors.size(), is(1));

    }

    @Test // This should be stopped by the amount of iterations.
    public void findSimpleErrorChrome() throws Exception {
        long startTime = System.currentTimeMillis();

        RunResult result = run(benchFromFolder("findSimpleErrorChrome", options().setMaxTime(60 * 1000).build()).withRunMethod(BROWSER));

        assertThat(result.typeErrors.size(), is(1));

        long time = System.currentTimeMillis() - startTime;

        System.out.println("Time taken: " + (time / 1000.0) + "s");

        assertThat(time, is(lessThan((long)60 * 1000)));
    }

    @Test // This should be stopped by the amount of iterations.
    public void findSimpleErrorChromeWithErrors() throws Exception {
        long startTime = System.currentTimeMillis();

        RunResult result = run(benchFromFolder("findSimpleErrorChromeWithErrors", options().setMaxTime(60 * 1000).build()).withRunMethod(BROWSER));

        assertThat(result.typeErrors.size(), is(1));

        long time = System.currentTimeMillis() - startTime;

        System.out.println("Time taken: " + (time / 1000.0) + "s");

        assertThat(time, is(lessThan((long)60 * 1000)));
    }

    @Test
    public void classProperties() throws Exception {
        RunResult result = run("classProperties", options().setConstructAllTypes(true).setCheckDepthReport(0).build());

        assertThat(result.typeErrors.size(), is(3)); // The tests are in the .js file.

        expect(result)
                .forPath("module.isCalled(class)")
                .expected("true")
                .got(JSON, "false");

        expect(result)
                .forPath("module.Class")
                .expected("A valid overload")
                .got(JSON, "[true]");

        expect(result)
                .forPath("module.Class")
                .expected("a constructor")
                .got(STRING, "undefined");
    }

    @Test
    public void booleans() throws Exception {
        RunResult result = run("booleans");

        assertThat(result.typeErrors, is(empty()));
    }

    @Test
    public void tupleSizes() throws Exception {
        RunResult result = run("tupleSizes");

        assertThat(result.typeErrors, is(empty()));
    }

    @Test
    public void complexThisTypes3() throws Exception {
        RunResult result = run("complexThisTypes3", options().setCheckDepthUseValue(2).build());

        assertThat(result.typeErrors, is(empty()));

    }

    @Test
    public void staticFields() throws Exception {
        RunResult result = run("staticFields");

        assertThat(result.typeErrors.size(), is(2));

        expect(result)
                .forPath("module.Foo.foo")
                .expected("true")
                .got(JSON, "false");

    }


    @Test
    public void voidReturnCanBeAnything() throws Exception {
        RunResult result = run("voidReturnCanBeAnything");

        assertThat(result.typeErrors.size(), is(1));

        expect(result)
                .forPath("module.bar()")
                .got(STRING, "any");
    }

    @Test
    public void canConstructUnderscoreWithUnconstrainedGenerics() throws Exception {
        Main.generateFullDriver(RunBenchmarks.benchmarks.get("Underscore.js").withOptions(options -> options.setCombineAllUnboundGenerics(false)));
    }

    @Test
    public void genericClassFeedback() throws Exception {
        RunResult result = run("genericClassFeedback");

        assertThat(result.typeErrors.size(), is(1));

        expect(result)
                .forPath("module.returnsFalse(obj)")
                .expected("true")
                .got(STRING, "false");
    }

    @Test
    public void genericClassFeedbackWithConstraint() throws Exception {
        RunResult result = run("genericClassFeedbackWithConstraint");

        assertThat(result.typeErrors.size(), is(1));

        expect(result)
                .forPath("module.returnsFalse(obj)")
                .expected("true")
                .got(STRING, "false");
    }

    @Test
    public void genericInterfaceFeedback() throws Exception {
        RunResult result = run("genericInterfaceFeedback");

        assertThat(result.typeErrors.size(), is(1));
    }

    @Test
    public void canWritePrimitives() throws Exception {
        RunResult resultNoWrite = run("canWritePrimitives", options().setWritePrimitives(false).build());

        assertThat(resultNoWrite.typeErrors, is(empty()));

        RunResult resultWithWrite = run("canWritePrimitives", options().setWritePrimitives(true).build());

        assertThat(resultWithWrite.typeErrors.size(), is(1));

        expect(resultWithWrite)
                .forPath("module.test(obj)")
                .expected("true")
                .got(STRING, "false");
    }

    @Test
    public void canWriteComplex() throws Exception {
        RunResult resultNoWrite = run("canWriteComplex");

        assertThat(resultNoWrite.typeErrors, is(empty()));

        boolean hadAnError = false;
        for (int i = 0; i < 20; i++) {
            System.out.println("Trying with seed: " + i);
            RunResult resultWithWrite = run("canWriteComplex", options().setWriteAll(true).build(), i + "");

            if (resultWithWrite.typeErrors.size() > 0) {
                hadAnError = true;
                break;
            }
        }
        assertThat(hadAnError, is(true));
    }

    @Test
    public void noIterations() throws Exception {
        RunResult result = run("noIterations", options().setMaxIterationsToRun(0).build());

        assertThat(result.typeErrors, is(empty()));
    }

    @Test
    public void browserCoverage() throws Exception {
        Benchmark bench = benchFromFolder("browserCoverage").withRunMethod(BROWSER);

        RunResult result = run(bench);

        assertThat(result.typeErrors, is(empty()));

        Map<String, CoverageResult> coverage = Main.genCoverage(bench);

        assertThat(coverage, is(not(emptyMap())));

        for(String k : coverage.keySet()) {
            if (k.endsWith("implementation.js"))
                assertThat(coverage.get(k).statementCoverage(), is(greaterThan(0.5)));
        }
    }
    @Test
    public void browserCoverageTimeout() throws Exception {
        Benchmark bench = benchFromFolder("browserCoverageTimeout", options().setMaxTime(10 * 1000).setMaxIterationsToRun(-1).build()).withRunMethod(BROWSER);

        RunResult result = run(bench);

        assertThat(result.typeErrors, is(empty()));

        Map<String, CoverageResult> coverage = Main.genCoverage(bench);

        assertThat(coverage, is(not(emptyMap())));

        for(String k : coverage.keySet()) {
            if (k.endsWith("implementation.js"))
                assertThat(coverage.get(k).statementCoverage(), is(greaterThan(0.5)));
        }
    }

    @Test
    public void nodeCoverage() throws Exception {
        Benchmark bench = benchFromFolder("nodeCoverage").withOptions(options -> options.setMaxIterationsToRun(1));

        RunResult result = run(bench);

        assertThat(result.typeErrors, is(empty()));

        Map<String, CoverageResult> coverage = Main.genCoverage(bench);

        assertThat(coverage, is(not(emptyMap())));

        for(String k : coverage.keySet()) {
            if (k.endsWith("test.js")) assertThat(coverage.get(k).statementCoverage(), is(greaterThan(0.2)));
        }
    }

    @Test
    public void nodeCoverageTimeout() throws Exception {
        Benchmark bench = benchFromFolder("nodeCoverageTimeout").withOptions(options -> options.setMaxIterationsToRun(-1));

        Map<String, CoverageResult> coverage = Main.genCoverage(bench);

        assertThat(coverage, is(not(emptyMap())));

        for(String k : coverage.keySet()) {
            if (k.endsWith("test.js")) assertThat(coverage.get(k).statementCoverage(), is(greaterThan(0.2)));
        }
    }

    @Test
    public void notDuplicatedAssertTypeFunctions() throws Exception {
        CheckOptions options = options().setUseAssertTypeFunctions(true).build();
        Main.writeFullDriver(benchFromFolder("notDuplicatedAssertTypeFunctions", options));

        String driver = Main.generateFullDriver(benchFromFolder("notDuplicatedAssertTypeFunctions", options)).getRight();

        int matches = StringUtils.countMatches(driver,
                "if (!(assert(true === exp._isUnboundGeneric, path, \"a generic type marker (._isUnboundGeneric)\", exp, i, testType))) {");

        assertThat(matches, is(1));
    }

    @Test
    public void combinedShallowDeepChecking() throws Exception {
        RunResult result = run("combinedShallowDeepChecking");

        expect(result)
                .forPath("module.K")
                .type("load module")
                .expected("function")
                .got(TYPEOF, "undefined");

        expect(result)
                .forPath("module.K")
                .type("property access")
                .expected("function")
                .got(TYPEOF, "undefined");

        expect(result)
                .forPath("module.foo()")
                .expected("true")
                .got(STRING, "false");
    }

    @Test
    public void infiniteGenerics() throws Exception {
        RunResult result = run("infiniteGenerics");

        assertThat(result.typeErrors.size(), is(1));

        expect(result)
                .forPath("module.test(obj)")
                .expected("never")
                .got(STRING, "was any, good!");
    }

    @Test
    public void firstMatchPolicy() throws Exception {
        RunResult result = run("firstMatchPolicy");

        assertThat(result.typeErrors, is(empty()));
    }

    /*
     * Examples used in the paper are below this:
     */

    @Test
    public void genericsSplit() throws Exception {
        // Combine generics, find the error.
        RunResult result = run("genericsSplit", options().setCombineAllUnboundGenerics(true).build());
        assertThat(result.typeErrors.size(), is(1));

        // Not combining, error remain unfound
        result = run("genericsSplit", options().setCombineAllUnboundGenerics(false).build());

        assertThat(result.typeErrors, is(empty()));

    }

    @Test
    public void firstOrderFunctions() throws Exception {
        RunResult result = run("firstOrderFunctions");

        expect(result)
                .forPath("module.time.[arg1].[arg0]")
                .expected("number")
                .got(TYPEOF, "string");

    }

    @Test
    public void basicExample1() throws Exception {
        RunResult result = run("basicExample1", options().setCheckDepthReport(0).build());

        assertThat(result.typeErrors.size(), is(1));

        expect(result)
                .forPath("module.bar")
                .got(STRING, "123");
    }

    @Test
    public void asyncBasicExample() throws Exception {
        Main.writeFullDriver(benchFromFolder("asyncBasicExample"));
    }

    @Test
    public void higherOrderFunctions() throws Exception {
        RunResult result = run("higherOrderFunctions");

        expect(result)
                .forPath("module.twice.[arg1].[arg0]")
                .expected("string")
                .got(TYPEOF, "number");

    }

    @Test
    public void genericExtendMethod() throws Exception {
        RunResult result = run("genericExtendMethod", options().setCombineAllUnboundGenerics(false).build());

        assertThat(result.typeErrors, is(empty()));

    }

    @Test
    public void basicMemomizeExample() throws Exception {
        RunResult result = run(benchFromFolder("basicMemomizeExample"));

        assertThat(result.typeErrors, is(empty()));
    }

    @Test
    public void unsoundSiblings() throws Exception {
        CheckOptions options = options().setCheckDepthUseValue(2).build();
        RunResult result = parseDriverResult(runDriver(benchFromFolder("unsoundSiblings", options), "foo", true));

        assertThat(result.typeErrors.size(), is(greaterThanOrEqualTo(1)));
    }

    @Test
    @Ignore // Fails because Symbol. After this, construct an example where something beneath a symbol prop access fails.
    public void generators() throws Exception {
        String folderName = "generators";
        Benchmark bench = new Benchmark("unit-generators", ParseDeclaration.Environment.ES6DOM, "test/unit/" + folderName + "/implementation.js", "test/unit/" + folderName + "/declaration.d.ts", Benchmark.RUN_METHOD.NODE, options().setMaxIterationsToRun(1000).build());

        RunResult result = run(bench);

        assertThat(result.typeErrors, is(empty()));
    }

    @Test
    @Ignore // Fails because async iterators aren't included as a library. I don't need to fix this.
    public void asyncGenerator() throws Exception {
        try {
            Util.isDeltaDebugging = true; // Testing with this on, since it forces the TypeScript compiler to return with no errors.
            Main.writeFullDriver(benchFromFolder("asyncGenerator")); // Just testing the parsing.
        } finally {
            Util.isDeltaDebugging = false;
        }
    }

    @Test
    public void ambient() throws Exception {
        RunResult result = run("ambient");

        expect(result)
                .forPath("\"foo\".add(1, 2)")
                .expected("3.0")
                .got(STRING, "4");
    }

    @Test
    public void browserMultipleProperties() throws Exception {
        RunResult result = run(benchFromFolder("browserMultipleProperties", BROWSER));

        assertThat(result.typeErrors.size(), is(2));

        expect(result)
                .forPath("foo()")
                .expected("true")
                .got(STRING, "false");

        expect(result)
                .forPath("bar()")
                .expected("true")
                .got(STRING, "false");

    }

    @Test
    public void ambient2() throws Exception {
        RunResult result = run("ambient2", options().setCheckDepthReport(0).build());

        assertThat(result.typeErrors.size(), is(equalTo(1)));
    }

    @Test
    public void ambient3() throws Exception {
        RunResult result = run("ambient3", options().setCheckDepthReport(0).build());

        assertThat(result.typeErrors.size(), is(equalTo(2)));
    }

    @Test
    public void undefinedReturnCanFail() throws Exception {
        RunResult result = run("undefinedReturnCanFail");

        assertThat(result.typeErrors.size(), is(1));

        expect(result)
                .forPath("module.foo()")
                .expected("undefined")
                .got(TYPEOF, "string");
    }

    @Test
    public void smokeUnknownFieldsAccess() throws Exception {
        Benchmark bench = benchFromFolder("unknownFieldAccess", options().setMonitorUnkownPropertyAccesses(true).build());
        String driver = Main.generateFullDriver(bench).getRight();
        Main.writeFullDriver(bench);

        String res = Main.runBenchmark(bench);
        List<String> lines = Arrays.asList(res.split(Pattern.quote("\n")));
        if (lines.size() > 100) {
            lines = lines.subList(0, 100);
        }
        System.out.println(String.join("\n", lines));

    }

    @Test
    public void monitorUndeclaredAccess1() throws Exception {
        // Tests that class instances are handled correctly.
        RunResult result = run("monitorUndeclaredAccess1", options().setMonitorUnkownPropertyAccesses(true).setConstructClassInstances(true).build());

        assertThat(result.errors, not(hasItem(containsString("GetLocalVector"))));

    }

    @Test
    public void optionalDoesNotMeanUndefined() throws Exception {
        RunResult result = run("optionalDoesNotMeanUndefined", "foo");

        assertThat(result.typeErrors, is(empty()));
    }

    @Test
    public void optionalDoesNotMeanUndefinedWithRestArg() throws Exception {
        RunResult result = run("optionalDoesNotMeanUndefinedWithRestArg", "foo");

        assertThat(result.typeErrors, is(empty()));
    }

    @Test
    public void optionalParamsSmokeTest() throws Exception {
        Main.writeFullDriver(benchFromFolder("optionalParamsSmokeTest").withRunMethod(BOOTSTRAP));
    }

    @Test
    public void optionalParamsSmokeTest2() throws Exception {
        Main.writeFullDriver(benchFromFolder("optionalParamsSmokeTest2").withRunMethod(BOOTSTRAP));
    }

    @Test
    public void complexSanityCheck21() throws Exception {
        sanityCheck(benchFromFolder("complexSanityCheck21"), NODE);
    }

    @Test
    public void complexSanityCheck24() throws Exception {
        // should only fail if complexSanityCheck21 also fails.
        sanityCheck(benchFromFolder("complexSanityCheck24"), NODE);
    }

    @Test
    public void complexGenerics3() throws Exception {
        RunResult result = run(benchFromFolder("complexGenerics3").withOptions(options -> options.setCombineAllUnboundGenerics(false)));

        assertThat(result.typeErrors, is(empty()));
    }

    @Test
    public void genericSignaturesSmokeTest() throws Exception {
        Main.generateFullDriver(benchFromFolder("genericSignaturesSmokeTest")); // smoke test
    }

    @Test
    public void genericSignaturesError() throws Exception {
        RunResult result = run("genericSignaturesError");

        expect(result)
                .forPath("MyModule.createElement()(typeParameter)")
                .expected("string")
                .got(TYPEOF, "number");
    }

    @Test
    public void genericSignaturesSmokeTest2() throws Exception {
        Main.generateFullDriver(benchFromFolder("genericSignaturesSmokeTest2"));
    }
}
