package dk.webbies.tajscheck.buildprogram;

import dk.au.cs.casa.typescript.types.*;
import dk.au.cs.casa.typescript.types.BooleanLiteral;
import dk.au.cs.casa.typescript.types.NumberLiteral;
import dk.au.cs.casa.typescript.types.StringLiteral;
import dk.webbies.tajscheck.TypeWithContext;
import dk.webbies.tajscheck.benchmark.BenchmarkInfo;
import dk.webbies.tajscheck.buildprogram.typechecks.FieldTypeCheck;
import dk.webbies.tajscheck.buildprogram.typechecks.SimpleTypeCheck;
import dk.webbies.tajscheck.buildprogram.typechecks.TypeCheck;
import dk.webbies.tajscheck.paser.AST.*;
import dk.webbies.tajscheck.testcreator.test.check.Check;
import dk.webbies.tajscheck.testcreator.test.check.CheckToExpression;
import dk.webbies.tajscheck.typeutil.TypesUtil;
import dk.webbies.tajscheck.typeutil.typeContext.TypeContext;
import dk.webbies.tajscheck.util.Util;

import java.util.*;
import java.util.stream.Collectors;

import static dk.webbies.tajscheck.paser.AstBuilder.*;

public class TypeChecker {
    private final BenchmarkInfo info;
    private List<Statement> typeCheckingFunctionList = new ArrayList<>();

    public TypeChecker(BenchmarkInfo info) {
        this.info = info;
    }

    public Expression checkResultingType(TypeWithContext type, Expression exp, String path, int depth) {
        path = sanitizePath(path);

        return call(function(
                block(
                        statement(function("assert", block(
                                comment("Overriding assert, i'm only interested in IF the type check passes. "),
                                Return(identifier("cond"))
                        ), "cond")),

                        assertResultingType(type, exp, path, depth, "SHOULD NOT BE OUTPUTTED!"), // This returns false, if the type check fails
                        Return(bool(true)) // If it didn't return false, then the type check passed!
                )
        ));
    }

    private String sanitizePath(String path) {
        if (path.startsWith("window.")) {
            return Util.removePrefix(path, "window.");
        } else {
            return path;
        }
    }

    public String getTypeDescription(TypeWithContext type, int depth) {
        List<TypeCheck> result = type.getType().accept(new CreateTypeCheckVisitor(info), new Arg(type.getTypeContext(), depth));
        return createIntersection(result).getExpected();
    }

    public Statement assertResultingType(TypeWithContext type, Expression exp, String path, int depth, String testType) {
        path = sanitizePath(path);

        List<TypeCheck> typeChecks = type.getType().accept(new CreateTypeCheckVisitor(info), new Arg(type.getTypeContext(), depth));
        if (info.bench.options.useAssertTypeFunctions) {
            return block(
                    ifThen(
                            unary(Operator.NOT, checkToAssertions(typeChecks, exp, string(path), string(testType))),
                            Return(bool(false))
                    )
            );
        } else {
            String finalPath = path;
            return block(
                    variable("typeChecked", bool(true)),
                    block(
                            typeChecks.stream().map(check -> inlineCheckToAssertions(check, exp, finalPath, testType)).collect(Collectors.toList())
                    ),
                    ifThen(
                            unary(Operator.NOT, identifier("typeChecked")),
                            Return(bool(false))
                    )
            );
        }
    }

    private CallExpression checkToAssertions(List<TypeCheck> typeChecks, Expression exp, Expression path, Expression testType) {
        Expression function = createAssertTypeFunction(typeChecks);

        return call(function, identifier("assert"), exp, path, testType);
    }

