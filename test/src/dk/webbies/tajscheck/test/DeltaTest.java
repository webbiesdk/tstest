package dk.webbies.tajscheck.test;

import dk.webbies.tajscheck.Main;
import dk.webbies.tajscheck.OutputParser;
import dk.webbies.tajscheck.benchmark.Benchmark;
import dk.webbies.tajscheck.benchmark.CheckOptions;
import dk.webbies.tajscheck.test.dynamic.RunBenchmarks;
import dk.webbies.tajscheck.util.Util;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Created by erik1 on 10-01-2017.
 */
public class DeltaTest {
    public static void main(String[] args) throws IOException {
        Benchmark bench = RunBenchmarks.benchmarks.get("Underscore.js");
//        Benchmark bench = RunBenchmarks.benchmarks.get("async");
//        Benchmark bench = RunBenchmarks.benchmarks.get("RxJS");
        bench = bench.withOptions(options -> options.setWriteAll(true)).withOptions(CheckOptions::errorFindingOptions).withOptions(options -> options.setMaxTime(1 * 1000));
        String testPath = "_().<>.chain().<>.sortBy";
//        String expected = "(undefined or (a non null value and Array and (arrayIndex: (null or ([any] and a non null value and a generic type marker (._isUnboundGeneric))))))";

        String driver = Util.readFile(Main.getFolderPath(bench) + Main.TEST_FILE_NAME);
        List<String> paths = Arrays.stream(driver.split(Pattern.quote("\n")))
                .map(String::trim)
                .map(str -> str.replaceAll("\r", ""))
                .filter(line -> line.startsWith("// path:"))
                .map(str -> str.substring("// path: ".length(), str.lastIndexOf(" type: ")))
                .map(String::trim)
                .collect(Collectors.toList());

        bench = bench.withPathsToTest(paths);

        while (true) {
            try {
                Main.generateSmallestDriver(bench, testHasTypeError(bench, new OutputParser.TypeError(testPath, null, null, null, null, null, null)));
                break;
            } catch (RuntimeException e) {
                System.err.println(e.getMessage());
                System.err.println("Trying again!");
                // continue
            }
        }
        System.exit(0);
    }

    public static Function<String, Collection<Integer>> testHasTypeError(Benchmark bench, OutputParser.TypeError typeError) {
        return (path) -> {
            try {
                String out = Main.runBenchmark(path, bench);
                OutputParser.RunResult result = OutputParser.parseDriverResult(out, true, null);
                Optional<OutputParser.TypeError> first = result.typeErrors.stream().filter(te -> {
                    if (typeError.typeof != null && !te.typeof.equals(typeError.typeof)) {
                        return false;
                    }
                    if (typeError.expected != null && !te.expected.equals(typeError.expected)) {
                        return false;
                    }
                    if (typeError.toString != null && !te.toString.equals(typeError.toString)) {
                        return false;
                    }
                    if (typeError.type != null && !te.type.equals(typeError.type)) {
                        return false;
                    }
                    if (typeError.JSON != null && !te.JSON.equals(typeError.JSON)) {
                        return false;
                    }
                    //noinspection RedundantIfStatement
                    if (te.getPath().equals(typeError.getPath())) {
                        return true;
                    } else {
                        return false;
                    }
                }).findFirst();
                return first.map(typeError1 -> typeError1.testsCalled).orElse(null);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        };
    }
}
