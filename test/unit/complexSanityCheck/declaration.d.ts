declare namespace module {
    class SortedSet<T> {
        toArray():T[];
        union(otherSet:SortedSet<T>): void;
    }

    function load<T>():SortedSet<T>;

}
