declare module module {
    export class Foo<T> {
        foo: T;
    }
    export class Bar extends Foo<true> {}
}