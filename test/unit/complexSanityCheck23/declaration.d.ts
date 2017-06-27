interface Foo {
    forEach<T>(eachFn: number): ErrorConstructor;
}
interface Bar {
    forEach<T>(search?: string): ErrorConstructor;
}

declare var Sugar: Foo & Bar;