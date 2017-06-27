interface Foo {
    (a: string, ...b: number[]): true;
    (a: number, ...b: boolean[]): true;
}

interface Bar {
    (a: string, ...b: number[]): true;
    (a: number, ...b: boolean[]): true;
}

export module module {
    function foo(a: Foo): void;
    function bar(a: Bar): void;
}