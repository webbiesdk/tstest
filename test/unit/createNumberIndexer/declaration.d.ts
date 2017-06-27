interface Foo {
    [index: number]: number
}

interface Bar extends Foo {
    bar : string;
}


export module module {
    function id(a: Bar): Bar;
    function foo(a: Foo): Foo;
}