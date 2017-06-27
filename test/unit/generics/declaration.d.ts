interface Container<T> {
    value: {
        foo: T;
    }
}

export module module {
    function foo(): Container<string>;
}