    private Expression createAssertTypeFunction(List<TypeCheck> typeChecks) {
        if (!typeCheckFunctionNameCache.containsKey(typeChecks)) {
            Identifier exp = identifier("exp");
            Identifier path = identifier("path");
            Identifier testType = identifier("testType");

            List<Statement> result = new ArrayList<>();

            for (TypeCheck typeCheck : typeChecks) {
                if (typeCheck instanceof FieldTypeCheck) {

                    FieldTypeCheck fieldTypeCheck = (FieldTypeCheck) typeCheck;
                    String field = fieldTypeCheck.getField();

                    result.add(ifThen(
                            unary(Operator.NOT, checkToAssertions(fieldTypeCheck.getFieldChecks(), member(exp, field), binary(path, Operator.PLUS, string("." + field)), testType)),
                            block(
                                    Return(bool(false))
                            )
                    ));
                } else {
                    assert typeCheck instanceof SimpleTypeCheck;
                    Expression checkExpression = CheckToExpression.generate(typeCheck.getCheck(), exp);
                    CallExpression assertCall = call(identifier("assert"), checkExpression, path, string(typeCheck.getExpected()), exp, identifier("i"), testType);
                    result.add(ifThen(
                            unary(Operator.NOT, assertCall),
                            block(
                                    Return(bool(false))
                            )
                    ));
                }
            }
            result.add(Return(bool(true)));

            String name = "assertType_" + typeCheckingFunctionList.size();
            FunctionExpression function = function(name, block(result), "assert", "exp", "path", "testType");

            typeCheckingFunctionList.add(statement(function));

            typeCheckFunctionNameCache.put(typeChecks, name);
        }

        return identifier(typeCheckFunctionNameCache.get(typeChecks));
    }

    private Statement inlineCheckToAssertions(TypeCheck typeCheck, Expression exp, String path, String testType) {
        if (typeCheck instanceof FieldTypeCheck) {
            FieldTypeCheck fieldTypeCheck = (FieldTypeCheck) typeCheck;
            String field = fieldTypeCheck.getField();
            return statement(call(function(block(
                    fieldTypeCheck.getFieldChecks().stream().map(subCheck -> inlineCheckToAssertions(subCheck, member(exp, field), path + "." + field, testType)).collect(Collectors.toList())
            ))));
        }

        assert typeCheck instanceof SimpleTypeCheck;
        Expression checkExpression = CheckToExpression.generate(typeCheck.getCheck(), exp);
        CallExpression assertCall = call(identifier("assert"), checkExpression, string(path), string(typeCheck.getExpected()), exp, identifier("i"), string(testType));
        return ifThen(
                unary(Operator.NOT, assertCall),
                block(
                        statement(binary(identifier("typeChecked"), Operator.EQUAL, bool(false))),
                        Return(bool(false))
                )
        );
    }

    public static List<TypeCheck> getTypeChecks(Type type, TypeContext context, BenchmarkInfo info, int depth) {
        return type.accept(new CreateTypeCheckVisitor(info), new Arg(context, depth));
    }

    private final Map<List<TypeCheck>, String> typeCheckFunctionNameCache = new HashMap<>();

    public List<Statement> getTypeCheckingFunctionList() {
        return typeCheckingFunctionList;
    }

    static final class Arg {
        final TypeContext typeContext;
        final int depthRemaining;

        Arg(TypeContext typeContext, int depthRemaining) {
            this.typeContext = typeContext;
            this.depthRemaining = depthRemaining;
        }

        public Arg withContext(TypeContext map) {
            return new Arg(map, this.depthRemaining);
        }

        public Arg decreaseDepth() {
            return new Arg(this.typeContext, this.depthRemaining - 1);
        }

        public Arg withDepth(int depth) {
            return new Arg(typeContext, depth);
        }
    }

    static final class CreateTypeCheckVisitor implements TypeVisitorWithArgument<List<TypeCheck>, Arg> {
        private final BenchmarkInfo info;

        CreateTypeCheckVisitor(BenchmarkInfo info) {
            this.info = info;
        }

        @Override
        public List<TypeCheck> visit(AnonymousType t, Arg arg) {
            return Collections.emptyList();
        }

