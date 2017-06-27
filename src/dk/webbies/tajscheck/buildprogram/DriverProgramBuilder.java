package dk.webbies.tajscheck.buildprogram;

import dk.au.cs.casa.typescript.types.ReferenceType;
import dk.au.cs.casa.typescript.types.Type;
import dk.au.cs.casa.typescript.types.UnionType;
import dk.webbies.tajscheck.ExecutionRecording;
import dk.webbies.tajscheck.Main;
import dk.webbies.tajscheck.TypeWithContext;
import dk.webbies.tajscheck.benchmark.Benchmark;
import dk.webbies.tajscheck.benchmark.BenchmarkInfo;
import dk.webbies.tajscheck.paser.AST.ArrayLiteral;
import dk.webbies.tajscheck.paser.AST.Expression;
import dk.webbies.tajscheck.paser.AST.Operator;
import dk.webbies.tajscheck.paser.AST.Statement;
import dk.webbies.tajscheck.paser.AstBuilder;
import dk.webbies.tajscheck.testcreator.test.*;
import dk.webbies.tajscheck.testcreator.test.check.Check;
import dk.webbies.tajscheck.testcreator.test.check.CheckToExpression;
import dk.webbies.tajscheck.typeutil.typeContext.TypeContext;
import dk.webbies.tajscheck.util.Pair;
import dk.webbies.tajscheck.util.Util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static dk.webbies.tajscheck.paser.AstBuilder.*;

/**
 * Created by erik1 on 02-11-2016.
 */
public class DriverProgramBuilder {
    public static final String VARIABLE_NO_VALUE = "no_value";
    public static final String VALUE_VARIABLE_PREFIX = "value_";
    public static final String RUNTIME_ERROR_NAME = "RuntimeError";
    public static final String START_OF_FILE_MARKER = "-!-!-!- START OF FILE MARKER -!-!-!-:";

    private final List<Test> tests;
    private final BenchmarkInfo info;
    private final TypeChecker typeChecker;

    private TypeCreator typeCreator;


    public DriverProgramBuilder(List<Test> tests, BenchmarkInfo info) {
        this.tests = new ArrayList<>(tests);
        this.info = info;
        this.typeChecker = new TypeChecker(info);
    }

