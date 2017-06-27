interface Container<T> {
    flagObj: {
        flag: boolean
    }
}

export module module {
    function foo(): Container<string>;
}