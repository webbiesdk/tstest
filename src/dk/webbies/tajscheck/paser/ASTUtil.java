package dk.webbies.tajscheck.paser;

import dk.webbies.tajscheck.paser.AST.Expression;
import dk.webbies.tajscheck.paser.AST.Operator;

import java.util.Arrays;
import java.util.List;

import static dk.webbies.tajscheck.paser.AstBuilder.*;

/**
 * Created by erik1 on 04-11-2016.
 */
public class ASTUtil {
    public static Expression and(List<Expression> expressions) {
        return binaryList(expressions, Operator.AND);
    }

    private static Expression binaryList(List<Expression> expressions, Operator operator) {
        if (expressions.isEmpty()) {
            throw new RuntimeException();
        }
        if (expressions.size() == 1) {
            return expressions.iterator().next();
        }
        return binary(expressions.iterator().next(), operator, binaryList(expressions.subList(1, expressions.size()), operator));
    }

    public static Expression and(Expression... expressions) {
        return and(Arrays.asList(expressions));
    }

    public static Expression or(List<Expression> expressions) {
        return binaryList(expressions, Operator.OR);
    }

    public static Expression or(Expression... expressions) {
        return or(Arrays.asList(expressions));
    }
}
