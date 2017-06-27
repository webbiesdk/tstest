declare module module {
    class Foo<T> {
        constructor(t: T);
        value: T;
    }
    function returnsFalse<T>(foo: Foo<T>): true;
}