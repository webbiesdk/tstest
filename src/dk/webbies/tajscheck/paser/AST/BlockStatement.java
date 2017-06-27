package dk.webbies.tajscheck.paser.AST;

import com.google.javascript.jscomp.parsing.parser.util.SourceRange;
import dk.webbies.tajscheck.paser.StatementVisitor;

import java.util.Collection;

/**
 * Created by erik1 on 01-09-2015.
 */
public class BlockStatement extends Statement{
    private final Collection<Statement> statements;

    public BlockStatement(SourceRange location, Collection<Statement> statements) {
        super(location);
        this.statements = statements;
    }

    @Override
    public <T> T accept(StatementVisitor<T> visitor) {
        return visitor.visit(this);
    }

    public Collection<Statement> getStatements() {
        return statements;
    }
}
