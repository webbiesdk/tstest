package dk.webbies.tajscheck.testcreator.test;

import dk.au.cs.casa.typescript.types.Signature;
import dk.au.cs.casa.typescript.types.Type;
import dk.webbies.tajscheck.typeutil.PrettyTypes;
import dk.webbies.tajscheck.typeutil.typeContext.TypeContext;

import java.util.Collection;
import java.util.List;

/**
 * Created by erik1 on 02-11-2016.
 */
public abstract class FunctionTest extends Test {
    private final List<Type> parameters;
    private final List<Signature> precedingSignatures;
    private boolean restArgs;

    public FunctionTest(Collection<Type> typeToTest, List<Type> parameters, Type returnType, String path, TypeContext typeContext, List<Signature> precedingSignatures, boolean restArgs) {
        super(typeToTest, parameters, returnType, path + PrettyTypes.parametersTypes(parameters, restArgs), typeContext);
        this.parameters = parameters;
        this.precedingSignatures = precedingSignatures;
        this.restArgs = restArgs;
    }

    public List<Type> getParameters() {
        return parameters;
    }

    public final boolean isRestArgs() {
        return restArgs;
    }

    public List<Signature> getPrecedingSignatures() {
        return precedingSignatures;
    }

    public final Type getReturnType() {
        assert this.getProduces().size() == 1;
        return this.getProduces().iterator().next();
    }
}
