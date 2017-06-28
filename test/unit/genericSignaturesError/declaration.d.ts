declare namespace MyModule {
    type Wlala<T> = (instance: T) => string;
    function createElement<T>(): Wlala<T>;
}