        @Override
        public List<TypeCheck> visit(ClassType t, Arg arg) {
            List<TypeCheck> result = new ArrayList<>();

            result.add(new SimpleTypeCheck(
                    Check.typeOf("function"),
                    "a constructor"
            ));

            if (arg.depthRemaining > 0) {
                Arg subArg = arg.decreaseDepth();
                for (Map.Entry<String, Type> entry : t.getStaticProperties().entrySet()) {
                    List<TypeCheck> fieldChecks = entry.getValue().accept(this, subArg);
                    if (!fieldChecks.isEmpty()) {
                        result.add(new FieldTypeCheck(entry.getKey(), fieldChecks));
                    }
                }
            }

            return result;
        }

        @Override
        public List<TypeCheck> visit(GenericType t, Arg arg) {
            if (info.nativeTypes.contains(t)) {
                switch (info.typeNames.get(t)) {
                    case "RegExp":
                    case "HTMLCanvasElement":
                    case "HTMLImageElement":
                    case "HTMLVideoElement":
                    case "Float32Array":
                    case "HTMLElement":
                    case "XMLHttpRequest":
                    case "Uint16Array":
                    case "Uint32Array":
                    case "Int8Array":
                    case "Uint8Array":
                    case "Int32Array":
                    case "Uint8ClampedArray":
                    case "Int16Array":
                    case "Float64Array":
                    case "Selection":
                    case "Promise":
                    case "Request":
                        return Collections.singletonList(
                                new SimpleTypeCheck(
                                        Check.instanceOf(identifier(info.typeNames.get(t))),
                                        info.typeNames.get(t)
                                )
                        );
                    case "Array":
                        assert t.getTypeArguments().size() == 1;
                        return checkArrayThinghy(t.getTypeArguments().get(0), "Array", arg);
                    case "NodeListOf":
                        assert t.getTypeArguments().size() == 1;
                        return checkArrayThinghy(t.getTypeArguments().get(0), "NodeList", arg.withDepth(0)); // NodeLists not checked.
                    case "ArrayLike":
                    case "IterableIterator":
                    case "Iterator":
                        break; // Check manually.
                    default:
                        throw new RuntimeException(info.typeNames.get(t));

                }
            }
            return t.toInterface().accept(this, arg);
        }

