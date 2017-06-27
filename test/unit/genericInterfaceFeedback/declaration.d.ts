declare module module {
    function returnsFalse(arg: Foo<void>): true;
}

interface Foo<T> {
    baz: T;
}