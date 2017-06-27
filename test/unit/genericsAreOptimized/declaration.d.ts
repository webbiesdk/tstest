interface Container<T> {
    value: T,
    flagObj: {
        flag: boolean
    }
}

export module module {
    function foo(): Container<string>;
    function bar(): Container<number>;
}