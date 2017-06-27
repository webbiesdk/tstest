package dk.webbies.tajscheck.paser.AST;

import com.google.javascript.jscomp.parsing.parser.util.SourceRange;
import dk.webbies.tajscheck.paser.ExpressionVisitor;

import java.util.List;

/**
 * Created by Erik Krogh Kristensen on 04-09-2015.
 */
public class CallExpression extends Expression {
    private final Expression function;
    private final List<? extends Expression> args;

    public CallExpression(SourceRange location, Expression function, List<? extends Expression> args) {
        super(location);
        this.function = function;
        this.args = args;
    }

    public Expression getFunction() {
        return function;
    }

    public List<? extends Expression> getArgs() {
        return args;
    }

    @Override
    public <T> T accept(ExpressionVisitor<T> visitor) {
        return visitor.visit(this);
    }

}
