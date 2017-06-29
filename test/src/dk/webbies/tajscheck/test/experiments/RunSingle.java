package dk.webbies.tajscheck.test.experiments;

import dk.au.cs.casa.typescript.types.Type;
import dk.webbies.tajscheck.Main;
import dk.webbies.tajscheck.OutputParser;
import dk.webbies.tajscheck.benchmark.Benchmark;
import dk.webbies.tajscheck.benchmark.CheckOptions;
import dk.webbies.tajscheck.test.dynamic.RunBenchmarks;
import dk.webbies.tajscheck.util.MultiMap;
import dk.webbies.tajscheck.util.Pair;
import dk.webbies.tajscheck.util.Util;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Created by erik1 on 12-04-2017.
 */
public class RunSingle {
    public static void main(String[] args) throws Exception {
        if (args.length == 0 || RunBenchmarks.benchmarks.get(args[0]) == null) {
            System.out.println("Run as: \"ant run -Dname=X\", where X is replaced with the name of a benchmark");
            System.out.println("Valid names: ");
            System.out.println(String.join(" - ", RunBenchmarks.benchmarks.keySet()
                    .stream().filter(Util.not(str -> str.contains("motivating"))).collect(Collectors.toList())
            ));

            return;
        }

        Benchmark bench = RunBenchmarks.benchmarks.get(args[0]).withOptions(CheckOptions::errorFindingOptions).withOptions(options -> options.setMaxTime(10 * 1000));

        System.out.println("Generating type test script");
        Main.writeFullDriver(bench);

        System.out.println("Running type test script");
        OutputParser.RunResult result = OutputParser.parseDriverResult(Main.runBenchmark(bench));

        System.out.println(CountUniques.uniqueWarnings(result.typeErrors, bench) + " mismatches found: \n");

        RunBenchmarks.printErrors(bench, result);

        System.exit(0);
    }
}
