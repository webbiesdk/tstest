package dk.webbies.tajscheck.paser;


import com.google.javascript.jscomp.parsing.Config;
import com.google.javascript.jscomp.parsing.Config.LanguageMode;
import com.google.javascript.jscomp.parsing.ConfigExposer;
import com.google.javascript.jscomp.parsing.ParserRunner;
import com.google.javascript.jscomp.parsing.parser.Parser;
import com.google.javascript.jscomp.parsing.parser.Parser.Config.Mode;
import com.google.javascript.jscomp.parsing.parser.SourceFile;
import com.google.javascript.jscomp.parsing.parser.trees.ProgramTree;
import com.google.javascript.jscomp.parsing.parser.util.SourcePosition;
import com.google.javascript.jscomp.parsing.parser.util.SourceRange;
import com.google.javascript.rhino.ErrorReporter;
import dk.webbies.tajscheck.parsespec.ParseDeclaration;
import dk.webbies.tajscheck.paser.AST.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * JavaScript parser.
 * Based on the parser from the Google Closure Compiler.
 *
 * And this is based on the Google Closure Compiler interface in TAJS.
 */
public class JavaScriptParser {
    private final Mode mode;
    private final Config config;
    private final ParseDeclaration.Environment environment;

    /**
     * Constructs a new parser.
     * @param environment
     */
    public JavaScriptParser(ParseDeclaration.Environment environment) {
        this.environment = environment;
        LanguageMode m;
        switch (environment) {
            case ES5Core:
            case ES5DOM:
                this.mode = Mode.ES5;
                m = LanguageMode.ECMASCRIPT5;
                break;
            case ES6Core:
            case ES6DOM:
                this.mode = Mode.ES6;
                m = LanguageMode.ECMASCRIPT6;
                break;
            default:
                throw new RuntimeException("Unexpected enum: " + environment);
        }
        config = ConfigExposer.createConfig(new HashSet<>(), new HashSet<>(), m);
    }

    /**
     * Parses the given JavaScript code.
     * The syntax check includes break/continue label consistency and no duplicate parameters.
     *
     * @param name     file name or URL of the code
     * @param contents the code
     *
     *                 new ErrorReporter() {
     */
    public ParseResult parse(String name, String contents) {
        final List<SyntaxMesssage> warnings = new ArrayList<>();
        final List<SyntaxMesssage> errors = new ArrayList<>();
        ParserRunner.parse(new com.google.javascript.jscomp.SourceFile(name), contents, config, new ErrorReporter() {
            @Override
            public void warning(String message, String name2, int lineNumber, int columnNumber) {
                warnings.add(new SyntaxMesssage(message, new SourceLocation(lineNumber, columnNumber + 1, name2)));
            }

            @Override
            public void error(String message, String name2, int lineNumber, int columnNumber) {
                errors.add(new SyntaxMesssage(message, new SourceLocation(lineNumber, columnNumber + 1, name2)));
            }
        });
        ProgramTree programAST = null;
        if (errors.isEmpty()) {
            programAST = new Parser(new Parser.Config(mode), new MutedErrorReporter(), new SourceFile(name, contents)).parseProgram();
        }
        return new ParseResult(programAST, errors, warnings, environment);
    }

    /**
     * Syntax error message.
     */
    static class SyntaxMesssage {

        private final String message;

        private final SourceLocation sourceLocation;

        /**
         * Constructs a new syntax error message object.
         */
        SyntaxMesssage(String message, SourceLocation sourceLocation) {
            this.message = message;
            this.sourceLocation = sourceLocation;
        }

        /**
         * Returns the message.
         */
        String getMessage() {
            return message;
        }

        /**
         * Returns the source location.
         */
        SourceLocation getSourceLocation() {
            return sourceLocation;
        }
    }

    /**
     * Result from parser.
     */
    public static class ParseResult {

        private ProgramTree programAST;

        private final List<SyntaxMesssage> errors;

        private final List<SyntaxMesssage> warnings;
        private ParseDeclaration.Environment environment;

        private ParseResult(ProgramTree programAST, List<SyntaxMesssage> errors, List<SyntaxMesssage> warnings, ParseDeclaration.Environment environment) {
            this.programAST = programAST;
            this.errors = errors;
            this.warnings = warnings;
            this.environment = environment;
        }

        /**
         * Returns the AST, or null if parse error.
         */
        ProgramTree getProgramAST() {
            return programAST;
        }

        /**
         * Returns the list of parse errors.
         */
        List<SyntaxMesssage> getErrors() {
            return errors;
        }

        /**
         * Returns the list of parse warnings.
         */
        List<SyntaxMesssage> getWarnings() {
            return warnings;
        }

        public FunctionExpression toTSCreateAST() {
            if (this.getErrors().size() > 0) {
                for (SyntaxMesssage messsage : this.getErrors()) {
                    System.out.println(messsage.getMessage());
                }
            }
            AstTransformer transformer = new AstTransformer(environment);
            List<Statement> body = this.programAST.sourceElements.stream().map(transformer::convert).map(JavaScriptParser::toStatement).collect(Collectors.toList());
            if (body.isEmpty()) {
                SourcePosition position = new SourcePosition(new SourceFile("empty", ""), 0, 0, 0);
                SourceRange location = new SourceRange(position, position);
                return new FunctionExpression(location, new Identifier(location, ":program"), new BlockStatement(location, Collections.EMPTY_LIST), Collections.EMPTY_LIST);
            } else {
                SourceRange location = new SourceRange(body.get(0).location.start, body.get(body.size() - 1).location.end);

                return new FunctionExpression(location, new Identifier(location, ":program"), new BlockStatement(location, body), Collections.EMPTY_LIST);
            }
        }
    }

    public static Statement toStatement(AstNode node) {
        if (node instanceof Statement) {
            return (Statement)node;
        } else if (node instanceof FunctionExpression) {
            return new ExpressionStatement(node.location, (FunctionExpression) node);
        } else {
            throw new RuntimeException("Cannot make class into a statement for the top-program: " + node.getClass().getName());
        }
    }

    public class SourceLocation {
        private final int lineNumber;
        private final int columnNumber;
        private final String name2;

        public SourceLocation(int lineNumber, int columnNumber, String name2) {
            this.lineNumber = lineNumber;
            this.columnNumber = columnNumber;
            this.name2 = name2;
        }
    }

    private class MutedErrorReporter extends com.google.javascript.jscomp.parsing.parser.util.ErrorReporter {
        @Override
        protected void reportError(SourcePosition sourcePosition, String s) { }

        @Override
        protected void reportWarning(SourcePosition sourcePosition, String s) { }
    }

}
