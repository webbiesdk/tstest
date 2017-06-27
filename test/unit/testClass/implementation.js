/*
export module module {
    class Foo {
        field: number;
    }
    function test(foo: Foo): never;
}*/

module.exports = {
    Foo: function () {
        this.field = 123;
    },
    test: function (foo) {
        return foo; // <- Actually returns normally, this is a test that it is ever executed.
    }
};