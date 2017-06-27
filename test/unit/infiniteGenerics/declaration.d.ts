
export module module {
    interface Foo<T> {
        bar: Foo<T[]>;
        value: T
    }
    function test(x: Foo<true>): never;
}