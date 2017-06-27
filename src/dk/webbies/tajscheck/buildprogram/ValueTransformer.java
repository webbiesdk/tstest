package dk.webbies.tajscheck.buildprogram;

import dk.au.cs.casa.typescript.types.Type;
import dk.webbies.tajscheck.paser.AST.Expression;
import dk.webbies.tajscheck.paser.AST.Statement;
import dk.webbies.tajscheck.typeutil.typeContext.TypeContext;

import java.util.List;

interface ValueTransformer {

    ValueTransformer identityTransformer = (x, y) -> x;

    Expression transform(Expression value, Type type);
}
