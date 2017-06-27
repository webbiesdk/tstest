package dk.webbies.tajscheck.test.experiments;

import dk.webbies.tajscheck.Main;
import dk.webbies.tajscheck.OutputParser;
import dk.webbies.tajscheck.benchmark.Benchmark;
import dk.webbies.tajscheck.test.dynamic.RunBenchmarks;

/**
 * Created by erik1 on 12-04-2017.
 */
public class RunSingleTrivial {
    @SuppressWarnings("Duplicates")
    public static void main(String[] args) throws Exception {
        if (args.length == 0 || RunBenchmarks.benchmarks.get(args[0]) == null) {
            System.out.println("Run as: \"ant run-trivial -Dname=X\", where X is replaced with the name of a benchmark");
            System.out.println("Valid names: ");
            System.out.println(String.join(" - ", RunBenchmarks.benchmarks.keySet()));

            return;
        }

        Benchmark bench = RunBenchmarks.benchmarks.get(args[0]).withOptions(options -> options.setMakeTSInferLike(true));

        System.out.println("Generating type test script");
        Main.writeFullDriver(bench);

        System.out.println("Running type test script");
        OutputParser.RunResult result = OutputParser.parseDriverResult(Main.runBenchmark(bench));

        System.out.println(result.typeErrors.size() + " mismatches found: \n");

        RunBenchmarks.printErrors(bench, result);

        System.exit(0);
    }
}
