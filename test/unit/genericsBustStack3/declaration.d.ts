interface Foo<T> {
    groupBy: Bar<T>;
    value: T[];
}
interface Bar<T> {
    flatten: Foo<T>;
}
declare var module: Foo<void>;