interface Base<T> extends Functions<T> {}

interface Functions<T> {
    slice: T;
    reverse: Base<T>;
}

declare var module: Base<void>;