declare module module {
    var foo: L1<true>;
    var bar: L1<false>;
}
interface L1<T> {
    foo: L2<T & true>;
    bar: L2<T & false>;
}
interface L2<T> {
    foo: L3<T & true>;
    bar: L3<T & false>;
}
interface L3<T> {
    foo: L4<T & true>;
    bar: L4<T & false>;
}
interface L4<T> {
    foo: L5<T & true>;
    bar: L5<T & false>;
}
interface L5<T> {
    foo: L6<T & true>;
    bar: L6<T & false>;
}
interface L6<T> {
    foo: L1<T & true>;
    bar: L1<T & false>;
    get: T;
}