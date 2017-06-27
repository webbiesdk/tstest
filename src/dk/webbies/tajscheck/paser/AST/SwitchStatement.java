package dk.webbies.tajscheck.paser.AST;

import com.google.javascript.jscomp.parsing.parser.util.SourceRange;
import dk.webbies.tajscheck.paser.StatementVisitor;
import dk.webbies.tajscheck.util.Pair;

import java.util.List;

/**
 * Created by Erik Krogh Kristensen on 07-09-2015.
 */
public class SwitchStatement extends Statement {
    private final Expression expression;
    private final List<Pair<Expression, Statement>> cases;
    private final BlockStatement defaultCase;

    public SwitchStatement(SourceRange loc, Expression expression, List<Pair<Expression, Statement>> cases, BlockStatement defaultCase) {
        super(loc);
        this.expression = expression;
        this.cases = cases;
        this.defaultCase = defaultCase;
    }

    public Expression getExpression() {
        return expression;
    }

    public List<Pair<Expression, Statement>> getCases() {
        return cases;
    }

    public BlockStatement getDefaultCase() {
        return defaultCase;
    }

    @Override
    public <T> T accept(StatementVisitor<T> visitor) {
        return visitor.visit(this);
    }

}