    public Statement buildDriver(ExecutionRecording recording) throws IOException {
        List<Statement> program = new ArrayList<>();
        this.typeCreator = new TypeCreator(tests, info, typeChecker);

        // var initialRandomness = Math.random()
        if (recording == null || recording.seed == null) {
            program.add(variable("initialRandomness", methodCall(identifier("Math"), "random")));
        } else {
            program.add(variable("initialRandomness", string(recording.seed)));
        }

        program.add(variable("maxTime", number(info.options.maxTime)));
        program.add(variable("maxIterations", number(info.options.maxIterationsToRun)));

        program.add(variable("failOnAny", bool(info.options.failOnAny)));

        program.add(AstBuilder.programFromFile(DriverProgramBuilder.class.getResource("/prelude.js")));

        program.add(block(typeCreator.getValueVariableDeclarationList()));

        program.add(block(typeChecker.getTypeCheckingFunctionList()));

        // Adding all the getType_X functions.

        program.add(typeCreator.getBlockStatementWithTypeFunctions());

        if (recording == null || recording.testSequence == null) {
            program.add(variable("runRecording", bool(false)));
        } else {
            program.add(variable("runRecording", bool(true)));
            program.add(variable("recording", array()));
            Arrays.stream(recording.testSequence)
                    .mapToObj(i -> methodCall(identifier("recording"), "push", number(i)))
                    .map(AstBuilder::statement)
                    .forEach(program::add);
        }

        for (int i = 0; i < tests.size(); i++) {
            Test test = tests.get(i);
            List<ArrayLiteral> args = test
                    .getTypeToTest()
                    .stream()
                    .map(typeToTest -> typeCreator.getValueIndex(typeToTest, test.getTypeContext()))
                    .map(valueIndexes -> valueIndexes.stream().distinct().map(AstBuilder::number).collect(Collectors.toList()))
                    .map(AstBuilder::array)
                    .collect(Collectors.toList());

            program.add(statement(
                    call(identifier("registerTest"), number(i), array(args))
            ));
        }

        program.add(statement(function("testStuff", block(
                statement(call(identifier("print"), string("total number of tests: " + tests.size()))),
                statement(call(identifier("continuation"))),
                statement(function("continuation",
                    block(
                            variable("testNumberToRun", call(identifier("selectTest"))),
                            ifThen(
                                    binary(identifier("testNumberToRun"), Operator.LESS_THAN, number(0)),
                                    block(
                                            statement(call(identifier("dumbMessages"))),
                                            Return()
                                    )
                            ),
                            statement(methodCall(identifier("testOrderRecording"), "push", identifier("testNumberToRun"))),
                            tryCatch(
                                    AstBuilder.switchCase(
                                            identifier("testNumberToRun"),
                                            buildTestCases()),
                                    catchBlock(
                                            identifier("e"),
                                            block(
                                                    // statement(call(identifier("error"), expFromString("e.toString()")))
                                            )
                                    )
                            ),
                            ifThenElse(binary(binary(identifier("i"), Operator.MOD, number(100)), Operator.EQUAL_EQUAL_EQUAL, number(0)),
                                    statement(call(
                                            identifier("setTimeout"),
                                            identifier("continuation")
                                    )),
                                    statement(call(identifier("continuation")))
                            )
                    )
                ))
        ))));

        program.add(AstBuilder.programFromFile(DriverProgramBuilder.class.getResource("/dumb.js")));

        if (info.bench.run_method == Benchmark.RUN_METHOD.BROWSER) {
            List<Statement> scripts = new ArrayList<>();
            for (Benchmark dependency : info.bench.getDependencies()) {
                String dependencyScript = Util.readFile(dependency.jsFile);
                String jsName = dependency.getJSName();
                scripts.add(comment(START_OF_FILE_MARKER + jsName));
                scripts.add(AstBuilder.stmtFromString(dependencyScript));
            }

            scripts.add(comment(START_OF_FILE_MARKER + info.bench.jsFile.substring(info.bench.jsFile.lastIndexOf('/') + 1, info.bench.jsFile.length())));

            scripts.add(AstBuilder.stmtFromString(Util.readFile(info.bench.jsFile)));

            scripts.add(comment(START_OF_FILE_MARKER + Main.TEST_FILE_NAME));

            return block(
                    block(scripts),
                    statement(call(function(block(program))))
            );
        } else {
            assert info.bench.run_method == Benchmark.RUN_METHOD.NODE || info.bench.run_method == Benchmark.RUN_METHOD.BOOTSTRAP;
            return statement(call(function(block(program))));
        }


    }

    private List<Pair<Expression, Statement>> buildTestCases() {
        List<Pair<Expression, Statement>> result = new ArrayList<>();

        List<Test> tests = new ArrayList<>(this.tests);

        for (int i = 0; i < tests.size(); i++) {
            Test test = tests.get(i);
            result.add(new Pair<>(
                    number(i),
                    block(
                            comment("path: " + test.getPath() + " type: " + test.getClass().getSimpleName()),
                            statement(call(identifier("testCalled"), number(i))),
                            statement(call(function(block(buildTestCase(test))))),
                            breakStatement()
                    )
            ));
        }

        return result;
    }

