package dk.webbies.tajscheck.typeutil;

import dk.au.cs.casa.typescript.types.*;
import dk.webbies.tajscheck.util.IdentityHashSet;
import dk.webbies.tajscheck.util.Util;

import java.util.Set;

abstract public class RecursiveTypeVisitor<T> implements TypeVisitor<T> {

        protected final Set<Type> seen = new IdentityHashSet<>();

        @Override
        public T visit(AnonymousType t) {
            return null;
        }

        @Override
        public T visit(ClassType t) {
            if (seen.contains(t)) {
                return null;
            }
            seen.add(t);

            t.getSignatures().forEach(this::acceptSignature);

            t.getInstanceType().accept(this);

            t.getBaseTypes().forEach(this::accept);

            t.getStaticProperties().values().forEach(this::accept);

            t.getTarget().accept(this);

            t.getTypeArguments().forEach(this::accept);

            return null;
        }

        @Override
        public T visit(GenericType t) {
            if (seen.contains(t)) {
                return null;
            }
            seen.add(t);

            t.getTypeArguments().forEach(this::accept);
            t.getBaseTypes().forEach(this::accept);
            t.getDeclaredProperties().values().forEach(this::accept);
            Util.concat(t.getDeclaredCallSignatures(), t.getDeclaredConstructSignatures()).forEach(this::acceptSignature);
            if (t.getDeclaredStringIndexType() != null) {
                t.getDeclaredStringIndexType().accept(this);
            }
            if (t.getDeclaredNumberIndexType() != null) {
                t.getDeclaredNumberIndexType().accept(this);
            }
            t.getTarget().accept(this);
            t.getTypeArguments().forEach(this::accept);
            t.toInterface().accept(this);
            return null;
        }

        protected void accept(Type type) {
            type.accept(this);
        }

        @Override
        public T visit(InterfaceType t) {
            if (seen.contains(t)) {
                return null;
            }
            seen.add(t);

            t.getTypeParameters().forEach(this::accept);
            t.getBaseTypes().forEach(this::accept);
            t.getDeclaredProperties().values().forEach(this::accept);
            Util.concat(t.getDeclaredCallSignatures(), t.getDeclaredConstructSignatures()).forEach(this::acceptSignature);
            if (t.getDeclaredStringIndexType() != null) {
                t.getDeclaredStringIndexType().accept(this);
            }
            if (t.getDeclaredNumberIndexType() != null) {
                t.getDeclaredNumberIndexType().accept(this);
            }
            return null;
        }

        private void acceptSignature(Signature sig) {
            if (sig.getResolvedReturnType() != null) {
                sig.getResolvedReturnType().accept(this);
            }
            sig.getParameters().stream().map(Signature.Parameter::getType).forEach(this::accept);
            if (sig.getTarget() != null) {
                acceptSignature(sig.getTarget());
            }
            sig.getUnionSignatures().forEach(this::acceptSignature);
            if (sig.getIsolatedSignatureType() != null) {
                sig.getIsolatedSignatureType().accept(this);
            }
            sig.getTypeParameters().forEach(this::accept);
        }

        @Override
        public T visit(ReferenceType t) {
            if (seen.contains(t)) {
                return null;
            }
            seen.add(t);

            t.getTarget().accept(this);
            t.getTypeArguments().forEach(this::accept);
            return null;
        }

        @Override
        public T visit(SimpleType t) {
            seen.add(t);
            return null;
        }

        @Override
        public T visit(TupleType t) {
            if (seen.contains(t)) {
                return null;
            }
            seen.add(t);

            t.getElementTypes().forEach(this::accept);

            return null;
        }

        @Override
        public T visit(UnionType t) {
            if (seen.contains(t)) {
                return null;
            }
            seen.add(t);

            t.getElements().forEach(this::accept);

            return null;
        }

        @Override
        public T visit(IntersectionType t) {
            if (seen.contains(t)) {
                return null;
            }
            seen.add(t);

            t.getElements().forEach(this::accept);

            return null;
        }

        @Override
        public T visit(ClassInstanceType t) {
            if (seen.contains(t)) {
                return null;
            }
            seen.add(t);

            t.getClassType().accept(this);

            return null;
        }

        @Override
        public T visit(ThisType t) {
            if (seen.contains(t)) {
                return null;
            }
            seen.add(t);

            t.getConstraint().accept(this);

            return null;
        }

        @Override
        public T visit(IndexType t) {
            if (seen.contains(t)) {
                return null;
            }
            seen.add(t);

            t.getType().accept(this);

            return null;
        }

        @Override
        public T visit(IndexedAccessType t) {
            if (seen.contains(t)) {
                return null;
            }
            seen.add(t);

            t.getObjectType().accept(this);
            t.getIndexType().accept(this);

            return null;
        }

        @Override
        public T visit(TypeParameterType t) {
            if (seen.contains(t)) {
                return null;
            }
            seen.add(t);

            if (t.getConstraint() != null) {
                t.getConstraint().accept(this);
            }
            return null;
        }

        @Override
        public T visit(StringLiteral t) {
            if (seen.contains(t)) {
                return null;
            }
            seen.add(t);

            return null;
        }

        @Override
        public T visit(BooleanLiteral t) {
            if (seen.contains(t)) {
                return null;
            }
            seen.add(t);

            return null;
        }

        @Override
        public T visit(NumberLiteral t) {
            if (seen.contains(t)) {
                return null;
            }
            seen.add(t);

            return null;
        }
}
