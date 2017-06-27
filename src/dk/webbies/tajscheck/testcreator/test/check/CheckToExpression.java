package dk.webbies.tajscheck.testcreator.test.check;

import dk.webbies.tajscheck.paser.AST.Expression;
import dk.webbies.tajscheck.paser.AST.Operator;
import dk.webbies.tajscheck.paser.AstBuilder;

import java.util.stream.Collectors;

import static dk.webbies.tajscheck.paser.AstBuilder.*;

/**
 * Created by erik1 on 14-11-2016.
 */
public class CheckToExpression implements CheckVisitorWithArgument<Expression, Expression> {
    private CheckToExpression() {
    }

    @Override
    public Expression visit(OrCheck check, Expression expression) {
        return AstBuilder.or(
                check.getChecks()
                        .stream()
                        .map(subFilter -> subFilter.accept(this, expression))
                        .collect(Collectors.toList())
        );
    }

    @Override
    public Expression visit(TypeOfCheck check, Expression expression) {
        return binary(
                unary(Operator.TYPEOF, expression),
                Operator.EQUAL_EQUAL_EQUAL,
                string(check.getTypeString())
        );
    }

    @Override
    public Expression visit(NotCheck check, Expression expression) {
        if (check.getCheck() instanceof NotCheck) {
            return ((NotCheck) check.getCheck()).getCheck().accept(this, expression);
        } else {
            return unary(Operator.NOT, check.getCheck().accept(this, expression));
        }
    }

    @Override
    public Expression visit(AndCheck check, Expression expression) {
        return AstBuilder.and(check.getChecks().stream().map(subFilter -> subFilter.accept(this, expression)).collect(Collectors.toList()));
    }

    @Override
    public Expression visit(EqualityCheck check, Expression expression) {
        return binary(
                check.getExpression(),
                Operator.EQUAL_EQUAL_EQUAL,
                expression
        );
    }

    @Override
    public Expression visit(InstanceOfCheck check, Expression expression) {
        return binary(
                expression,
                Operator.INSTANCEOF,
                check.getExp()
        );
    }

    @Override
    public Expression visit(FieldCheck check, Expression expression) {
        return Check.and(check.getChecks()).accept(this, member(expression, check.getField()));
    }

    @Override
    public Expression visit(NumberIndexCheck check, Expression expression) {
        return call(identifier("numberIndexCheck"), expression, function(block(
                Return(check.getSubCheck().accept(this, identifier("exp")))
        ), "exp"));
    }

    @Override
    public Expression visit(StringIndexCheck check, Expression expression) {
        return call(identifier("stringIndexCheck"), expression, function(block(
                Return(check.getSubCheck().accept(this, identifier("exp")))
        ), "exp"));
    }

    @Override
    public Expression visit(ExpressionCheck check, Expression expression) {
        return check.getGenerator().apply(expression);
    }

    public static Expression generate(Check check, Expression exp) {
        return check.accept(new CheckToExpression(), exp);
    }
}