    private List<Statement> buildTestCase(Test test) {
        List<Statement> testCode = test.accept(new TestBuilderVisitor());

        List<Type> produces = new ArrayList<>(test.getProduces());
        assert produces.size() == typeCreator.getTestProducesIndexes(test).size();

        Statement saveResultStatement;
        if (produces.size() == 0) {
            saveResultStatement = block();
        } else if (produces.size() == 1) {
            Type product = produces.iterator().next();
            int index = typeCreator.getTestProducesIndexes(test).iterator().next();
            saveResultStatement = block(
                    info.options.makeSeparateReportAssertions() ? block() : expressionStatement(call(function(
                            block(
                                    comment("There warnings are just reported, not used to see if the value should be used (that comes below). "),
                                    typeChecker.assertResultingType(new TypeWithContext(product, test.getTypeContext()), identifier("result"), test.getPath(), info.options.checkDepthReport, test.getTestType())
                            )
                    ))),
                    typeChecker.assertResultingType(new TypeWithContext(product, test.getTypeContext()), identifier("result"), test.getPath(), info.options.checkDepthUseValue, test.getTestType()),
                    statement(binary(identifier(VALUE_VARIABLE_PREFIX + index), Operator.EQUAL, identifier("result"))),
                    statement(call(identifier("registerValue"), number(index)))
            );
        } else {
            List<Integer> valueIndexes = typeCreator.getTestProducesIndexes(test);

            saveResultStatement = block(
                    variable("passedResults", array()),
                    block(
                            Util.withIndex(produces).map(pair -> {
                                Type type = pair.getLeft();
                                Integer valueIndex = pair.getRight();
                                return block(
                                        variable("passed" + valueIndex, typeChecker.checkResultingType(new TypeWithContext(type, test.getTypeContext()), identifier("result"), test.getPath(), info.options.checkDepthForUnions)),
                                        ifThen(
                                                identifier("passed" + valueIndex),
                                                statement(methodCall(identifier("passedResults"), "push", number(valueIndex)))
                                        )
                                );
                            }).collect(Collectors.toList())
                    ),
                    // If no type passed, then we have an assertionError
                    ifThen(
                            binary(
                                    member(identifier("passedResults"), "length"),
                                    Operator.EQUAL_EQUAL_EQUAL,
                                    number(0)
                            ),
                            block(
                                    statement(
                                            call(
                                                    identifier("assert"),
                                                    binary(
                                                            member(identifier("passedResults"), "length"),
                                                            Operator.NOT_EQUAL_EQUAL,
                                                            number(0)
                                                    ),
                                                    string(test.getPath()),
                                                    string(typeChecker.getTypeDescription(new TypeWithContext(createUnionType(produces), test.getTypeContext()), info.options.checkDepthForUnions)),
                                                    identifier("result"),
                                                    identifier("i"),
                                                    string(test.getTestType())
                                            )
                                    ),
                                    Return()
                            )
                    ),
                    // If we have more than 1 type that passed, then it is our fault, we cannot distinguish between the union types.
                    ifThen(
                            binary(
                                    member(identifier("passedResults"), "length"),
                                    Operator.GREATER_THAN_EQUAL,
                                    number(2)
                            ),
                            block(
                                    statement(call(identifier("error"), binary(string("Could not distinguish which union on path: " + test.getPath() + " types: "), Operator.PLUS, methodCall(identifier("passedResults"), "toString")))),
                                    Return()
                            )
                    ),
                    // Otherwise, assign to the single found union-type, the result.
                    switchCase(
                            arrayAccess(identifier("passedResults"), number(0)),
                            IntStream.range(0, produces.size()).mapToObj(index ->
                                    new Pair<Expression, Statement>(
                                            number(index),
                                            block(
                                                    statement(binary(identifier(VALUE_VARIABLE_PREFIX + valueIndexes.get(index)), Operator.EQUAL, identifier("result"))),
                                                    statement(call(identifier("registerValue"), number(valueIndexes.get(index)))),
                                                    breakStatement()
                                            )
                                    )
                            ).collect(Collectors.toList())
                    )
            );
        }

        /*
         * Check dependencies
         * Run test, put result in "result"
         * Check that "result" is of the right type
         * Store result for use by other tests
         */

        Type product;
        if (test.getProduces().size() == 0) {
            product = null;
        } else if (test.getProduces().size() == 1) {
            product = test.getProduces().iterator().next();
        } else {
            UnionType union = new UnionType();
            union.setElements(new ArrayList<>(test.getProduces()));
            product = union;
        }

        return Util.concat(
                testCode,
                Collections.singletonList(saveResultStatement)
        );
    }

    private UnionType createUnionType(List<Type> types) {
        UnionType union = new UnionType();
        union.setElements(types);
        return union;
    }

