interface Foo {
    [index: string]: number
}

interface Bar extends Foo {
    bar : 12;
}


export module module {
    function id(a: Bar): Bar;
    function foo(a: Foo): Foo;
}