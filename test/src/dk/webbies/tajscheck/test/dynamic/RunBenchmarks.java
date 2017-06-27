package dk.webbies.tajscheck.test.dynamic;

import dk.au.cs.casa.typescript.types.Type;
import dk.au.cs.casa.typescript.types.Type;
import dk.webbies.tajscheck.CoverageResult;
import dk.webbies.tajscheck.Main;
import dk.webbies.tajscheck.OutputParser;
import dk.webbies.tajscheck.RunSmall;
import dk.webbies.tajscheck.benchmark.Benchmark;
import dk.webbies.tajscheck.benchmark.BenchmarkInfo;
import dk.webbies.tajscheck.benchmark.CheckOptions;
import dk.webbies.tajscheck.parsespec.ParseDeclaration;
import dk.webbies.tajscheck.paser.AST.Statement;
import dk.webbies.tajscheck.test.TestParsing;
import dk.webbies.tajscheck.test.experiments.CountUniques;
import dk.webbies.tajscheck.testcreator.TestCreator;
import dk.webbies.tajscheck.util.MultiMap;
import dk.webbies.tajscheck.util.Pair;
import dk.webbies.tajscheck.test.experiments.CountUniques;
import dk.webbies.tajscheck.util.MultiMap;
import dk.webbies.tajscheck.util.Pair;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;

import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static dk.webbies.tajscheck.benchmark.Benchmark.RUN_METHOD.*;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Created by erik1 on 22-11-2016.
 */
@RunWith(Parameterized.class)
public class RunBenchmarks {

    @SuppressWarnings("WeakerAccess")
    @Parameterized.Parameter
    public Benchmark benchmark = null;

    @SuppressWarnings("WeakerAccess")
    public static final Map<String, Benchmark> benchmarks = new HashMap<>();

    private static void register(Benchmark benchmark) {
        assert !benchmarks.containsKey(benchmark.name);
        benchmarks.put(benchmark.name, benchmark);
    }

