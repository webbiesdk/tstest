
export module module {
    interface Foo {
        foo(): this;
    }
    interface Bar extends Foo {
        bar: this;
    }

    function baz(): Bar;
}