interface Foo {
    value: number[];
}
declare module module {
    function gen(): Foo;
    function test(a: Foo): true;
}