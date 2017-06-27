declare namespace module {
    class Foo {
        foo(): this;
        mod: {
            stuff: true
        }
    }

    class Bar extends Foo {
        bar(): string;
    }
}