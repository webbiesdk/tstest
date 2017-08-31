package dk.webbies.tajscheck.test.experiments;

import dk.au.cs.casa.typescript.SpecReader;
import dk.au.cs.casa.typescript.types.*;
import dk.webbies.tajscheck.OutputParser;
import dk.webbies.tajscheck.TypeWithContext;
import dk.webbies.tajscheck.benchmark.Benchmark;
import dk.webbies.tajscheck.benchmark.BenchmarkInfo;
import dk.webbies.tajscheck.typeutil.TypesUtil;
import dk.webbies.tajscheck.typeutil.typeContext.TypeContext;
import dk.webbies.tajscheck.util.ArrayListMultiMap;
import dk.webbies.tajscheck.util.MultiMap;
import dk.webbies.tajscheck.util.Pair;
import dk.webbies.tajscheck.util.Util;

import java.util.*;

/**
 * Created by erik1 on 28-03-2017.
 */
public class CountUniques {
    public static int uniqueWarnings(Collection<OutputParser.TypeError> errors, Benchmark benchmark) {
        return groupWarnings(errors, benchmark).size();
    }

    public static MultiMap<Pair<Type, String>, OutputParser.TypeError> groupWarnings(Collection<OutputParser.TypeError> errors, Benchmark benchmark) {
        BenchmarkInfo info = BenchmarkInfo.create(benchmark.withOptions(options -> options.setDisableGenerics(false)));

        MultiMap<Pair<Type, String>, OutputParser.TypeError> seenWarnings = new ArrayListMultiMap<>();

        errors.forEach(te -> {
            String path = te.getPath();
            if (!path.contains(".")) {
                seenWarnings.put(new Pair<>(lookupType(path, info), null), te);
                return;
            }
            String prop = path.substring(path.lastIndexOf(".") + 1, path.length());
            path = path.substring(0, path.lastIndexOf("."));

            seenWarnings.put(new Pair<>(lookupType(path, info), prop), te);
        });
        return seenWarnings;
    }

    private static String removeArguments(String path) {
        int index = 0;
        while (((index = path.indexOf("(", index + 1)) != -1) && path.indexOf(")", index + 1) != -1) {
            try {
                path = path.substring(0, index) + "()" + path.substring(path.indexOf(")", index + 1) + 1, path.length());
            } catch (StringIndexOutOfBoundsException e) {
                e.printStackTrace();
            }
        }
        return path;
    }

    public static void main(String[] args) {
        lookupType("Backbone.$.noConflict()(obj)", null);
    }

    private static Type lookupType(String path, BenchmarkInfo info) {
        path = removeArguments(path);

        if (path.startsWith("window.")) {
            path = Util.removePrefix(path, "window.");
        }

        path = path.replace("()", ".()");
        path = path.replace("new.()", "new()");

        List<String> pathList = new ArrayList<>();

        if (path.contains(".")) {
            for (String pathPart : Arrays.asList(path.split("\\."))) {
                if (pathPart.endsWith("()")) {
                    if (pathPart.startsWith("new")) {
                        pathList.add(pathPart);
                    } else {
                        pathList.add(pathPart.substring(0, pathPart.length() - 2));
                        pathList.add("()");
                    }
                } else {
                    pathList.add(pathPart);
                }
            }

            path = String.join(".", pathList);
        } else if (path.endsWith("()")) {
            path = path.substring(0, path.length() - 2) + ".()";
        }
        while (path.contains("..")) {
            path = path.replace("..", ".");
        }

        SpecReader spec = info.getSpec();

        Type result = new LookupTypeVisitor(info).recurse(spec.getGlobal(), new Arg(path, TypeContext.create(info)));
        if (result == null) {
            for (SpecReader.NamedType namedType : spec.getNamedTypes()) {
                String name = String.join(".", namedType.qName);
                if (path.startsWith(name)) {
                    String subPath = Util.removePrefix(path, name);
                    while (subPath.startsWith(".")) {
                        subPath = subPath.substring(1, subPath.length());
                    }
                    result = new LookupTypeVisitor(info).recurse(namedType.type, new Arg(subPath, TypeContext.create(info)));
                    if (result != null) {
                        return result;
                    }
                }
            }
        }
        return result;
    }

    private static final class Arg {
        private final String path;
        private final TypeContext context;

        public Arg(String path, TypeContext context) {
            this.path = path;
            this.context = context;
        }

        public Arg withPath(String path) {
            return new Arg(path, this.context);
        }

        public Arg withContext(TypeContext context) {
            return new Arg(this.path, context);
        }

