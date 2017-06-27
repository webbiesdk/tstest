interface Container<T> {
    value: T,
    bar: this
}

export module module {
    function foo(): Container<string>;
}