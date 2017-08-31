package dk.webbies.tajscheck.typeutil;

import dk.au.cs.casa.typescript.types.*;
import dk.webbies.tajscheck.util.Util;

import java.util.*;

/**
 * Created by erik1 on 01-11-2016.
 */
public class PrettyTypes {
    public static String parametersTypes(List<Type> parameters, boolean isRestArgs) {
        StringBuilder builder = new StringBuilder();

        builder.append("(");
        parameters = new ArrayList<>(parameters);

        Type restArgsType = isRestArgs ? TypesUtil.extractRestArgsType(parameters) : null;
        if (isRestArgs) {
            parameters.remove(parameters.size() - 1);
        }

        for (int i = 0; i < parameters.size(); i++) {
            Type type = parameters.get(i);
            builder.append(type(type));

            if (i != parameters.size() - 1) {
                builder.append(",");
            }
        }

        if (isRestArgs) {
            builder.append(",...").append(type(restArgsType));
        }

        builder.append(")");

        return builder.toString();
    }

    public static String parameters(List<Signature.Parameter> parameters) {
        StringBuilder builder = new StringBuilder();

        builder.append("(");
        for (int i = 0; i < parameters.size(); i++) {
            Type type = parameters.get(i).getType();
            String name = parameters.get(i).getName();
            builder.append(name).append(": ");
            builder.append(type(type));

            if (i != parameters.size() - 1) {
                builder.append(", ");
            }
        }

        builder.append(")");

        return builder.toString();
    }

    public static String type(Type type) {
        if (type instanceof SimpleType) {
            switch (((SimpleType) type).getKind()) {
                case Any:
                    return "any";
                case Boolean:
                    return "boolean";
                case Enum:
                    return "enum";
                case Null:
                    return "null";
                case Number:
                    return "number";
                case String:
                    return "string";
                case Undefined:
                    return "undefined";
                case Void:
                    return "void";
                case Never:
                    return "never";
                case Symbol:
                    return "symbol";
                default:
                    throw new RuntimeException("what? " + ((SimpleType) type).getKind().name());
            }
        } else if (type instanceof NumberLiteral) {
            return Util.toPrettyNumber(((NumberLiteral) type).getValue());
        } else if (type instanceof BooleanLiteral) {
            return Boolean.toString(((BooleanLiteral) type).getValue());
        } else if (type instanceof StringLiteral) {
            return "\"" + ((StringLiteral) type).getText() + "\"";
        } else if (type instanceof InterfaceType || type instanceof GenericType || type instanceof ReferenceType) {
            return "obj";
        } else if (type instanceof TypeParameterType) {
            return "typeParameter";
        } else if (type instanceof UnionType) {
            return "union";
        } else if (type instanceof IntersectionType) {
            return "intersection";
        } else if (type instanceof ClassInstanceType) {
            return "classInstance";
        } else if (type instanceof ClassType) {
            return "class";
        } else if (type instanceof ThisType) {
            return "this";
        }

        throw new RuntimeException(type.getClass().getName());
    }
}
