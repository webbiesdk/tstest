interface Foo {
    bar: {
        baz: true // <- sometimes false
    }
}

export module module {
    function foo() : Foo[];
}