interface KnockoutObservableArray extends KnockoutObservable {
    subscribe<T>(): T;
}

interface KnockoutObservable {
    subscribe<T>(): T;
}

declare var module: KnockoutObservableArray;