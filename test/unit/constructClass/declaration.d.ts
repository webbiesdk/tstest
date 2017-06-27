declare namespace module {
    function foo(arg: typeof Foo): "foo";

    class Foo {
        stuff(): "foo";
    }
}