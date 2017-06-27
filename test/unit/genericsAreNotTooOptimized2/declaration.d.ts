declare var module: Foo<false, false>;
// TODO: Simplify this, as long as there is a test with no assertions.
interface Foo<T0, T1> {
    mark: Foo<true, T1>;
    get: T1;
}