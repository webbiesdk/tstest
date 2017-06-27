package dk.webbies.tajscheck.testcreator.test;

/**
 * Created by erik1 on 02-11-2016.
 */
public interface TestVisitor<T> {

    T visit(PropertyReadTest test);

    T visit(LoadModuleTest test);

    T visit(MethodCallTest test);

    T visit(ConstructorCallTest test);

    T visit(FunctionCallTest test);

    T visit(FilterTest test);

    T visit(UnionTypeTest test);

    T visit(NumberIndexTest test);

    T visit(StringIndexTest test);

    T visit(PropertyWriteTest test);
}
