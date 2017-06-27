package dk.webbies.tajscheck.parsespec;

import dk.au.cs.casa.typescript.SpecReader;
import dk.au.cs.casa.typescript.types.*;
import dk.webbies.tajscheck.util.Util;

import java.io.File;
import java.util.*;

/**
 * Created by erik1 on 01-11-2016.
 */
public class ParseDeclaration {
    public static SpecReader getTypeSpecification(Environment env, Collection<String> declarationFilePaths) {
        String runString = "ts-spec-reader/src/CLI.js --env " + env.getCliArgument();
        for (String declarationFile : declarationFilePaths) {
            runString += " \"" + declarationFile + "\"";
        }

        String cachePath = "declaration-" + env.getCliArgument() + "-" + runString.hashCode() + ".json";

        List<File> toCheckAgainst = new ArrayList<>(Arrays.asList(new File("ts-spec-reader")));
        declarationFilePaths.stream().map(File::new).forEach(toCheckAgainst::add);

        String specification;
        try {
            specification = Util.getCachedOrRunNode(cachePath, toCheckAgainst, runString);
            return new SpecReader(specification.split("\\n")[specification.split("\\n").length - 1]);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void markNamedTypes(List<SpecReader.NamedType> namedTypes, Map<Type, String> typeNames) {
        for (SpecReader.NamedType namedType : namedTypes) {
            StringBuilder name = new StringBuilder();
            for (int i = 0; i < namedType.qName.size(); i++) {
                name.append(namedType.qName.get(i));
                if (i + 1 < namedType.qName.size()) {
                    name.append(".");
                }
            }
            typeNames.put(namedType.type, name.toString());
        }
    }

    public enum Environment {
        ES5Core(5, false),
        ES5DOM(5, true),
        ES6Core(6, false),
        ES6DOM(6, true);

        public final int ESversion;
        public final boolean hasDOM;

        Environment(int ESversion, boolean hasDOM) {
            this.ESversion = ESversion;
            this.hasDOM = hasDOM;
        }

        public String getCliArgument() {
            return "es" + ESversion + (this.hasDOM ? "-dom" : "");
        }
    }

    public static Map<Type, String> getTypeNamesMap(SpecReader spec) {
        Map<Type, String> typeNames = new HashMap<>();
        markNamedTypes(spec.getNamedTypes(), typeNames);

        PriorityQueue<Arg> queue = new PriorityQueue<>(Comparator.comparingInt(Arg::getDepth));
        queue.add(new Arg("window", 0, spec.getGlobal()));

        for (SpecReader.NamedType ambient : spec.getAmbientTypes()) {
            assert ambient.qName.size() == 1;
            queue.add(new Arg("\"" + ambient.qName.get(0) + "\"", 1, ambient.type));
        }


        Set<Type> seen = new HashSet<>();

        while (!queue.isEmpty()) {
            Arg arg = queue.poll();
            arg.type.accept(new NameAllTypesVisitor(queue, seen, typeNames), arg);
        }

        return typeNames;
    }

    private static final class Arg {
        final int depth;
        final String path;
        final Type type;

        private Arg(String path, int depth, Type type) {
            this.path = path;
            this.depth = depth;
            this.type = type;
        }

        public int getDepth() {
            return depth;
        }

        public Arg append(String key, Type newType) {
            return new Arg(this.path + "." + key, depth + 1, newType);
        }

        public Arg extraDepth(int depth) {
            return new Arg(this.path, this.depth + depth, this.type);
        }

    }

    private static class NameAllTypesVisitor implements TypeVisitorWithArgument<Void, Arg> {
        private final PriorityQueue<Arg> queue;
        private final Set<Type> seen;
        private Map<Type, String> typeNames;

        public NameAllTypesVisitor(PriorityQueue<Arg> queue, Set<Type> seen, Map<Type, String> typeNames) {
            this.queue = queue;
            this.seen = seen;
            this.typeNames = typeNames;
        }

        @Override
        public Void visit(AnonymousType t, Arg arg) {
            return null;
        }

        @Override
        public Void visit(ClassType t, Arg arg) {
            if (seen.contains(t)) {
                return null;
            }
            seen.add(t);
            addName(t, arg.path);

            for (Signature signature : t.getSignatures()) {
                for (int i = 0; i < signature.getParameters().size(); i++) {
                    queue.add(arg.append("[arg" + i + "]", signature.getParameters().get(i).getType()));
                }
            }
            for (Type baseType : t.getBaseTypes()) {
                queue.add(arg.append("[base]", baseType));
            }
            for (Map.Entry<String, Type> entry : t.getInstanceProperties().entrySet()) {
                queue.add(arg.append(entry.getKey(), entry.getValue()));
            }

            for (Map.Entry<String, Type> entry : t.getStaticProperties().entrySet()) {
                queue.add(arg.append("[static]" + entry.getKey(), entry.getValue()));
            }
            if (t.getDeclaredNumberIndexType() != null) {
                queue.add(arg.append("[numberIndexer]", t.getDeclaredNumberIndexType()));
            }
            if (t.getDeclaredStringIndexType() != null) {
                queue.add(arg.append("[stringIndexer]", t.getDeclaredStringIndexType()));
            }

            return null;
        }

        @Override
        public Void visit(GenericType t, Arg arg) {
            if (seen.contains(t)) {
                return null;
            }
            seen.add(t);
            addName(t, arg.path);

            return t.toInterface().accept(this, arg);
        }

        @Override
        public Void visit(InterfaceType t, Arg arg) {
            if (seen.contains(t)) {
                return null;
            }
            seen.add(t);
            addName(t, arg.path);

            for (Type baseType : t.getBaseTypes()) {
                queue.add(arg.append("[base]", baseType));
            }

            for (Signature signature : Util.concat(t.getDeclaredCallSignatures(), t.getDeclaredConstructSignatures())) {
                for (int i = 0; i < signature.getParameters().size(); i++) {
                    queue.add(arg.append("[arg" + i + "]", signature.getParameters().get(i).getType()));
                }
            }
            for (Signature signature : t.getDeclaredCallSignatures()) {
                queue.add(arg.append("()", signature.getResolvedReturnType()));
            }
            for (Signature signature : t.getDeclaredConstructSignatures()) {
                queue.add(arg.append("new()", signature.getResolvedReturnType()));
            }


            if (t.getDeclaredNumberIndexType() != null) {
                queue.add(arg.append("[numberIndexer]", t.getDeclaredNumberIndexType()));
            }
            if (t.getDeclaredStringIndexType() != null) {
                queue.add(arg.append("[stringIndexer]", t.getDeclaredStringIndexType()));
            }

            for (Map.Entry<String, Type> entry : t.getDeclaredProperties().entrySet()) {
                queue.add(arg.append(entry.getKey(), entry.getValue()));
            }

            return null;
        }

        private void addName(Type type, String path) {
            if (!typeNames.containsKey(type)) {
                typeNames.put(type, path);
            }
        }

        @Override
        public Void visit(ReferenceType t, Arg arg) {
            if (seen.contains(t)) {
                return null;
            }
            seen.add(t);
            addName(t, arg.path);

            for (int i = 0; i < t.getTypeArguments().size(); i++) {
                queue.add(arg.append("[typeArg" + i + "]", t.getTypeArguments().get(i)));
            }

            return t.getTarget().accept(this, arg);
        }

        @Override
        public Void visit(SimpleType t, Arg arg) {
            return null;
        }

        @Override
        public Void visit(TupleType t, Arg arg) {
            if (seen.contains(t)) {
                return null;
            }
            seen.add(t);
            addName(t, arg.path);

            for (int i = 0; i < t.getElementTypes().size(); i++) {
                queue.add(arg.append(Integer.toString(i), t.getElementTypes().get(i)));
            }

            return null;
        }

        @Override
        public Void visit(UnionType t, Arg arg) {
            if (seen.contains(t)) {
                return null;
            }
            seen.add(t);
            addName(t, arg.path);

            for (int i = 0; i < t.getElements().size(); i++) {
                queue.add(arg.append("[union" + i + "]", t.getElements().get(i)));
            }

            return null;
        }

        @Override
        public Void visit(TypeParameterType t, Arg arg) {
            if (seen.contains(t)) {
                return null;
            }
            seen.add(t);
            addName(t, arg.path);

            if (t.getConstraint() != null) {
                queue.add(arg.append("[constraint]", t.getConstraint()));
            }

            return null;
        }

        @Override
        public Void visit(StringLiteral t, Arg arg) {
            return null;
        }

        @Override
        public Void visit(BooleanLiteral t, Arg arg) {
            return null;
        }

        @Override
        public Void visit(NumberLiteral t, Arg arg) {
            return null;
        }

        @Override
        public Void visit(IntersectionType t, Arg arg) {
            if (seen.contains(t)) {
                return null;
            }
            seen.add(t);
            addName(t, arg.path);

            for (int i = 0; i < t.getElements().size(); i++) {
                queue.add(arg.append("[intersection" + i + "]", t.getElements().get(i)));
            }

            return null;
        }

        @Override
        public Void visit(ClassInstanceType t, Arg arg) {
            if (seen.contains(t)) {
                return null;
            }
            seen.add(t);
            addName(t, arg.path);

            queue.add(arg.append("[instanceOf]", t.getClassType()).extraDepth(1000));

            return null;
        }

        @Override
        public Void visit(ThisType t, Arg arg) {
            if (seen.contains(t)) {
                return null;
            }
            seen.add(t);
            addName(t, arg.path);

            queue.add(arg.append("[constraint]", t.getConstraint()));

            return null;
        }

        @Override
        public Void visit(IndexType t, Arg arg) {
            if (seen.contains(t)) {
                return null;
            }
            seen.add(t);
            addName(t, arg.path);

            queue.add(arg.append("[index]", t.getType()));
            return null;
        }

        @Override
        public Void visit(IndexedAccessType t, Arg arg) {
            if (seen.contains(t)) {
                return null;
            }
            seen.add(t);
            addName(t, arg.path);

            queue.add(arg.append("[indexType]", t.getIndexType()));
            queue.add(arg.append("[objectType]", t.getObjectType()));

            return null;
        }
    }
}
