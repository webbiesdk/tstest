declare namespace MyModule {
    type TypeRef<T> = (instance: T) => string;
    function createElement<T>(): TypeRef<T>;
}