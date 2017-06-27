// Based on lunr.d.ts
declare namespace module  {
    interface SortedSet<T> {
        foo:Container<T>;
    }
    interface Container<T> {
        value: T;
    }
    var documentStore:SortedSet<string>;
    var store:SortedSet<void>;
}