        @Override
        public List<TypeCheck> visit(InterfaceType t, Arg arg) {
            if (TypesUtil.isEmptyInterface(t)) {
                return Collections.singletonList(new SimpleTypeCheck(Check.alwaysTrue(), "[any]"));
            }
            if (info.nativeTypes.contains(t) && !info.typeNames.get(t).startsWith("window.")) {
                String name = info.typeNames.get(t);

                if (name.startsWith("global.")) {
                    name = name.substring("global.".length(), name.length());
                }

                switch (name) {
                    case "Function":
                        return Collections.singletonList(new SimpleTypeCheck(Check.typeOf("function"), "function"));
                    case "String":
                        return Collections.singletonList(
                                new SimpleTypeCheck(
                                        Check.or(
                                                Check.typeOf("string"),
                                                Check.instanceOf(identifier("String"))
                                        ),
                                        "String"
                                )
                        );
                    case "Boolean":
                        return Collections.singletonList(
                                new SimpleTypeCheck(
                                        Check.or(
                                                Check.typeOf("boolean"),
                                                Check.instanceOf(identifier("Boolean"))
                                        ),
                                        "Boolean"
                                )
                        );
                    case "Number":
                        return Collections.singletonList(
                                new SimpleTypeCheck(
                                        Check.or(
                                                Check.typeOf("number"),
                                                Check.instanceOf(identifier("Number"))
                                        ),
                                        "Number"
                                )
                        );
                    case "NumberConstructor":
                        return Collections.singletonList(new SimpleTypeCheck(Check.equalTo(identifier("Number")), "NumberConstructor"));
                    case "BooleanConstructor":
                        return Collections.singletonList(new SimpleTypeCheck(Check.equalTo(identifier("Boolean")), "BooleanConstructor"));
                    case "ObjectConstructor":
                        return Collections.singletonList(new SimpleTypeCheck(Check.equalTo(identifier("Object")), "ObjectConstructor"));
                    case "ArrayConstructor":
                        return Collections.singletonList(new SimpleTypeCheck(Check.equalTo(identifier("Array")), "ArrayConstructor"));
                    case "DateConstructor":
                        return Collections.singletonList(new SimpleTypeCheck(Check.equalTo(identifier("Date")), "DateConstructor"));
                    case "StringConstructor":
                        return Collections.singletonList(new SimpleTypeCheck(Check.equalTo(identifier("String")), "StringConstructor"));
                    case "ErrorConstructor":
                        return Collections.singletonList(new SimpleTypeCheck(Check.equalTo(identifier("Error")), "StringConstructor"));
                    case "Object":
                        return Arrays.asList(expectNotNull(), new SimpleTypeCheck(Check.typeOf("object"), "Object"));
                    case "Console":
                        return Collections.singletonList(new SimpleTypeCheck(Check.equalTo(identifier("console")), "console"));
                    case "Date":
                    case "Error":
                    case "ImageData":
                    case "WebGLBuffer":
                    case "WebGLContextEvent":
                    case "WebGLTexture":
                    case "CanvasRenderingContext2D":
                    case "WebGLProgram":
                    case "ArrayBuffer":
                    case "CanvasGradient":
                    case "WebGLFramebuffer":
                    case "HTMLCanvasElement":
                    case "WebGLRenderbuffer":
                    case "HTMLImageElement":
                    case "HTMLVideoElement":
                    case "XMLHttpRequest":
                    case "HTMLElement":
                    case "CanvasPattern":
                    case "MouseEvent":
                    case "PointerEvent":
                    case "TouchEvent":
                    case "ErrorEvent":
                    case "ProgressEvent":
                    case "Document":
                    case "Element":
                    case "Text":
                    case "DocumentFragment":
                    case "Node":
                    case "XMLDocument":
                    case "DragEvent":
                    case "MessageEvent":
                    case "UIEvent":
                    case "KeyboardEvent":
                    case "DeviceOrientationEvent":
                    case "PageTransitionEvent":
                    case "WheelEvent":
                    case "DeviceMotionEvent":
                    case "PopStateEvent":
                    case "FocusEvent":
                    case "BeforeUnloadEvent":
                    case "StorageEvent":
                    case "HashChangeEvent":
                    case "Window":
                    case "SVGElement":
                    case "EventTarget":
                    case "SVGGElement":
                    case "TouchList":
                    case "SVGSVGElement":
                    case "HTMLScriptElement":
                    case "DataTransfer":
                    case "Location":
                    case "DynamicsCompressorNode":
                    case "GainNode":
                    case "AudioContext":
                    case "AudioNode":
                    case "PannerNode":
                    case "HTMLAudioElement":
                    case "AudioBufferSourceNode":
                    case "HTMLDocument":
                    case "CSSStyleSheet":
                    case "HTMLTrackElement":
                    case "TimeRanges":
                    case "HTMLInputElement":
                    case "HTMLTextAreaElement":
                    case "HTMLSourceElement":
                    case "HTMLDivElement":
                    case "NodeList":
                    case "HTMLCollection":
                    case "Range":
                    case "Request":
                    case "Headers":
                    case "CustomElementRegistry":
                    case "CacheStorage":
                    case "SpeechSynthesisUtterance":
                    case "IDBFactory":
                    case "Storage":
                    case "Navigator":
                    case "MediaQueryList":
                    case "URL":
                    case "BarProp":
                    case "ApplicationCache":
                    case "Screen":
                    case "Blob":
                    case "History":
                    case "Crypto":
                    case "CSSStyleDeclaration":
                    case "CSSRuleList":
                    case "Selection":
                    case "Performance":
                    case "SVGImageElement":
                    case "URLSearchParams":
                    case "ImageBitmap":
                        return Collections.singletonList(new SimpleTypeCheck(Check.instanceOf(identifier(name)), name));
                    case "WebGLRenderingContext":
                        return Collections.singletonList(new SimpleTypeCheck(Check.or(Check.instanceOf(identifier(name)), Check.equalTo(nullLiteral())), name)); // TODO: Headless chrome doesn't have WebGL context.
                    case "StyleMedia":
                        return Collections.singletonList(new SimpleTypeCheck(Check.instanceOf(expFromString("window.styleMedia.__proto__.constructor")), name));
                    case "MSPointerEvent":
                    case "MSGestureEvent":
                    case "DeviceLightEvent":
                    case "MediaStreamErrorEvent":
                        // Checking both that the type exists, and that it is an instance of.
                        return Collections.singletonList(new SimpleTypeCheck(Check.and(Check.expression(
                                binary(member(identifier("window"), name), Operator.INSTANCEOF, string("function"))
                        ), Check.instanceOf(identifier(name))), name));
                    case "Event":
                        List<TypeCheck> structuralCheckList = new ArrayList<>();
                        for (Map.Entry<String, Type> entry : t.getDeclaredProperties().entrySet()) {
                            List<TypeCheck> fieldChecks = entry.getValue().accept(this, arg);
                            if (!fieldChecks.isEmpty()) {
                                structuralCheckList.add(new FieldTypeCheck(entry.getKey(), fieldChecks));
                            }
                        }

                        TypeCheck structuralCheck = createIntersection(structuralCheckList);
                        return Collections.singletonList(new SimpleTypeCheck(Check.or(structuralCheck.getCheck(), Check.instanceOf(identifier(name))), "(" + name + " or " + structuralCheck.getExpected() + ")"));
                    case "EventListener":
                    case "ObjectURLOptions":
                    case "EventListenerObject":
                    case "ScrollToOptions":
                    case "ScrollOptions":
                    case "WebKitPoint":
                    case "FrameRequestCallback":
                    case "BlobPropertyBag":
                    case "CanvasPathMethods":
                    case "Intl.CollatorOptions":
                    case "Intl.ResolvedCollatorOptions":
                    case "Intl.NumberFormatOptions":
                    case "Intl.ResolvedNumberFormatOptions":
                    case "Intl.DateTimeFormatOptions":
                    case "Intl.ResolvedDateTimeFormatOptions":
                    case "RTCIceServer":
                    case "FocusNavigationOrigin":
                    case "RequestInit":
                    case "DeviceAccelerationDict":
                    case "DeviceRotationRateDict":
                    case "MediaStreamError":
                    case "DeviceAcceleration":
                    case "DeviceRotationRate":
                    case "ExtensionScriptApis":
                    case "ErrorEventHandler":
                    case "SpeechSynthesis":
                    case "SpeechSynthesisVoice":
                    case "MSCredentials":
                    case "EventListenerOptions":
                    case "ImageBitmapOptions":
                        arg = arg.withDepth(1);
                        break; // Testing manually.
                    default:
                        throw new RuntimeException(info.typeNames.get(t));
                }
            }

            if (info.freeGenericsFinder.hasThisTypes(t)) {
                arg = arg.withContext(arg.typeContext.withThisType(t));
            }

            List<TypeCheck> result = new ArrayList<>();

            if (!t.getDeclaredCallSignatures().isEmpty() || !t.getDeclaredConstructSignatures().isEmpty()) {
                result.add(
                        new SimpleTypeCheck(
                                Check.typeOf("function"),
                                "function"
                        )
                );
            } else {
                result.add(
                        new SimpleTypeCheck(
                                Check.or(
                                        Check.typeOf("function"),
                                        Check.typeOf("object")
                                ),
                                "(function or object)"
                        )
                );
            }

            result.add(new SimpleTypeCheck(Check.not(Check.equalTo(nullLiteral())), "not null"));

            // Adding all baseTypes
            for (Type base : t.getBaseTypes()) {
                result.addAll(base.accept(this, arg));
            }

            if (arg.depthRemaining > 0) {
                Arg subArg = arg.decreaseDepth();
                for (Map.Entry<String, Type> entry : t.getDeclaredProperties().entrySet()) {
                    List<TypeCheck> fieldChecks = entry.getValue().accept(this, subArg);
                    if (!fieldChecks.isEmpty()) {
                        result.add(new FieldTypeCheck(entry.getKey(), fieldChecks));
                    }
                }

                if (t.getDeclaredNumberIndexType() != null) {
                    Type indexType = t.getDeclaredNumberIndexType();

                    TypeCheck indexCheck = createIntersection(indexType.accept(this, subArg));

                    result.add(new SimpleTypeCheck(Check.numberIndex(indexCheck.getCheck()), "(numberIndexer: " + indexCheck.getExpected() + ")"));
                }
                if (t.getDeclaredStringIndexType() != null) {
                    Type indexType = t.getDeclaredStringIndexType();

                    TypeCheck indexCheck = createIntersection(indexType.accept(this, subArg));

                    result.add(new SimpleTypeCheck(Check.stringIndex(indexCheck.getCheck()), "(stringIndexer: " + indexCheck.getExpected() + ")"));
                }
            }

            return result;
        }

