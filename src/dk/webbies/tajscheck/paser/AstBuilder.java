package dk.webbies.tajscheck.paser;

import com.google.common.io.Resources;
import dk.webbies.tajscheck.parsespec.ParseDeclaration;
import dk.webbies.tajscheck.paser.AST.*;
import dk.webbies.tajscheck.testcreator.test.check.TypeOfCheck;
import dk.webbies.tajscheck.util.Pair;
import dk.webbies.tajscheck.util.Util;
import org.apache.commons.io.Charsets;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Created by erik1 on 01-11-2016.
 */
public class AstBuilder {
    public static VariableNode variable(Expression lValue, Expression init) {
        return new VariableNode(null, lValue, init);
    }

    public static VariableNode variable(String id, Expression init) {
        return new VariableNode(null, identifier(id), init);
    }

    public static Identifier identifier(String name) {
        return new Identifier(null, name);
    }

    public static CallExpression call(Expression function, Expression... args) {
        return call(function, Arrays.asList(args));
    }

    public static CallExpression call(Expression function, List<? extends Expression> args) {
        return new CallExpression(null, function, args);
    }

    public static StringLiteral string(String str) {
        return new StringLiteral(null, str);
    }

    public static NewExpression newCall(Expression function, Expression... args) {
        return newCall(function, Arrays.asList(args));
    }

    public static NewExpression newCall(Expression function, List<Expression> args) {
        return new NewExpression(null, function, args);
    }

    public static FunctionExpression function(String name, BlockStatement body, String... args) {
        return function(name, body, Arrays.asList(args));
    }

    public static FunctionExpression function(String name, Statement body, String... args) {
        return function(name, block(body), Arrays.asList(args));
    }

    public static FunctionExpression function(String name, BlockStatement body, List<String> args) {
        Identifier nameIdentifier = null;
        if (name != null) {
            nameIdentifier = identifier(name);
        }
        List<Identifier> arguments = new ArrayList<>();
        for (String arg : args) {
            arguments.add(new Identifier(null, arg));
        }

        return new FunctionExpression(null, nameIdentifier, body, arguments);
    }

    public static FunctionExpression function(BlockStatement body, String... args) {
        return function(null, body, args);
    }

    public static FunctionExpression function(Statement body, String... args) {
        return function(block(body), Arrays.asList(args));
    }

    public static FunctionExpression function(Statement body, List<String> args) {
        return function(null, block(body), args);
    }

    public static BlockStatement block(Statement... statements) {
        return block(Arrays.asList(statements));
    }

    public static BlockStatement block(Collection<Statement> statements) {
        return new BlockStatement(null, statements);
    }

    public static ExpressionStatement statement(Expression expression) {
        return expressionStatement(expression);
    }

    public static ExpressionStatement expressionStatement(Expression expression) {
        return new ExpressionStatement(null, expression);
    }

    public static CommentStatement comment(String comment) {
        return new CommentStatement(comment);
    }

    public static IfStatement ifThen(Expression condition, Statement ifBranch) {
        return new IfStatement(null, condition, ifBranch, null);
    }

    public static IfStatement ifThenElse(Expression condition, Statement ifBranch, Statement elseBranch) {
        return new IfStatement(null, condition, ifBranch, elseBranch);
    }

    public static UnaryExpression unary(Operator operator, Expression expression) {
        return new UnaryExpression(null, operator, expression);
    }

    public static final Predicate<String> testIdentifier = Pattern.compile("^[^\\d\\W]\\w*\\Z").asPredicate(); // Not complete, or sound.
    public static Expression member(Expression expression, String property) {
        if (testIdentifier.test(property)) {
            return new MemberExpression(null, property, expression);
        } else if (Util.isInteger(property)){
            return arrayAccess(expression, number(Integer.parseInt(property)));
        } else {
            return arrayAccess(expression, string(property));
        }
    }

    public static Return Return() {
        return new Return(null, null);
    }

    public static Return Return(Expression expression) {
        return new Return(null, expression);
    }

    public static MethodCallExpression methodCall(Expression expression, String property, Expression... args) {
        return methodCall(expression, property, Arrays.asList(args));
    }

    public static MethodCallExpression methodCall(Expression expression, String property, List<Expression> args) {
        return new MethodCallExpression(null, new MemberExpression(null, property, expression), args);
    }

    public static ArrayLiteral array(Expression... elements) {
        return array(Arrays.asList(elements));
    }

    public static ArrayLiteral array(List<? extends Expression> expressions) {
        return new ArrayLiteral(null, expressions);
    }

