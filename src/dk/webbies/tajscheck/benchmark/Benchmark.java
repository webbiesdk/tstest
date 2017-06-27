package dk.webbies.tajscheck.benchmark;

import dk.webbies.tajscheck.parsespec.ParseDeclaration;
import dk.webbies.tajscheck.testcreator.TestCreator;
import dk.webbies.tajscheck.util.Util;

import java.io.File;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created by erik1 on 01-11-2016.
 */
public class Benchmark {
    public final String name;
    public final ParseDeclaration.Environment environment;
    public final String jsFile;
    public final String dTSFile;
    public final Set<String> pathsToTest;
    public final RUN_METHOD run_method;
    public final CheckOptions options;
    public final String exportName;
    private final List<Benchmark> dependencies;

    public Benchmark(String name, ParseDeclaration.Environment environment, String jsFile, String dTSFile, RUN_METHOD load_method, CheckOptions options, String exportName) {
        this(name, environment, jsFile, dTSFile, load_method, null, options, new ArrayList<>(), exportName);
    }

    public Benchmark(String name, ParseDeclaration.Environment environment, String jsFile, String dTSFile, RUN_METHOD load_method, CheckOptions options) {
        this(name, environment, jsFile, dTSFile, load_method, options, null);
    }

    private Benchmark(String name, ParseDeclaration.Environment environment, String jsFile, String dTSFile, RUN_METHOD load_method, Set<String> pathsToTest, CheckOptions options, List<Benchmark> dependencies, String exportName) {
        this.name = name;
        this.environment = environment;
        this.dTSFile = dTSFile;
        this.pathsToTest = pathsToTest;
        this.run_method = load_method;
        this.options = options;
        this.dependencies = dependencies;
        this.exportName = exportName;
        if (options.useTracified && !jsFile.contains("tracified")) {
            this.jsFile = Util.removeSuffix(jsFile, ".js") + "-tracified.js";
            assert new File(this.jsFile).exists();
        } else {
            this.jsFile = jsFile;
        }
    }

    public Benchmark withPathsToTest(Collection<String> pathsToTest) {
        return new Benchmark(
                this.name,
                this.environment,
                this.jsFile,
                this.dTSFile,
                this.run_method,
                Collections.unmodifiableSet(pathsToTest.stream().map(TestCreator::simplifyPath).collect(Collectors.toSet())),
                this.options,
                this.dependencies,
                this.exportName);
    }

    public Benchmark withRunMethod(RUN_METHOD method) {
        return new Benchmark(
                this.name,
                this.environment,
                this.jsFile,
                this.dTSFile,
                method,
                this.pathsToTest,
                this.options,
                this.dependencies,
                this.exportName);
    }

    public Benchmark addDependencies(Benchmark... benchmarks) {
        assert this.run_method == RUN_METHOD.BROWSER; // <- Only works for this one.
        if (benchmarks.length == 0) {
            throw new RuntimeException();
        }
        Benchmark clone = withRunMethod(this.run_method);// <- Clone
        for (Benchmark benchmark : benchmarks) {
            if (benchmark == null) {
                throw new RuntimeException();
            }
            clone.dependencies.add(benchmark);
        }

        return clone;
    }

    public List<Benchmark> getDependencies() {
        return Collections.unmodifiableList(dependencies);
    }

    public Benchmark withOptions(CheckOptions options) {
        return new Benchmark(
                this.name,
                this.environment,
                this.jsFile,
                this.dTSFile,
                this.run_method,
                this.pathsToTest,
                options,
                this.dependencies,
                this.exportName
        );
    }

    public Benchmark withOptions(CheckOptions.Builder options) {
        return withOptions(options.build());
    }

    public Benchmark withOptions(Function<CheckOptions.Builder, CheckOptions.Builder> transformer) {
        return new Benchmark(
                this.name,
                this.environment,
                this.jsFile,
                this.dTSFile,
                this.run_method,
                this.pathsToTest,
                transformer.apply(this.options.getBuilder()).build(),
                this.dependencies,
                this.exportName
        );
    }

    public enum RUN_METHOD {
        NODE,
        BROWSER,
        BOOTSTRAP
    }

    public String getJSName() {
        return this.jsFile.substring(this.jsFile.lastIndexOf('/') + 1, this.jsFile.length());
    }

    @Override
    public String toString() {
        return "Benchmark{" +
                 name +
                '}';
    }
}
