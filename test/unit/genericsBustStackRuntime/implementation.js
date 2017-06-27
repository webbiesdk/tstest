/*
declare module module {
    function foo(arg: Foo<void>): true;
}

interface Foo<T> {
    bar: Foo<T & string>;
    baz: T;
}*/

module.exports = {
    foo: function (arg) {
        return false; // <- Wrong, and this is just a test that the function is ever executed.
    }
};