        @Override
        public List<TypeCheck> visit(ReferenceType t, Arg arg) {
            if ("Array".equals(info.typeNames.get(t.getTarget()))) {
                Type indexType = t.getTypeArguments().get(0);
                return checkArrayThinghy(indexType, "Array", arg);
            } else if ("ArrayLike".equals(info.typeNames.get(t.getTarget()))) {
                Type indexType = t.getTypeArguments().get(0);
                return checkArrayThinghy(indexType, null, arg);
            }

            if (info.nativeTypes.contains(t) && !(info.typeNames.get(t) != null && info.typeNames.get(t).startsWith("window."))) {
                throw new RuntimeException(info.typeNames.get(t));
            }
            if (info.nativeTypes.contains(t.getTarget()) && !(t.getTarget() instanceof TupleType) && !(info.typeNames.get(t) != null && info.typeNames.get(t).startsWith("window."))) {
                throw new RuntimeException(info.typeNames.get(t));
            }
            return t.getTarget().accept(this, arg.withContext(arg.typeContext.append(new TypesUtil(info).generateParameterMap(t))));
        }

        private List<TypeCheck> checkArrayThinghy(Type indexType, String instance, Arg arg) {
            List<TypeCheck> result = new ArrayList<>();
            result.add(expectNotNull());
            if (instance != null) {
                result.add(new SimpleTypeCheck(Check.instanceOf(identifier(instance)), instance));
            }

            if (arg.depthRemaining > 0 && !info.bench.options.disableGenerics) {
                arg = arg.decreaseDepth();
                TypeCheck indexCheck = createIntersection(indexType.accept(this, arg));

                result.add(
                        new SimpleTypeCheck(Check.numberIndex(indexCheck.getCheck()), "(arrayIndex: " + indexCheck.getExpected() + ")")
                );
            }

            return result;
        }

