package dk.webbies.tajscheck.paser.AST;

import com.google.javascript.jscomp.parsing.parser.util.SourceRange;
import dk.webbies.tajscheck.paser.ExpressionVisitor;

import java.util.List;

/**
 * Created by erik1 on 22-01-2016.
 */
public class ArrayLiteral extends Expression {
    private final List<? extends Expression> expressions;

    public ArrayLiteral(SourceRange location, List<? extends Expression> expression) {
        super(location);
        this.expressions = expression;
    }

    public List<? extends Expression> getExpressions() {
        return expressions;
    }

    @Override
    public <T> T accept(ExpressionVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