    static {
        CheckOptions options = CheckOptions.builder().setSplitUnions(false).build();

        register(new Benchmark("Moment.js", ParseDeclaration.Environment.ES5Core, "test/benchmarks/moment/moment.js", "test/benchmarks/moment/moment.d.ts", BROWSER, options));
        register(new Benchmark("async", ParseDeclaration.Environment.ES5Core, "test/benchmarks/async/async.js", "test/benchmarks/async/async.d.ts", BROWSER, options));
        register(new Benchmark("pathjs", ParseDeclaration.Environment.ES5Core, "test/benchmarks/pathjs/pathjs.js", "test/benchmarks/pathjs/pathjs.d.ts", BROWSER, options));
        register(new Benchmark("accounting.js", ParseDeclaration.Environment.ES5Core, "test/benchmarks/accounting/accounting.js", "test/benchmarks/accounting/accounting.d.ts", NODE, options));
        register(new Benchmark("lunr.js", ParseDeclaration.Environment.ES5Core, "test/benchmarks/lunr/lunr.js", "test/benchmarks/lunr/lunr.d.ts", NODE, options));
        register(new Benchmark("PixiJS", ParseDeclaration.Environment.ES5Core, "test/benchmarks/pixi/pixi.js", "test/benchmarks/pixi/pixi.d.ts", BROWSER, options));

        /*"fixedMoment", new Benchmark(ParseDeclaration.Environment.ES5Core, "test/benchmarks/fixedMoment/moment.js", "test/benchmarks/fixedMoment/moment.d.ts", "moment", NODE, options));*/


        register(new Benchmark("Ace", ParseDeclaration.Environment.ES5Core, "test/benchmarks/ace/ace.js", "test/benchmarks/ace/ace.d.ts", BROWSER, options));
        Benchmark jQuery = new Benchmark("jQuery", ParseDeclaration.Environment.ES5Core, "test/benchmarks/jquery/jquery.js", "test/benchmarks/jquery/jquery.d.ts", BROWSER, options);
        register(jQuery);


        Benchmark angular = new Benchmark("AngularJS", ParseDeclaration.Environment.ES5Core, "test/benchmarks/angular1/angular1.js", "test/benchmarks/angular1/angular1.d.ts", BROWSER, options).addDependencies(jQuery);
        register(angular);


        register(new Benchmark("box2dweb", ParseDeclaration.Environment.ES5Core, "test/benchmarks/box2dweb/box2dweb.js", "test/benchmarks/box2dweb/box2dweb.d.ts", BROWSER, options));

        Benchmark underscore = new Benchmark("Underscore.js", ParseDeclaration.Environment.ES5Core, "test/benchmarks/underscore/underscore.js", "test/benchmarks/underscore/underscore.d.ts", NODE,
                options.getBuilder()
                .setFirstMatchSignaturePolicy(false)
                .build()
        );
        register(underscore);

        Benchmark handlebars = new Benchmark("Handlebars", ParseDeclaration.Environment.ES6DOM, "test/benchmarks/handlebars/handlebars.js", "test/benchmarks/handlebars/handlebars.d.ts", BROWSER, options);
        register(handlebars);

        register(new Benchmark("Hammer.js", ParseDeclaration.Environment.ES5Core, "test/benchmarks/hammer/hammer.js", "test/benchmarks/hammer/hammer.d.ts", BROWSER, options));

        register(new Benchmark("Jasmine", ParseDeclaration.Environment.ES5Core, "test/benchmarks/jasmine/jasmine.js", "test/benchmarks/jasmine/jasmine.d.ts", BROWSER, options));

        register(new Benchmark("Knockout", ParseDeclaration.Environment.ES5Core, "test/benchmarks/knockout/knockout.js", "test/benchmarks/knockout/knockout.d.ts", BROWSER, options));

        register(new Benchmark("Fabric.js", ParseDeclaration.Environment.ES5Core, "test/benchmarks/fabric/fabric.js", "test/benchmarks/fabric/fabric.d.ts", BROWSER, options));

        register(new Benchmark("Ember.js", ParseDeclaration.Environment.ES5DOM, "test/benchmarks/ember/ember.js", "test/benchmarks/ember/ember.d.ts", BROWSER, options)
            .addDependencies(jQuery, handlebars)
        );

        register(new Benchmark("D3.js", ParseDeclaration.Environment.ES5Core, "test/benchmarks/d3/d3.js", "test/benchmarks/d3/d3.d.ts", BROWSER, options));

        register(new Benchmark("MathJax", ParseDeclaration.Environment.ES5Core, "test/benchmarks/mathjax/mathjax.js", "test/benchmarks/mathjax/mathjax.d.ts", BROWSER, options));

        register(new Benchmark("PeerJS", ParseDeclaration.Environment.ES5Core, "test/benchmarks/peerjs/peerjs.js", "test/benchmarks/peerjs/peerjs.d.ts", BROWSER, options));
        register(new Benchmark("PleaseJS", ParseDeclaration.Environment.ES5Core, "test/benchmarks/pleasejs/please.js", "test/benchmarks/pleasejs/please.d.ts", NODE, options));
        Benchmark webcomponents = new Benchmark("webcomponents.js", ParseDeclaration.Environment.ES5Core, "test/benchmarks/webcomponents/webcomponents.js", "test/benchmarks/webcomponents/webcomponents.d.ts", BROWSER, options); // Doesn't really directly expose an API, so I'm just keeping it as dependency only.
        register(new Benchmark("Polymer", ParseDeclaration.Environment.ES5Core, "test/benchmarks/polymer/polymer.js", "test/benchmarks/polymer/polymer.d.ts", BROWSER, options).addDependencies(webcomponents));
        register(new Benchmark("q", ParseDeclaration.Environment.ES5Core, "test/benchmarks/q/q.js", "test/benchmarks/q/q.d.ts", NODE, options));
        register(new Benchmark("QUnit", ParseDeclaration.Environment.ES5Core, "test/benchmarks/qunit/qunit.js", "test/benchmarks/qunit/qunit.d.ts", BROWSER, options));
        Benchmark react = new Benchmark("React", ParseDeclaration.Environment.ES5Core, "test/benchmarks/react/react.js", "test/benchmarks/react/react.d.ts", BROWSER, options);
        register(react);
        register(new Benchmark("RequireJS", ParseDeclaration.Environment.ES5Core, "test/benchmarks/requirejs/require.js", "test/benchmarks/requirejs/requirejs.d.ts", BROWSER, options).addDependencies(jQuery));
        register(new Benchmark("Sugar", ParseDeclaration.Environment.ES6DOM, "test/benchmarks/sugar/sugar.js", "test/benchmarks/sugar/sugar.d.ts", NODE,
                options.getBuilder()
                .setDisableGenerics(true)
                .build()

        ));

        register(new Benchmark("PhotoSwipe", ParseDeclaration.Environment.ES5Core, "test/benchmarks/photoswipe/photoswipe.js", "test/benchmarks/photoswipe/photoswipe.d.ts", BROWSER, options));
        register(new Benchmark("CreateJS", ParseDeclaration.Environment.ES5Core, "test/benchmarks/createjs/createjs.js", "test/benchmarks/createjs/createjs.d.ts", BROWSER, options));
        register(new Benchmark("Vue.js", ParseDeclaration.Environment.ES6DOM, "test/benchmarks/vue/vue.js", "test/benchmarks/vue/index.d.ts", BROWSER, options));
        register(new Benchmark("three.js", ParseDeclaration.Environment.ES6DOM, "test/benchmarks/three/three.js", "test/benchmarks/three/three.d.ts", BROWSER,
                options.getBuilder()
                .setDisableGenerics(true)
                .build()
        ));
        register(new Benchmark("Leaflet", ParseDeclaration.Environment.ES5Core, "test/benchmarks/leaflet/leaflet.js", "test/benchmarks/leaflet/leaflet.d.ts", BROWSER, options));

        register(new Benchmark("Backbone.js", ParseDeclaration.Environment.ES5Core, "test/benchmarks/backbone/backbone.js", "test/benchmarks/backbone/backbone.d.ts", BROWSER, options)
                .addDependencies(underscore)
                .addDependencies(jQuery)
        );

        register(new Benchmark("Lodash", ParseDeclaration.Environment.ES5Core, "test/benchmarks/lodash/lodash.js", "test/benchmarks/lodash/lodash.d.ts", NODE,
                options.getBuilder()
                        .setDisableGenerics(true)
                        .build()
        ));

        register(new Benchmark("P2.js", ParseDeclaration.Environment.ES5Core, "test/benchmarks/p2/p2.js", "test/benchmarks/p2/p2.d.ts", BROWSER, options));

        register(new Benchmark("Zepto.js", ParseDeclaration.Environment.ES5Core, "test/benchmarks/zepto/zepto.js", "test/benchmarks/zepto/zepto.d.ts", BROWSER, options));

        register(new Benchmark("Redux", ParseDeclaration.Environment.ES5Core, "test/benchmarks/redux/redux.js", "test/benchmarks/redux/reduxModule.d.ts", NODE, options, "redux"));

        register(new Benchmark("Ionic", ParseDeclaration.Environment.ES5Core, "test/benchmarks/ionic/ionic.js", "test/benchmarks/ionic/ionic.d.ts", BROWSER, options)
            .addDependencies(jQuery)
            .addDependencies(angular)
        );

        register(new Benchmark("Foundation", ParseDeclaration.Environment.ES5Core, "test/benchmarks/foundation/foundation.js", "test/benchmarks/foundation/foundation.d.ts", BROWSER, options)
                .addDependencies(jQuery)
        );

        register(new Benchmark("Chart.js", ParseDeclaration.Environment.ES5Core, "test/benchmarks/chartjs/chart.js", "test/benchmarks/chartjs/chart.d.ts", BROWSER, options));

        register(new Benchmark("Video.js", ParseDeclaration.Environment.ES5Core, "test/benchmarks/video/video.js", "test/benchmarks/video/video.d.ts", BROWSER, options));

        register(new Benchmark("reveal.js", ParseDeclaration.Environment.ES5Core, "test/benchmarks/reveal/reveal.js", "test/benchmarks/reveal/reveal.d.ts", BROWSER, options));

        Benchmark pickadate = new Benchmark("pickadate.js", ParseDeclaration.Environment.ES5Core, "test/benchmarks/pickadate/picker.js", "test/benchmarks/pickadate/pickadate.d.ts", BROWSER, options).addDependencies(jQuery); // Just a jQuery plugin, I therefore don't test it.
        register(new Benchmark("Materialize", ParseDeclaration.Environment.ES5Core, "test/benchmarks/materialize/materialize.js", "test/benchmarks/materialize/materialize.d.ts", BROWSER, options)
                .addDependencies(jQuery)
                .addDependencies(pickadate)
        );

        register(new Benchmark("CodeMirror", ParseDeclaration.Environment.ES5Core, "test/benchmarks/codemirror/codemirror.js", "test/benchmarks/codemirror/codemirror.d.ts", BROWSER, options));

        register(new Benchmark("bluebird", ParseDeclaration.Environment.ES5Core, "test/benchmarks/bluebird/bluebird.js", "test/benchmarks/bluebird/bluebird.d.ts", NODE, options));

        register(new Benchmark("Modernizr", ParseDeclaration.Environment.ES5Core, "test/benchmarks/modernizr/modernizr.js", "test/benchmarks/modernizr/modernizr.d.ts", BROWSER, options));

        register(new Benchmark("RxJS", ParseDeclaration.Environment.ES5Core, "test/benchmarks/rx/Rx.js", "test/benchmarks/rx/types/rx/index.d.ts", NODE, options.getBuilder().setDisableGenerics(true).build(), "\"rx\""));

        register(new Benchmark("PDF.js", ParseDeclaration.Environment.ES5Core, "test/benchmarks/pdf/pdf.js", "test/benchmarks/pdf/pdf.d.ts", BROWSER, options));

        register(new Benchmark("highlight.js", ParseDeclaration.Environment.ES5Core, "test/benchmarks/highlight/highlight.js", "test/benchmarks/highlight/highlight.d.ts", BROWSER, options));

        register(new Benchmark("intro.js", ParseDeclaration.Environment.ES5Core, "test/benchmarks/intro/intro.js", "test/benchmarks/intro/intro.d.ts", BROWSER, options));

        register(new Benchmark("Swiper", ParseDeclaration.Environment.ES5Core, "test/benchmarks/swiper/swiper.js", "test/benchmarks/swiper/swiper.d.ts", BROWSER, options));

        register(new Benchmark("axios", ParseDeclaration.Environment.ES5Core, "test/benchmarks/axios/axios.js", "test/benchmarks/axios/axios.d.ts", NODE, options));

        register(new Benchmark("Medium Editor", ParseDeclaration.Environment.ES5Core, "test/benchmarks/medium-editor/medium-editor.js", "test/benchmarks/medium-editor/medium-editor.d.ts", BROWSER, options));

        register(new Benchmark("Sortable", ParseDeclaration.Environment.ES5Core, "test/benchmarks/sortable/sortable.js", "test/benchmarks/sortable/sortable.d.ts", BROWSER, options));


        // If need more benchmarks, get some from here: https://www.javascripting.com/?p=5
    }

