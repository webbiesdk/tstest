// Based on lunr.d.ts
declare namespace module {
    var store: Store<string>;

    class Store<T> {
        value: T;
    }
}