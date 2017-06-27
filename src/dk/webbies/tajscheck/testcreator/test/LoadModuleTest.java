package dk.webbies.tajscheck.testcreator.test;

import dk.au.cs.casa.typescript.types.Type;
import dk.webbies.tajscheck.benchmark.Benchmark;
import dk.webbies.tajscheck.benchmark.BenchmarkInfo;
import dk.webbies.tajscheck.typeutil.typeContext.TypeContext;

import java.util.Collections;

public class LoadModuleTest extends Test {
    private final String module;
    private Type moduleType;

    public LoadModuleTest(String module, Type moduleType, BenchmarkInfo info, String path) {
        super(
                Collections.emptyList(),
                info.bench.run_method == Benchmark.RUN_METHOD.BOOTSTRAP ? Collections.singletonList(moduleType) : Collections.emptyList(),
                moduleType,
                path,
                TypeContext.create(info)
        );
        this.module = module;
        this.moduleType = moduleType;
    }

    public Type getModuleType() {
        return moduleType;
    }

    public String getModule() {
        return module;
    }

    @Override
    public boolean equalsNoPath(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LoadModuleTest test = (LoadModuleTest) o;
        if (!test.module.equals(this.module)) return false;
        return super.equalsNoPathBase(test);
    }

    @Override
    public int hashCodeNoPath() {
        return super.hashCodeNoPathBase() * this.module.hashCode();
    }

    @Override
    public String getTestType() {
        return "load module";
    }

    @Override
    public <T> T accept(TestVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String toString() { return this.getClass().getSimpleName()+"(" + module + ":" + moduleType + ")";}
}