    @Parameterized.Parameters(name = "{0}")
    public static List<Benchmark> getBenchmarks() {
        return new ArrayList<>(benchmarks.values());
    }

    @Test
    public void genFullDriver() throws Exception {
        Main.writeFullDriver(benchmark);
    }

    @Test
    public void runSmallDrivers() throws Exception {
        OutputParser.RunResult result = OutputParser.combine(RunSmall.runSmallDrivers(benchmark, RunSmall.runDriver(benchmark), 3, Integer.MAX_VALUE));

        for (OutputParser.TypeError typeError : result.typeErrors) {
            System.out.println(typeError);
        }
    }

    @Test
    public void runFullDriver() throws Exception {
        // Write the driver
        Main.writeFullDriver(benchmark.withOptions(CheckOptions::errorFindingOptions).withOptions(options -> options.setCombineAllUnboundGenerics(true)));

        String out = Main.runBenchmark(benchmark);
//        System.out.println(out);

        // Parse and print the result
        OutputParser.RunResult result = OutputParser.parseDriverResult(out);

        printErrors(benchmark, result);


        assert !out.trim().isEmpty();
    }

    @Test
    @Ignore
    public void coverage() throws Exception {
        if (Stream.of("underscore.d.ts", "fabric", "d3.d.ts", "backbone.d.ts", "three.d.ts").anyMatch(benchmark.dTSFile::contains)) {
            return; // Too big, node runs out of memory generating the instrumented version.
        }
        Map<String, CoverageResult> out = Main.genCoverage(benchmark);
        System.out.println("Coverage for " + benchmark.dTSFile);

        System.out.println(out);
    }

