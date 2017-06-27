package dk.webbies.tajscheck.paser;

import dk.webbies.tajscheck.paser.AST.*;

/**
 * Created by Erik Krogh Kristensen on 07-09-2015.
 */
public interface StatementVisitor<T> {
    T visit(BlockStatement block);

    T visit(BreakStatement breakStatement);

    T visit(ContinueStatement continueStatement);

    T visit(ExpressionStatement expressionStatement);

    T visit(ForStatement forStatement);

    T visit(IfStatement ifStatement);

    T visit(Return aReturn);

    T visit(SwitchStatement switchStatement);

    T visit(ThrowStatement throwStatement);

    T visit(VariableNode variableNode);

    T visit(WhileStatement whileStatement);

    T visit(ForInStatement forinStatement);

    T visit(TryStatement tryStatement);

    T visit(CatchStatement catchStatement);

    T visit(LabeledStatement labeledStatement);

    T visit(CommentStatement commentStatement);

    T visit(DoWhileStatement doWhileStatement);
}
