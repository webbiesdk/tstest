declare var module : Foo<void>;

interface Foo<T> {
    bar(): Foo<T & string>;
    baz: T;
}