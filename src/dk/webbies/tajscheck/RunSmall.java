package dk.webbies.tajscheck;

import dk.webbies.tajscheck.benchmark.Benchmark;
import dk.webbies.tajscheck.benchmark.BenchmarkInfo;
import dk.webbies.tajscheck.buildprogram.DriverProgramBuilder;
import dk.webbies.tajscheck.paser.AST.Statement;
import dk.webbies.tajscheck.paser.AstToStringVisitor;
import dk.webbies.tajscheck.testcreator.TestCreator;
import dk.webbies.tajscheck.testcreator.test.Test;
import dk.webbies.tajscheck.util.Util;
import dk.webbies.tajscheck.util.trie.Trie;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class RunSmall {
    public static <T> List<T> runSmallDrivers(Benchmark orgBench, Function<String, T> runner) throws IOException {
        return runSmallDrivers(orgBench, runner, Integer.MAX_VALUE, 1);
    }

    public static <T> List<T> runSmallDrivers(Benchmark orgBench, Function<String, T> runner, int runsLimit, int collectionSizeLimit) throws IOException {
        assert runsLimit > 0;

        List<List<String>> paths = getPathsToTest(orgBench, runsLimit, collectionSizeLimit);

        return runSmallDrivers(orgBench, runner, paths);
    }

    public static <T> List<T> runSmallDrivers(Benchmark orgBench, Function<String, T> runner, List<List<String>> paths) throws IOException {
        BenchmarkInfo info = BenchmarkInfo.create(orgBench);

        List<T> result = new ArrayList<>();

        for (int i = 0; i < paths.size(); i++) {
            List<String> path = paths.get(i);

            Benchmark bench = orgBench.withPathsToTest(path);

            System.out.println("Creating small driver for: " + path + "  " + (i + 1) + "/" + paths.size());

            List<Test> specificTests = new TestCreator(info.withBench(bench)).createTests();

            Statement program = new DriverProgramBuilder(specificTests, info).buildDriver(null);

            String filePath = Main.getFolderPath(bench) + Main.TEST_FILE_NAME;

            Util.writeFile(filePath, AstToStringVisitor.toString(program, info.options.compactOutput));

            try {
                result.add(runner.apply(filePath));
            } catch (Throwable e) {
                System.out.println("Got exception: " + e + ", while running small driver...");
            }
        }

        return result;
    }

    public static List<List<String>> getPathsToTest(Benchmark orgBench, int runsLimit, int collectionSizeLimit) {
        BenchmarkInfo firstInfo = BenchmarkInfo.create(orgBench);

        List<String> allPaths = new TestCreator(firstInfo).createTests(false).stream().map(Test::getPath).map(TestCreator::simplifyPath).collect(Collectors.toList());

        allPaths = allPaths.stream().filter(path -> !path.contains("[arg")).collect(Collectors.toList());

        Trie trie = Trie.create(allPaths);
        List<String> prefixFixedPaths = allPaths.stream().filter(Util.not(trie::containsChildren)).distinct().collect(Collectors.toList());
        Collections.shuffle(prefixFixedPaths, new Random(1337));

        collectionSizeLimit = Math.max(1, Math.min(collectionSizeLimit, prefixFixedPaths.size() / runsLimit));

        Iterator<String> iterator = prefixFixedPaths.iterator();
        List<List<String>> paths = new ArrayList<>();
        while (iterator.hasNext()) {
            ArrayList<String> subCollection = new ArrayList<>();
            paths.add(subCollection);
            for (int i = 0; i < collectionSizeLimit; i++) {
                if (!iterator.hasNext()) {
                    break;
                }
                subCollection.add(iterator.next());
            }
        }

        if (paths.size() > runsLimit) {
            paths = paths.subList(0, runsLimit);
        }

        File dir = new File(Main.getFolderPath(orgBench));
        if (!dir.exists()) {
            boolean created = dir.mkdir();
            assert created;
        }
        return paths;
    }

    public static Function<String, CoverageResult> runCoverage(Benchmark bench) {
        return (path) -> {
            try {
                path = path.substring(Main.getFolderPath(bench).length());
                Map<String, CoverageResult> coverage = Main.genCoverage(bench, path);
                return coverage.get(bench.getJSName());
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        };
    }

    public static Function<String, OutputParser.RunResult> runDriver(Benchmark bench) {
        return (path) -> {
            try {
                return OutputParser.parseDriverResult(Main.runBenchmark(path, bench));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
    }
}