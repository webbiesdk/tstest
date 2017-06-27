declare module module {
    interface Bar {
        marker: true;
    }

    function gen<T extends Bar>(t: T): Foo<T>;

    function returnsFalse<T extends Bar>(foo: Foo<T>): true;

    class Foo<T> {
        constructor(t: T);
        value: T;
    }
}