    /**
     * For each of these, produce code that expects all the getTypeString calls to succeed.
     * And the result of the test should be put into a variable "result".
     */
    private class TestBuilderVisitor implements TestVisitor<List<Statement>> {
        Expression getTypeExpression(Type type, TypeContext typeContext) {
            return typeCreator.getType(type, typeContext);
        }

        @Override
        public List<Statement> visit(PropertyReadTest test) {
            List<Statement> result = new ArrayList<>();
            result.add(variable("base", getTypeExpression(test.getBaseType(), test.getTypeContext())));
            if (Util.isInteger(test.getProperty())) {
                result.add(variable("result", arrayAccess(identifier("base"), number(Integer.parseInt(test.getProperty())))));
            } else {
                result.add(variable("result", member(identifier("base"), test.getProperty())));
            }
            return result;
        }

        @Override
        public List<Statement> visit(LoadModuleTest test) {
            switch (info.bench.run_method) {
                case NODE:
                    return Collections.singletonList(
                            variable(
                                    identifier("result"),
                                    call(identifier("loadLibrary"), string(test.getModule()))
                            )
                    );
                case BROWSER:
                    return Collections.singletonList(
                            variable("result", identifier(test.getPath()))
                    );
                case BOOTSTRAP:
                    return Collections.singletonList(
                            variable("result", typeCreator.constructType(test.getModuleType(), test.getTypeContext()))
                    );
                default:
                    throw new RuntimeException();
            }
        }

        @Override
        public List<Statement> visit(MethodCallTest test) {
            if (info.options.makeTSInferLike) {
                return Collections.singletonList(throwStatement(newCall(identifier("Error"))));
            } else {
                return callFunction(test, test.getObject(), test.getParameters(), test.isRestArgs(), (base, parameters) ->
                        methodCall(identifier("base"), test.getPropertyName(), parameters)
                );
            }
        }

        @Override
        public List<Statement> visit(ConstructorCallTest test) {
            if (info.options.makeTSInferLike) {
                return callFunction(test, test.getFunction(), Collections.emptyList(), false, AstBuilder::newCall);
            } else {
                return callFunction(test, test.getFunction(), test.getParameters(), test.isRestArgs(), AstBuilder::newCall);
            }
        }

        @Override
        public List<Statement> visit(FunctionCallTest test) {
            if (info.options.makeTSInferLike) {
                return Collections.singletonList(throwStatement(newCall(identifier("Error"))));
            } else {
                return callFunction(test, test.getFunction(), test.getParameters(), test.isRestArgs(), AstBuilder::call);
            }
        }

        private List<Statement> callFunction(FunctionTest test, Type object, List<Type> orgParameterTypes, boolean restArgs, BiFunction<Expression, List<Expression>, Expression> callGenerator) {
            if (restArgs) {
                Type restArgArr = orgParameterTypes.get(orgParameterTypes.size() - 1);
                assert restArgArr instanceof ReferenceType;
                assert "Array".equals(info.typeNames.get(((ReferenceType) restArgArr).getTarget()));
                assert ((ReferenceType) restArgArr).getTypeArguments().size() == 1;

                Type restArgType = ((ReferenceType) restArgArr).getTypeArguments().iterator().next();

                List<Type> parameterTypes = orgParameterTypes.subList(0, orgParameterTypes.size() - 1);

                List<Integer> numberOfRestArgsList = Arrays.asList(0, 1, 3, 5);

                return Collections.singletonList(
                        switchCase(AstBuilder.expFromString("Math.random() * " + numberOfRestArgsList.size() + " | 0"),
                                Util.withIndex(numberOfRestArgsList, (numberOfRestArgs, index) -> {
                                    List<Type> parameterTypesWithRestArg = new ArrayList<Type>(parameterTypes);

                                    for (int i = 0; i < numberOfRestArgs; i++) {
                                        parameterTypesWithRestArg.add(restArgType);
                                    }

                                    return new Pair<Expression, Statement>(
                                            number(index),
                                            block(
                                                    comment("restArgs with " + numberOfRestArgs + " extra arguments"),
                                                    block(callFunction(test, object, parameterTypesWithRestArg, callGenerator)),
                                                    breakStatement()
                                            )
                                    );
                                }).collect(Collectors.toList()),
                                block(
                                        throwStatement(newCall(identifier("Error"), string("unreachable!")))
                                )

                        )
                );


            } else {
                return callFunction(test, object, orgParameterTypes, callGenerator);
            }
        }