    @Test
    public void soundnessTest() throws Exception {
        Benchmark benchmark = this.benchmark.withRunMethod(BOOTSTRAP).withOptions(options -> options.setMaxIterationsToRun(100 * 1000).setConstructAllTypes(true).setCheckDepthReport(0));
        if (
                benchmark.dTSFile.contains("box2dweb.d.ts") ||// box2dweb uses bivariant function arguments, which is unsound, and causes this soundness-test to fail. (demonstrated in complexSanityCheck3)
                benchmark.dTSFile.contains("leaflet.d.ts") || // same unsoundness in leaflet. (Demonstrated in complexSanityCheck9)
                benchmark.dTSFile.contains("jquery.d.ts") || // Exactly the same thing, the two then methods of JQueryGenericPromise are being overridden in an unsound way.
                benchmark.dTSFile.contains("fabric.d.ts") || // Unsoundness in the noTransform argument of the render method (and that is it!).
                benchmark.dTSFile.contains("p2.d.ts") || // Has a class, that has a static length() function, this is not possible. (The class contains only static methods, go figure).
                benchmark.dTSFile.contains("ember.d.ts") || // It includes jQuery, therefore it fails.
                benchmark.dTSFile.contains("materialize.d.ts") || // Includes jQuery.
                benchmark.dTSFile.contains("three.d.ts") || // bivariant function-arguments in the addGroup() method (the last argument is optional in the base class, but non-optional in the sub class).
                benchmark.dTSFile.contains("backbone.d.ts")  || // Includes jQuery.
                benchmark.dTSFile.contains("foundation.d.ts")  || // Includes jQuery.
                benchmark.dTSFile.contains("angular1.d.ts") ||  // Includes jQuery.
                benchmark.dTSFile.contains("ionic.d.ts")  || // Includes angular, which includes jQuery.
                benchmark.dTSFile.contains("sugar.d.ts") // has known unsoundness, demonstrated in complexSanityCheck23
        ) {
            System.out.println("Is a benchmark which i know to fail. ");
            return;
        }

        Main.writeFullDriver(benchmark); // No seed specified, in case of failure, the seed can be seen from the output.
        System.out.println("Driver written");
        String output = Main.runBenchmark(benchmark);
        OutputParser.RunResult result = OutputParser.parseDriverResult(output);

        for (OutputParser.TypeError typeError : result.typeErrors) {
            System.out.println(typeError);
        }


        assertThat(result.typeErrors.size(), is(0));
    }

    @Test
    public void testParsing() throws Exception {
        // A sanitycheck that JavaScript parsing+printing is idempotent.
        System.out.println(benchmark.jsFile);
        TestParsing.testFile(benchmark.jsFile);
    }

    public static void printErrors(Benchmark bench, OutputParser.RunResult result) {
        MultiMap<Pair<Type, String>, OutputParser.TypeError> groups = CountUniques.groupWarnings(result.typeErrors, bench);

        for (Map.Entry<Pair<Type, String>, Collection<OutputParser.TypeError>> entry : groups.asMap().entrySet()) {
            Collection<OutputParser.TypeError> errors = entry.getValue();
            assert !errors.isEmpty();

            if (errors.size() == 1) {
                System.out.println(errors.iterator().next());
                System.out.println();
            } else {
                System.out.println("Group of " + errors.size() + " similar errors");
                for (OutputParser.TypeError error : errors) {
                    String errorIndented = String.join("\n", Arrays.stream(error.toString().split(Pattern.quote("\n"))).map(line -> "   " + line).collect(Collectors.toList()));
                    System.out.println(errorIndented);
                    System.out.println();
                }
                System.out.println();

            }
        }
    }
}
