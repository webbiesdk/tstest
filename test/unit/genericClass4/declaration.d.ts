export module module {
    class Container<T> {
        constructor(t : T);
        value: T;
    }
    let stringContainer: Container<string>;
}