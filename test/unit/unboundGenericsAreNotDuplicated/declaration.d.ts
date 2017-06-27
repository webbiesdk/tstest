export module module {
    interface Container<T> {
        value: T;
    }
    function one<T>(t: T): Container<T>;
    function two<T>(t: T): Container<T>;
    function three<T>(t: T): Container<T>;
}