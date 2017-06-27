export module module {
    class Container<T> {
        constructor(t: T);
        value: T;
    }
    function createNumberContainer(num: number): Container<number>;
}