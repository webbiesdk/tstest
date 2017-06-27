package dk.webbies.tajscheck.paser.AST;

import com.google.javascript.jscomp.parsing.parser.util.SourceRange;
import dk.webbies.tajscheck.paser.ExpressionVisitor;

/**
 * Created by erik1 on 01-09-2015.
 */
public class BooleanLiteral extends Expression {
    private final boolean b;

    public BooleanLiteral(SourceRange location, boolean b) {
        super(location);
        this.b = b;
    }

    public boolean getBooleanValue() {
        return b;
    }

    @Override
    public <T> T accept(ExpressionVisitor<T> visitor) {
        return visitor.visit(this);
    }

}