        public Arg rest() {
            return withPath(CountUniques.rest(this.path));
        }
    }

    static String firstPath(String path) {
        if (!path.contains(".")) {
            return path;
        } else {
            return path.substring(0, path.indexOf("."));
        }
    }

    static String rest(String path) {
        if (path.contains(".")) {
            return path.substring(path.indexOf(".") + 1, path.length());
        } else {
            return "";
        }
    }

    private static class LookupTypeVisitor implements TypeVisitorWithArgument<Type, Arg> {
        private final BenchmarkInfo info;

        private LookupTypeVisitor(BenchmarkInfo info) {
            this.info = info;
        }

        @Override
        public Type visit(AnonymousType t, Arg arg) {
            throw new RuntimeException();
        }

        @Override
        public Type visit(ClassType t, Arg arg) {
            if (info.freeGenericsFinder.hasThisTypes(t)) {
                arg = arg.withContext(arg.context.withThisType(info.typesUtil.createClassInstanceType(t)));
            }
            if (firstPath(arg.path).equals("new()")) {
                return recurse(t.getInstance(), arg.rest());
            }
            if (firstPath(arg.path).equals("[base]")) {
                for (Type base : t.getBaseTypes()) {
                    Type result = recurse(base, arg.rest());
                    if (result != null) {
                        return result;
                    }
                }
                return null;
            }
            if (firstPath(arg.path).startsWith("[static]")) {
                return recurse(t.getStaticProperties().get(Util.removePrefix(firstPath(arg.path), "[static]")), arg.rest());
            }
            if (firstPath(arg.path).startsWith("[arg")) {
                int argNumber = Integer.parseInt(firstPath(arg.path).substring("[arg".length(), firstPath(arg.path).length() - 1));
                for (Signature signature : t.getSignatures()) {
                    if (signature.getParameters().size() > argNumber) {
                        Type result = recurse(signature.getParameters().get(argNumber).getType(), arg.rest());
                        if (result != null) {
                            return result;
                        }
                    }
                }
                return null;

            }
            if (firstPath(arg.path).contains("(") || firstPath(arg.path).contains("[") || firstPath(arg.path).contains("<")) {
                return null;
            }
            Type result = recurse(t.getStaticProperties().get(firstPath(arg.path)), arg.rest());
            if (result != null) {
                return result;
            }
            return recurse(t.getInstanceProperties().get(firstPath(arg.path)), arg.rest());
        }

        @Override
        public Type visit(GenericType t, Arg arg) {
            return recurse(t.toInterface(), arg);
        }

        @Override
        public Type visit(InterfaceType t, Arg arg) {
            if (info.freeGenericsFinder.hasThisTypes(t)) {
                arg = arg.withContext(arg.context.withThisType(t));
            }
            for (Type base : t.getBaseTypes()) {
                Type result = recurse(base, arg);
                if (result != null) {
                    return result;
                }
            }

            if (firstPath(arg.path).startsWith("[")) {
                if (firstPath(arg.path).startsWith("[arg")) {
                    int argNumber = Integer.parseInt(firstPath(arg.path).substring("[arg".length(), firstPath(arg.path).length() - 1));
                    for (Signature signature : Util.concat(t.getDeclaredCallSignatures(), t.getDeclaredConstructSignatures())) {
                        if (signature.getParameters().size() > argNumber) {
                            Type result = recurse(signature.getParameters().get(argNumber).getType(), arg.rest());
                            if (result != null) {
                                return result;
                            }
                        }
                    }
                    return null;

                }
                if (firstPath(arg.path).startsWith("[union")) {
                    return recurse(t, arg.rest());
                }
                if (firstPath(arg.path).equals("[base]")) {
                    return recurse(t, arg.rest());
                }
                if (firstPath(arg.path).startsWith("[typeArg")) {
                    int index = Integer.parseInt(firstPath(arg.path).substring("[typeArg".length(), firstPath(arg.path).length() - 1));
                    if (t.getTypeParameters().size() > index) {
                        return recurse(t.getTypeParameters().get(index), arg.rest());
                    } else {
                        return null;
                    }
                }
                if (firstPath(arg.path).equals("[numberIndexer]")) {
                    return recurse(t.getDeclaredNumberIndexType(), arg.rest());
                }
                if (firstPath(arg.path).equals("[stringIndexer]")) {
                    return recurse(t.getDeclaredStringIndexType(), arg.rest());
                }
                if (firstPath(arg.path).startsWith("[intersection") || firstPath(arg.path).equals("[constraint]")) {
                    return null;
                }
                throw new RuntimeException(firstPath(arg.path));
            }
            if (firstPath(arg.path).startsWith("(")) {
                for (Signature signature : Util.concat(t.getDeclaredCallSignatures(), t.getDeclaredConstructSignatures())) {
                    Type result = recurse(signature.getResolvedReturnType(), arg.rest());
                    if (result != null) {
                        return result;
                    }
                }
                return null;
            }
            if (firstPath(arg.path).equals("new()")) {
                for (Signature signature : t.getDeclaredConstructSignatures()) {
                    Type result = recurse(signature.getResolvedReturnType(), arg.rest());
                    if (result != null) {
                        return result;
                    }
                }
                return null;
            }
            if (firstPath(arg.path).contains("(") || firstPath(arg.path).contains("<") || firstPath(arg.path).contains("[")) {
                return null;
            }
            return recurse(t.getDeclaredProperties().get(firstPath(arg.path)), arg.rest());
        }

