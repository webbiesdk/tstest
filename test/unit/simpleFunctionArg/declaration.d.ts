
interface Container<T> {
    value: T
}

export module module {
    function foo(callback: (str: Container<string>) => boolean): boolean;
}