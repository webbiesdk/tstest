interface Container<T> {
    value: T
}

export module module {
    function foo(): Container<string>;
}