        private Type recurse(Type type, Arg arg) {
            if (type == null) {
                return null;
            }
            while (firstPath(arg.path).startsWith("<")) {
                if (arg.path.equals("<>")) {
                    arg = arg.withPath("");
                }
                arg = arg.withPath(arg.path.substring(arg.path.indexOf(".") + 1, arg.path.length()));
            }
            if (arg.path.isEmpty() && !(type instanceof TypeParameterType || type instanceof GenericType || type instanceof ReferenceType)) {
                return type;
            }
            return type.accept(this, arg);
        }

        @Override
        public Type visit(ReferenceType t, Arg arg) {
            return recurse(t.getTarget(), arg.withContext(info.typesUtil.generateParameterMap(t, arg.context)));
        }

        @Override
        public Type visit(SimpleType t, Arg arg) {
            return null;
        }

        @Override
        public Type visit(TupleType t, Arg arg) {
            if (firstPath(arg.path).startsWith("[typeArg")) {
                int index = Integer.parseInt(firstPath(arg.path).substring("[typeArg".length(), firstPath(arg.path).length() - 1));
                if (t.getElementTypes().size() > index) {
                    return recurse(t.getElementTypes().get(index), arg.rest());
                } else {
                    return null;
                }
            } else {
                return null;
            }
        }

        @Override
        public Type visit(UnionType t, Arg arg) {
            if (!arg.path.startsWith("[union")) {
                for (Type type : t.getElements()) {
                    Type result = recurse(type, arg);
                    if (result != null) {
                        return result;
                    }
                }
                return null;
            }
            int index = Integer.parseInt(firstPath(arg.path).substring("[union".length(), firstPath(arg.path).length() - 1));
            if (t.getElements().size() > index) {
                return recurse(t.getElements().get(index), arg.rest());
            } else {
                return null;
            }
        }

        @Override
        public Type visit(TypeParameterType t, Arg arg) {
            TypeWithContext lookup = arg.context.get(t);
            if (lookup != null) {
                return recurse(lookup.getType(), arg.withContext(lookup.getTypeContext()));
            } else {
                return t;
            }
        }

        @Override
        public Type visit(StringLiteral t, Arg arg) {
            return null;
        }

        @Override
        public Type visit(BooleanLiteral t, Arg arg) {
            return null;
        }

        @Override
        public Type visit(NumberLiteral t, Arg arg) {
            return null;
        }

        @Override
        public Type visit(IntersectionType t, Arg arg) {
            if (!arg.path.startsWith("[intersection")) {
                for (Type type : t.getElements()) {
                    Type result = recurse(type, arg);
                    if (result != null) {
                        return result;
                    }
                }
                return null;
            }
            int index = Integer.parseInt(firstPath(arg.path).substring("[intersection".length(), firstPath(arg.path).length() - 1));
            if (t.getElements().size() > index) {
                return recurse(t.getElements().get(index), arg.rest());
            } else {
                return null;
            }
        }

        @Override
        public Type visit(ClassInstanceType t, Arg arg) {
            return recurse(info.typesUtil.createClassInstanceType(((ClassType) t.getClassType())), arg);
        }

        @Override
        public Type visit(ThisType t, Arg arg) {
            if (arg.context.getThisType() == null) {
                System.out.println();
                throw new RuntimeException();
            } else {
                return recurse(arg.context.getThisType(), arg);
            }
        }

        @Override
        public Type visit(IndexType t, Arg arg) {
            return null;
        }

        @Override
        public Type visit(IndexedAccessType t, Arg arg) {
            return null;
        }
    }
}
