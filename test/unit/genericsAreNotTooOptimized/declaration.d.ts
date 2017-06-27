declare var module: Foo<false, false>;

interface Foo<T0, T1> {
    mark: Foo<true, T1>;
    get: T1;
}