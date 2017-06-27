interface Foo {
    (a: string, ...b: number[]): true;
}

export module module {
    function foo(a: Foo): void;
}