declare module module {
    function foo(arg: Foo<void>): true;
}

interface Foo<T> {
    bar: Foo<T & {foo: string}>;
    baz: T;
}

type test = Foo<void> & {foo: string};