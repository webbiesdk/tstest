declare namespace module {
    class Foo {
        foo(): this;
    }

    class Bar extends Foo {
        bar(): string;
    }
}