        @Override
        public List<TypeCheck> visit(SimpleType t, Arg arg) {
            if (t.getKind() == SimpleTypeKind.Any) {
                return Collections.singletonList(
                        new SimpleTypeCheck(Check.alwaysTrue(), "[any]")
                );
            }

            if (t.getKind() == SimpleTypeKind.Void) {
                return Collections.singletonList(
                        new SimpleTypeCheck(Check.alwaysTrue(), "[any(void)]")
                );
            }

            if (info.options.combineNullAndUndefined && (t.getKind() == SimpleTypeKind.Null || t.getKind() == SimpleTypeKind.Undefined)) {
                return Collections.singletonList(
                        new SimpleTypeCheck(
                                Check.or(
                                        Check.equalTo(nullLiteral()),
                                        Check.typeOf("undefined")
                                ),
                                "(null or undefined)"
                        )
                );
            }

            if (t.getKind() == SimpleTypeKind.Null) {
                return Collections.singletonList(
                        new SimpleTypeCheck(Check.equalTo(nullLiteral()), "null")
                );
            }
            if (t.getKind() == SimpleTypeKind.Never) {
                return Collections.singletonList(new SimpleTypeCheck(
                        Check.equalTo(object()), "never" // equalTo check with a newly constructed object will always fail.
                ));
            }
            if (t.getKind() == SimpleTypeKind.Object) {
                return Collections.singletonList(new SimpleTypeCheck(
                        Check.or(Check.typeOf("object"), Check.typeOf("function")), "object"
                ));
            }
            String typeOf = getTypeOf(t);
            return Collections.singletonList(new SimpleTypeCheck(Check.typeOf(typeOf), typeOf));
        }

