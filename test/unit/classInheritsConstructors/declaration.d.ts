declare module module {
    class Foo {
        constructor(n: number);
        passed: true;
    }
    class Bar extends Foo {
        // no constructor, in this case, the constructor is inherited from Foo.
    }
    class Baz extends Foo {
        constructor(s: string) // this overrides the one in Foo.
    }
    function foo(foo: typeof Foo, bar: typeof Bar, baz: typeof Baz): true;
}
