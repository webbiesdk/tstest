package dk.webbies.tajscheck.test;

import dk.webbies.tajscheck.benchmark.Benchmark;
import dk.webbies.tajscheck.parsespec.ParseDeclaration;
import dk.webbies.tajscheck.paser.AST.Statement;
import dk.webbies.tajscheck.paser.AstToStringVisitor;
import dk.webbies.tajscheck.paser.JavaScriptParser;
import dk.webbies.tajscheck.util.Util;
import org.hamcrest.core.Is;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;

/**
 * Created by erik1 on 03-01-2017.
 */
@RunWith(Parameterized.class)
public class TestParsing {

    @Parameterized.Parameter
    public boolean compact;

    @Parameterized.Parameters(name = "{0}")
    public static List<Boolean> getBenchmarks() {
        return Arrays.asList(true, false);
    }

    @Test
    public void testEscapedQuotes() throws Exception {
        testParse(
                "var test = \"\\\\[\" + whitespace + \"*(\" + identifier + \")(?:\" + whitespace +\n" +
                "                    // Operator (capture 2)\n" +
                "                    \"*([*^$|!~]?=)\" + whitespace +\n" +
                "                    // \"Attribute values must be CSS identifiers [capture 5] or strings [capture 3 or capture 4]\"\n" +
                "                    \"*(?:'((?:\\\\\\\\.|[^\\\\\\\\'])*)'|\\\"((?:\\\\\\\\.|[^\\\\\\\\\\\"])*)\\\"|(\" + identifier + \"))|)\" + whitespace +\n" +
                "                    \"*\\\\]\""
                , compact);

    }

    @Test
    public void testNewOnExpression() throws Exception {
        testParse(
                "new ((function () {\n" +
                "\n" +
                "\n" +
                "})());"
                , compact);
    }

    @Test
    public void labeledBreakWithNoWhile() throws Exception {
        testParse(
                "(function (global, factory) {}(this, (function (exports) { 'use strict';\n" +
                        "\tInterpolant.prototype = {\n" +
                        "\t\tevaluate: function( t ) {\n" +
                        "\t\t\tvalidate_interval: {\n" +
                        "\t\t\t\tseek: {\n" +
                        "\t\t\t\t\tlinear_scan: {\n" +
                        "\t\t\t\t\t\tforward_scan: if ( ! ( t < t1 ) ) {\n" +
                        "\t\t\t\t\t\t\tbreak linear_scan;\n" +
                        "\t\t\t\t\t\t}\n" +
                        "\t\t\t\t\t}\n" +
                        "\t\t\t\t} // seek\n" +
                        "\t\t\t} // validate_interval\n" +
                        "\t\t},\n" +
                        "\t}\n" +
                        "})));"
                , compact);
    }

    @Test
    public void forLoopsWithNoInitializer() throws Exception {
        String content = "for(;i;i--){}";
        JavaScriptParser parser = new JavaScriptParser(ParseDeclaration.Environment.ES5DOM);
        Statement iteration1Ast = parser.parse("name", content).toTSCreateAST().getBody();

        System.out.println("First parsing complete");

        String iteration1String = AstToStringVisitor.toString(iteration1Ast, compact);

        assertThat(iteration1String, not(containsString("null")));
    }

    @Test
    public void emptyForLoop() throws Exception {
        String content = "for(;;){}";
        JavaScriptParser parser = new JavaScriptParser(ParseDeclaration.Environment.ES5DOM);
        Statement iteration1Ast = parser.parse("name", content).toTSCreateAST().getBody();

        System.out.println("First parsing complete");

        String iteration1String = AstToStringVisitor.toString(iteration1Ast, compact);

        assertThat(iteration1String, not(containsString("null")));
    }

    @Test
    public void fewerQuotesInFields() throws Exception {
        String content = "var test = {a: 123}";
        JavaScriptParser parser = new JavaScriptParser(ParseDeclaration.Environment.ES5DOM);
        Statement iteration1Ast = parser.parse("name", content).toTSCreateAST().getBody();

        System.out.println("First parsing complete");

        String iteration1String = AstToStringVisitor.toString(iteration1Ast, compact);

        assertThat(iteration1String, not(containsString("\"")));
    }

    @Test
    public void stillSomeQuotesInFields() throws Exception {
        String content = "var test = {\"-\": 123}";
        JavaScriptParser parser = new JavaScriptParser(ParseDeclaration.Environment.ES5DOM);
        Statement iteration1Ast = parser.parse("name", content).toTSCreateAST().getBody();

        System.out.println("First parsing complete");

        String iteration1String = AstToStringVisitor.toString(iteration1Ast, compact);

        assertThat(iteration1String, containsString("\""));
    }

    @Test
    public void doWhile() throws Exception {
        String content = "do {} while (true);";
        JavaScriptParser parser = new JavaScriptParser(ParseDeclaration.Environment.ES5DOM);
        Statement iteration1Ast = parser.parse("name", content).toTSCreateAST().getBody();

        System.out.println("First parsing complete");

        String iteration1String = AstToStringVisitor.toString(iteration1Ast, compact);

        assertThat(iteration1String, containsString("do"));
    }

    @Test
    public void functionWithSemiColon() throws Exception {
        testParse(
                "var test = function () {\n" +
                "    ;\n" +
                "}"
                , compact);
    }

    @Test
    public void doWhileTest() throws Exception {
        testParse("do {} while (this.expect(','));", compact);
    }

    public static void testFile(String file, boolean compact) throws IOException {
        String script = Util.readFile(file);

        testParse(script, compact);
    }

    private static void testParse(String content, boolean compact) {
        JavaScriptParser parser = new JavaScriptParser(ParseDeclaration.Environment.ES5DOM);
        Statement iteration1Ast = parser.parse("name", content).toTSCreateAST().getBody();

        System.out.println("First parsing complete");

        String iteration1String = AstToStringVisitor.toString(iteration1Ast, compact);

        Statement iteration2Ast = parser.parse("name", iteration1String).toTSCreateAST().getBody();

        String iteration2String = AstToStringVisitor.toString(iteration2Ast, compact);

        assertThat(iteration1String, Is.is(equalTo(iteration2String)));
    }
}