        @Override
        public List<TypeCheck> visit(TupleType tuple, Arg arg) {
            int size = tuple.getElementTypes().size();
            List<TypeCheck> result = new ArrayList<>(Arrays.asList(
                    expectNotNull(),
                    new SimpleTypeCheck(Check.instanceOf(identifier("Array")), "tuple"),
                    new SimpleTypeCheck(Check.field("length", Check.expression((actualSize) ->
                            binary(actualSize, Operator.GREATER_THAN_EQUAL, number(size))
                    )), "tuple of " + size + " elements")
            ));

            if (arg.depthRemaining > 0) {
                arg = arg.decreaseDepth();
                for (int i = 0; i < size; i++) {
                    List<TypeCheck> subCheck = tuple.getElementTypes().get(i).accept(this, arg);
                    if (!subCheck.isEmpty()) {
                        result.add(new FieldTypeCheck(Integer.toString(i), subCheck));
                    }
                }
            }

            return result;
        }

        @Override
        public List<TypeCheck> visit(UnionType t, Arg arg) {
            return Collections.singletonList(
                    createUnionCheck(t.getElements().stream().map(subType -> subType.accept(this, arg)).collect(Collectors.toList()))
            );
        }

        @Override
        public List<TypeCheck> visit(TypeParameterType parameter, Arg arg) {
            TypeContext typeContext = arg.typeContext;

            if (typeContext.containsKey(parameter)) {
                if (arg.depthRemaining <= -1) { // One level deeper than normal property-accesses, on purpose.
                    return Collections.emptyList();
                }
                arg = arg.decreaseDepth();
                TypeWithContext lookup = typeContext.get(parameter);
                return lookup.getType().accept(this, arg.withContext(lookup.getTypeContext()));
            }

            List<TypeCheck> checks = new ArrayList<>(parameter.getConstraint() != null ? parameter.getConstraint().accept(this, arg) : Collections.emptyList());

            String markerField = info.typeParameterIndexer.getMarkerField(parameter);

            checks.add(expectNotNull());

            if (parameter.getConstraint() == null || !(parameter.getConstraint() instanceof InterfaceType) || ((InterfaceType) parameter.getConstraint()).getDeclaredStringIndexType() == null) {
                checks.add(new SimpleTypeCheck(
                        Check.field(markerField, Check.equalTo(bool(true))),
                        "a generic type marker (." + markerField + ")"
                ));
            }


            return checks;
        }

        @Override
        public List<TypeCheck> visit(StringLiteral t, Arg arg) {
            return Collections.singletonList(
                    new SimpleTypeCheck(Check.equalTo(string(t.getText())), "\"" + t.getText() + "\"")
            );
        }

