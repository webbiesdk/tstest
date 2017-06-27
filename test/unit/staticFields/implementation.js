/*
export module module {
    class Foo {
        static foo: true;
        static bar: true;
    }
}*/

var Foo = function () {};
Foo.foo = false; // <- Wrong
Foo.bar = true; // <- Right!

module.exports = {
    Foo: Foo
};