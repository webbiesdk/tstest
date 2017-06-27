package dk.webbies.tajscheck.paser.AST;

import dk.webbies.tajscheck.paser.ExpressionTransverse;
import dk.webbies.tajscheck.paser.ExpressionVisitor;
import dk.webbies.tajscheck.paser.StatementTransverse;
import dk.webbies.tajscheck.paser.StatementVisitor;

/**
 * A default implementation of NodeVisitor, that visits everything, but returns null for everything.
 */

public interface NodeTransverse<T> extends StatementTransverse<T>, ExpressionTransverse<T> {
    @Override
    public default ExpressionVisitor<T> getExpressionVisitor() {
        return this;
    }

    @Override
    public default StatementVisitor<T> getStatementVisitor() {
        return this;
    }
}