        @Override
        public List<TypeCheck> visit(BooleanLiteral t, Arg arg) {
            return Collections.singletonList(
                    new SimpleTypeCheck(Check.equalTo(bool(t.getValue())), Boolean.toString(t.getValue()))
            );
        }

        @Override
        public List<TypeCheck> visit(NumberLiteral t, Arg arg) {
            return Collections.singletonList(
                    new SimpleTypeCheck(Check.equalTo(number(t.getValue())), Double.toString(t.getValue()))
            );
        }

        @Override
        public List<TypeCheck> visit(IntersectionType t, Arg arg) {
            return t.getElements()
                    .stream()
                    .map(subType -> subType.accept(this, arg))
                    .reduce(new ArrayList<>(), Util::reduceList);
        }

        @Override
        public List<TypeCheck> visit(ClassInstanceType t, Arg arg) {
            return ((ClassType) t.getClassType()).getInstanceType().accept(this, arg);
        }

        @Override
        public List<TypeCheck> visit(ThisType t, Arg arg) {
            if (arg.typeContext.getThisType() == null) {
                System.out.println();
            }
            return arg.typeContext.getThisType().accept(this, arg);
        }

        @Override
        public List<TypeCheck> visit(IndexType t, Arg arg) {
            return Collections.singletonList(new SimpleTypeCheck(Check.alwaysTrue(), "indexType(ignored)"));
        }

        @Override
        public List<TypeCheck> visit(IndexedAccessType t, Arg arg) {
            return Collections.singletonList(new SimpleTypeCheck(Check.alwaysTrue(), "indexedAccessType(ignored)"));
        }
    }

    private static TypeCheck createUnionCheck(List<List<TypeCheck>> checksLists) {
        assert !checksLists.isEmpty();
        if (checksLists.size() == 1) {
            return createIntersection(checksLists.iterator().next());
        }

        List<TypeCheck> checks = checksLists.stream().map(TypeChecker::createIntersection).collect(Collectors.toList());

        StringBuilder expected = new StringBuilder("(");
        for (int i = 0; i < checks.size(); i++) {
            expected.append(checks.get(i).getExpected());
            if (i != checks.size() - 1) {
                expected.append(" or ");
            }
        }
        expected.append(")");

        Check check = Check.or(checks.stream().map(TypeCheck::getCheck).collect(Collectors.toList()));

        return new SimpleTypeCheck(check, expected.toString());
    }

    static TypeCheck createIntersection(List<TypeCheck> checks) {
        if (checks.isEmpty()) {
            return new SimpleTypeCheck(Check.alwaysTrue(), "[any]");
        }
        if (checks.size() == 1) {
            return checks.iterator().next();
        }
        String expected = createIntersectionDescription(checks);

        Check check = Check.and(checks.stream().map(TypeCheck::getCheck).collect(Collectors.toList()));

        return new SimpleTypeCheck(check, expected);
    }

    public static String createIntersectionDescription(List<TypeCheck> checks) {
        if (checks.size() == 1) {
            return checks.iterator().next().getExpected();
        }
        StringBuilder expected = new StringBuilder("(");
        for (int i = 0; i < checks.size(); i++) {
            expected.append(checks.get(i).getExpected());
            if (i != checks.size() - 1) {
                expected.append(" and ");
            }
        }
        expected.append(")");
        return expected.toString();
    }

    private static String getTypeOf(SimpleType type) {
        switch (type.getKind()) {
            case String:
                return "string";
            case Number:
            case Enum:
                return "number";
            case Boolean:
                return "boolean";
            case Void:
            case Undefined:
                return "undefined";
            case Symbol:
                return "symbol";
            default:
                throw new RuntimeException(type.getKind().toString());
        }
    }

    private static TypeCheck expectNotNull() {
        return new SimpleTypeCheck(
                Check.and(
                        Check.not(Check.typeOf("undefined")),
                        Check.not(Check.equalTo(nullLiteral()))
                ),
                "a non null value"
        );
    }
}
