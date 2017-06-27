package dk.webbies.tajscheck.paser.AST;

import com.google.javascript.jscomp.parsing.parser.util.SourcePosition;
import com.google.javascript.jscomp.parsing.parser.util.SourceRange;
import dk.webbies.tajscheck.paser.AstToStringVisitor;

/**
 * Created by erik1 on 01-09-2015.
 */
public abstract class AstNode {
    public final SourceRange location;
    AstNode(SourceRange location) {
        this.location = location;
    }

    @Override
    public String toString() {
        if (location == null) {
            if (this instanceof Expression) {
                return AstToStringVisitor.toString((Expression)this);
            } else {
                return AstToStringVisitor.toString((Statement)this);
            }
        }
        SourcePosition start = this.location.start;
        SourcePosition end = this.location.end;
        String contents = start.source.contents;
        return contents.substring(start.offset, end.offset);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AstNode astNode = (AstNode) o;

        if (location == null || astNode.location == null) {
            return this.toString().equals(astNode.toString());
        }

        return location.equals(astNode.location);

    }

    @Override
    public int hashCode() {
        if (location != null) {
            return location.hashCode();
        } else {
            return this.toString().hashCode();
        }
    }
}
