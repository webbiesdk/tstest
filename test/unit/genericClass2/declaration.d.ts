// Based on lunr.d.ts
declare namespace module {
    class Index {
        constructor();
        store: Store<string>;
    }

    class Store<T> {
        constructor(t: T);
        value: T;
    }
}