/*
export module module {
    interface Foo {
        foo: this;
    }
    interface Bar extends Foo {
        bar: this;
    }

    function baz(): Bar;
}*/

var Foo = function () {
    this.foo = function () {
        return new Foo(); // <- Wrong.
    }
};
var Bar = function () {
    Foo.call(this);
    this.bar = this;
};
module.exports = {
    Foo: Foo,
    Bar: Bar,
    baz: function () {
        return new Bar();
    }
};