        private List<Statement> callFunction(FunctionTest test, Type object, List<Type> parameterTypes, BiFunction<Expression, List<Expression>, Expression> callGenerator) {
            List<Statement> result = new ArrayList<>();

            result.add(variable("base", getTypeExpression(object, test.getTypeContext())));

            List<Expression> parameters = Util.withIndex(parameterTypes, (type, index) -> {
                result.add(variable(identifier("argument_" + index), typeCreator.constructType(type, test.getTypeContext())));
                return identifier("argument_" + index);
            }).collect(Collectors.toList());

            if (!test.getPrecedingSignatures().isEmpty() && info.bench.options.firstMatchSignaturePolicy) {
                result.add(block(
                        tryCatch(
                                block(
                                        variable("signatureCheckFunction", typeCreator.constructType(typeCreator.getPrecedingSignaturesType(test))),
                                        statement(call(identifier("signatureCheckFunction"), parameters)),
                                        comment("No exception thrown, I'm not continuing"),
                                        Return()
                                ),
                                catchBlock("e", block(
                                        comment("An exception was thrown, only continue if it contains 'no valid overload found'"),
                                        ifThenElse(expFromString("e.message.toLowerCase().indexOf('no valid overload found') !== -1"),
                                                comment("continue"),
                                                Return()
                                        )
                                ))
                        )
                ));
            }


            Expression newCall = callGenerator.apply(identifier("base"), parameters);
            result.add(variable("result", newCall));

            return result;
        }

        @Override
        public List<Statement> visit(FilterTest test) {
            return Arrays.asList(
                    variable("result", getTypeExpression(test.getType(), test.getTypeContext())),
                    ifThen(
                            CheckToExpression.generate(Check.not(test.getCheck()), identifier("result")),
                            Return()
                    )
            );
        }

        @Override
        public List<Statement> visit(UnionTypeTest test) {
            // Looks trivial, but that is because everything complicated is handled by the method calling this visitor.
            return Collections.singletonList(
                    variable("result", getTypeExpression(test.getGetUnionType(), test.getTypeContext()))
            );
        }

        @Override
        public List<Statement> visit(NumberIndexTest test) {
            return Arrays.asList(
                    variable("base", getTypeExpression(test.getObj(), test.getTypeContext())),
                    stmtFromString("var keys = getAllKeys(base).filter(function (e) {return Number(e) + \"\" === e})"),
                    stmtFromString("if (keys.length == 0) {return false}"),
                    stmtFromString("var key = keys[Math.floor(Math.random()*keys.length)];"),
                    stmtFromString("var result = base[key]")
            );
        }

        @Override
        public List<Statement> visit(StringIndexTest test) {
            return Arrays.asList(
                    variable("base", getTypeExpression(test.getObj(), test.getTypeContext())),
                    stmtFromString("var keys = getAllKeys(base)"),
                    stmtFromString("if (keys.length == 0) {return false}"),
                    stmtFromString("var key = keys[Math.floor(Math.random()*keys.length)];"),
                    stmtFromString("var result = base[key]")
            );
        }

        @Override
        public List<Statement> visit(PropertyWriteTest test) {
            return Arrays.asList(
                    variable("base", getTypeExpression(test.getBaseType(), test.getTypeContext())),
                    variable("newValue", typeCreator.constructType(test.getToWrite(), test.getTypeContext())),
                    statement(binary(
                            member(identifier("base"), test.getProperty()),
                            Operator.EQUAL,
                            identifier("newValue")
                    )),
                    stmtFromString("var result = {};")
            );
        }

    }
}
