package dk.webbies.tajscheck.paser;

import dk.webbies.tajscheck.paser.AST.*;

/**
 * Created by Erik Krogh Kristensen on 07-09-2015.
 *
 * Transverses all the Expressions and sub-Expressions (except the function, since it contains Statements).
 */
public interface ExpressionTransverse<T> extends ExpressionVisitor<T> {
    StatementVisitor<T> getStatementVisitor();

    @Override
    public default T visit(BinaryExpression expression) {
        expression.getLhs().accept(this);
        expression.getRhs().accept(this);
        return null;
    }

    @Override
    public default T visit(FunctionExpression function) {
        function.getBody().accept(getStatementVisitor());
        if (function.getName() != null) {
            function.getName().accept(this);
        }
        function.getArguments().forEach(arg -> arg.accept(this));
        return null;
    }

    @Override
    public default T visit(UnaryExpression unOp) {
        unOp.getExpression().accept(this);
        return null;
    }


    @Override
    public default T visit(MemberExpression memberExpression) {
        memberExpression.getExpression().accept(this);
        return null;
    }

    @Override
    public default T visit(StringLiteral string) {
        return null;
    }

    @Override
    public default T visit(Identifier identifier) {
        return null;
    }

    @Override
    public default T visit(BooleanLiteral booleanLiteral) {
        return null;
    }

    @Override
    public default T visit(UndefinedLiteral undefined) {
        return null;
    }

    @Override
    public default T visit(NumberLiteral number) {
        return null;
    }


    @Override
    public default T visit(NullLiteral nullLiteral) {
        return null;
    }

    @Override
    public default T visit(CallExpression callExpression) {
        callExpression.getFunction().accept(this);
        callExpression.getArgs().stream().forEach(arg -> arg.accept(this));
        return null;
    }

    @Override
    public default T visit(ObjectLiteral object) {
        for (ObjectLiteral.Property property : object.getProperties()) {
            property.expression.accept(this);
        }

        return null;
    }

    @Override
    public default T visit(MethodCallExpression methodCall) {
        methodCall.getMemberExpression().accept(this);
        methodCall.getArgs().forEach(arg -> arg.accept(this));
        return null;
    }

    @Override
    public default T visit(ThisExpression thisExpression) {
        return null;
    }

    @Override
    public default T visit(NewExpression newExp) {
        newExp.getOperand().accept(this);
        newExp.getArgs().forEach(arg -> arg.accept(this));
        return null;
    }

    @Override
    public default T visit(ConditionalExpression cond) {
        cond.getCondition().accept(this);
        cond.getLeft().accept(this);
        cond.getRight().accept(this);
        return null;
    }

    @Override
    public default T visit(DynamicAccessExpression memberLookup) {
        memberLookup.getOperand().accept(this);
        memberLookup.getLookupKey().accept(this);
        return null;
    }

    @Override
    public default T visit(CommaExpression commaExpression) {
        commaExpression.getExpressions().forEach(exp -> exp.accept(this));
        return null;
    }

    @Override
    public default T visit(GetterExpression getter) {
        getter.asFunction().accept(this);
        return null;
    }

    @Override
    public default T visit(SetterExpression setter) {
        setter.asFunction().accept(this);
        return null;
    }

    @Override
    public default T visit(ArrayLiteral arrayLiteral) {
        arrayLiteral.getExpressions().forEach(arr -> arr.accept(this));
        return null;
    }

    @Override
    public default T visit(RegExpExpression regExp) {
        return null;
    }
}
