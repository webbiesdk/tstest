package dk.webbies.tajscheck.test.experiments;

import dk.webbies.tajscheck.Main;
import dk.webbies.tajscheck.OutputParser;
import dk.webbies.tajscheck.RunSmall;
import dk.webbies.tajscheck.benchmark.Benchmark;
import dk.webbies.tajscheck.test.DeltaTest;
import dk.webbies.tajscheck.test.dynamic.RunBenchmarks;
import dk.webbies.tajscheck.util.Pair;
import dk.webbies.tajscheck.util.Util;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Created by erik1 on 19-01-2017.
 */
public class ManualExperiment {
    private static final Scanner scanner = new Scanner(System.in);

    private static int minimizedResultCounter = 0;
    private static void fillWithRandomTypeErrors(BlockingQueue<Pair<String, OutputParser.TypeError>> queue, String singleBench) throws Exception {
        //noinspection InfiniteLoopStatement
        List<Pair<String, Benchmark>> benchmarks = RunBenchmarks.benchmarks.entrySet().stream().map(entry -> new Pair<>(entry.getKey(), entry.getValue())).collect(Collectors.toList());
        if (singleBench != null) {
            benchmarks = benchmarks.stream().filter(pair -> pair.getLeft().equals(singleBench)).collect(Collectors.toList());
            if (benchmarks.size() != 1) {
                throw new RuntimeException("Could not find the benchmark: " + singleBench);
            }
        }

        while (true) {
            Pair<String, Benchmark> benchmarkPair = Util.selectRandom(benchmarks);
            String name = benchmarkPair.getLeft();
            Benchmark benchmark = benchmarkPair.getRight();
            try {
                OutputParser.RunResult result = getSomeResult(benchmark);
                if (!result.typeErrors.isEmpty()) {
                    String driver = Util.readFile(Main.getFolderPath(benchmark) + Main.TEST_FILE_NAME);
                    List<String> paths = Arrays.stream(driver.split(Pattern.quote("\n")))
                            .map(String::trim)
                            .map(str -> str.replaceAll("\r", ""))
                            .filter(line -> line.startsWith("// path:"))
                            .map(str -> str.substring("// path: ".length(), str.lastIndexOf(" type: ")))
                            .map(String::trim)
                            .collect(Collectors.toList());

                    OutputParser.TypeError typeError = Util.selectRandom(result.typeErrors);
                    typeError.JSON = null;
                    typeError.toString = null;

                    String smallDriver;
                    try {
                        smallDriver = Main.generateSmallestDriver(benchmark.withPathsToTest(paths), DeltaTest.testHasTypeError(benchmark, typeError));
                    } catch (RuntimeException e) {
                        continue; // No point in trying any more.
                    }

                    String driverName = "minimizedDriver" + minimizedResultCounter++ + ".js";
                    Util.writeFile(Main.getFolderPath(benchmark) + driverName, smallDriver);

                    queue.put(new Pair<>(name + " (" + driverName + ")", typeError));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static OutputParser.RunResult getSomeResult(Benchmark benchmark) throws Exception {
        if (new Random().nextBoolean()) {
            Main.writeFullDriver(benchmark);
            return OutputParser.parseDriverResult(Main.runBenchmark(benchmark));
        } else {
            if (true) {
                throw new RuntimeException("The below code runs deterministically, thus ruining the experience.");
            }
            List<OutputParser.RunResult> results = RunSmall.runSmallDrivers(benchmark, RunSmall.runDriver(benchmark), 1, 10);
            return OutputParser.combine(results);
        }

    }

    public static void main(String[] args) throws Exception {
        String singleBench = null;
        if (args.length > 0) {
            singleBench = args[0];
            if (args.length > 1) {
                throw new RuntimeException("Can only support one argument");
            }
        }
        if (singleBench != null && singleBench.equals("${name}")) {
            singleBench = null;
        }
        BlockingQueue<Pair<String, OutputParser.TypeError>> queue = new LinkedBlockingQueue<>(100);
        String finalSingleBench = singleBench;
        Thread fillerThread = new Thread(() -> {
            try {
                fillWithRandomTypeErrors(queue, finalSingleBench);
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        });

        fillerThread.start();

//        manualCheck(queue, fillerThread);
        printToDisk(queue);
    }

    private static void printToDisk(BlockingQueue<Pair<String, OutputParser.TypeError>> queue) throws InterruptedException, IOException {
        while (true) {
            Pair<String, OutputParser.TypeError> error = queue.poll(30, TimeUnit.DAYS);

            Util.append("errors.txt", "\n\n" + error.left + "\n" + error.right.toString() + "\n");
            System.out.println("Wrote an error to errors.txt");
        }
    }

    private static void manualCheck(BlockingQueue<Pair<String, OutputParser.TypeError>> queue, Thread fillerThread) throws InterruptedException {
        int good = 0;
        int bad = 0;
        int unknown = 0;
        int nullChecks = 0;
        boolean exit = false;

        System.out.println("Setting up, now waiting for first type-error in the queue. ");
        while (true) {
            Pair<String, OutputParser.TypeError> error = queue.poll(30, TimeUnit.DAYS);
            System.out.println("Got a type-error, because multi-threading, I recommend waiting a bit");
            System.out.println("Press enter to continue");
            scanner.nextLine();

            System.out.println();
            System.out.println("TypeError from " + error.getLeft());
            System.out.println(error.getRight());
            System.out.println();
            System.out.println("Press G for good, B for bad, U for benign, N for failure due to strict-null-checks (and E to exit)");
            while (true) {
                String line = scanner.nextLine().toUpperCase();
                switch (line) {
                    case "G":
                        good++;
                        break;
                    case "B":
                        bad++;
                        break;
                    case "U":
                        unknown++;
                        break;
                    case "N":
                        nullChecks++;
                        break;
                    case "E":
                        exit = true;
                        break;
                    default:
                        System.out.println("Unknown command: " + line + ", try again!");
                        continue;
                }
                break;
            }
            System.out.println("Results so far: ");
            System.out.println("Good: " + good);
            System.out.println("Bad: " + bad);
            System.out.println("Nulls: " + nullChecks);
            System.out.println("Benign: " + unknown);
            System.out.println();
            if (exit) {
                fillerThread.interrupt();
                fillerThread.stop();
                break;
            }

        }
    }
}
