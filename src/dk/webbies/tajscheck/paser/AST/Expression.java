package dk.webbies.tajscheck.paser.AST;

import com.google.javascript.jscomp.parsing.parser.util.SourceRange;
import dk.webbies.tajscheck.paser.ExpressionVisitor;

/**
 * Created by Erik Krogh Kristensen on 01-09-2015.
 */
public abstract class Expression extends AstNode {
    Expression(SourceRange location) {
        super(location);
    }

    public abstract <T> T accept(ExpressionVisitor<T> visitor);
}
