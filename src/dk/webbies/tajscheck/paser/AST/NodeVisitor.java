package dk.webbies.tajscheck.paser.AST;

import dk.webbies.tajscheck.paser.ExpressionVisitor;
import dk.webbies.tajscheck.paser.StatementVisitor;

/**
 * Created by Erik Krogh Kristensen on 01-09-2015.
 */
public interface NodeVisitor<T> extends StatementVisitor<T>, ExpressionVisitor<T> {

}