    public static ArrayLiteral array(int[] ints) {
        return new ArrayLiteral(null, Arrays.stream(ints).mapToObj(AstBuilder::number).collect(Collectors.toList()));
    }

    public static ForStatement forLoop(Statement initializer, Expression condition, Expression increment, Statement body) {
        return new ForStatement(null, initializer, condition, increment, body);
    }

    public static WhileStatement whileLoop(Expression condition, Statement body) {
        return new WhileStatement(null, condition, body);
    }

    public static NumberLiteral number(double number) {
        return new NumberLiteral(null, number);
    }

    public static BinaryExpression binary(Expression left, Operator op, Expression right) {
        return new BinaryExpression(null, left, right, op);
    }

    public static TryStatement tryCatch(Statement tryBlock, CatchStatement catchBlock) {
        return tryCatchFinally(tryBlock, catchBlock, null);
    }

    public static TryStatement tryCatchFinally(Statement tryBlock, CatchStatement catchBlock, Statement finallyBlock) {
        return new TryStatement(null, tryBlock, catchBlock, finallyBlock);
    }

    public static CatchStatement catchBlock(Identifier id, Statement body) {
        return new CatchStatement(null, id, body);
    }

    public static CatchStatement catchBlock(String id, Statement body) {
        return new CatchStatement(null, identifier(id), body);
    }

    public static SwitchStatement switchCase(Expression expression, List<Pair<Expression, Statement>> cases) {
        return switchCase(expression, cases, null);
    }

    public static SwitchStatement switchCase(Expression expression, List<Pair<Expression, Statement>> cases, BlockStatement defaultCase) {
        return new SwitchStatement(null, expression, cases, defaultCase);
    }

    public static BreakStatement breakStatement() {
        return new BreakStatement(null, null);
    }

    public static ContinueStatement continueStatement() {
        return new ContinueStatement(null);
    }

    public static BooleanLiteral bool(boolean b) {
        return new BooleanLiteral(null, b);
    }

    public static NullLiteral nullLiteral() {
        return new NullLiteral(null);
    }

    public static DynamicAccessExpression arrayAccess(Expression operand, Expression lookupKey) {
        return new DynamicAccessExpression(null, operand, lookupKey);
    }

    public static ObjectLiteral object(ObjectLiteral.Property... properties) {
        return object(Arrays.asList(properties));
    }

    public static ObjectLiteral object(List<ObjectLiteral.Property> properties) {
        return new ObjectLiteral(null, properties);
    }



    public static Expression and(List<Expression> expressions) {
        return binaryList(expressions, Operator.AND);
    }

    private static Expression binaryList(List<Expression> expressions, Operator operator) {
        if (expressions.isEmpty()) {
            throw new RuntimeException();
        }
        if (expressions.size() == 1) {
            return expressions.iterator().next();
        }
        return binary(expressions.iterator().next(), operator, binaryList(expressions.subList(1, expressions.size()), operator));
    }

    public static Expression and(Expression... expressions) {
        return and(Arrays.asList(expressions));
    }

    public static Expression or(List<Expression> expressions) {
        return binaryList(expressions, Operator.OR);
    }

    public static Expression or(Expression... expressions) {
        return or(Arrays.asList(expressions));
    }

    public static ThrowStatement throwStatement(Expression exp) {
        return new ThrowStatement(null, exp);
    }

    public static ConditionalExpression conditional(Expression cond, Expression then, Expression otherwise) {
        return new ConditionalExpression(null, cond, then, otherwise);
    }

    public static BlockStatement programFromFile(URL resource) throws IOException {
        return new JavaScriptParser(ParseDeclaration.Environment.ES5Core).parse("filename", Resources.toString(resource, Charsets.UTF_8)).toTSCreateAST().getBody();
    }

    public static BlockStatement stmtFromString(String program){
        // Packing and unpacking, to allow top-level return-statements.
        BlockStatement body = new JavaScriptParser(ParseDeclaration.Environment.ES5Core).parse("filename", "function foo() {\n" + program + "\n}").toTSCreateAST().getBody();
        return ((FunctionExpression)((ExpressionStatement)body.getStatements().iterator().next()).getExpression()).getBody();
    }

    public static Expression expFromString(String program){
        BlockStatement block = stmtFromString(program);
        assert block.getStatements().size() == 1;
        Statement statement = block.getStatements().iterator().next();
        return ((ExpressionStatement) statement).getExpression();
    }
}
