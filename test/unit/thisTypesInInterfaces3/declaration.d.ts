interface Container<T> {
    value: T & {b: true},
    bar: this
}

export module module {
    function foo(): Container<{a: true}>;
}