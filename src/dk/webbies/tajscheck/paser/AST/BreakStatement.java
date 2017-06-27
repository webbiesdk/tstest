package dk.webbies.tajscheck.paser.AST;

import com.google.javascript.jscomp.parsing.parser.util.SourceRange;
import dk.webbies.tajscheck.paser.StatementVisitor;

/**
 * Created by Erik Krogh Kristensen on 07-09-2015.
 */
public class BreakStatement extends Statement {
    private String label;

    public BreakStatement(SourceRange loc, String label) {
        super(loc);
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public <T> T accept(StatementVisitor<T> visitor) {
        return visitor.visit(this);
    }

}
