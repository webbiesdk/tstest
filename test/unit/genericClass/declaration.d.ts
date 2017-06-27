export module module {
    class Container<T> {
        constructor(t : T);
        value: T;
        static create<T>(t: T) : Container<T>;
    }
    function createStringContainer(str: string): Container<string>;
    function createNumberContainer(num: number): Container<number>;
}