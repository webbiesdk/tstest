/*
declare namespace module {
    class Foo {
        foo(): this;
        mod: {
            stuff: true
        }
    }

    class Bar extends Foo {
        bar(): string;
    }
}*/


function Foo() {
    this.foo = function () {
        return this;
    };
    this.mod = {
        stuff: false // <- WRONG
    };
}
function Bar() {
    Foo.call(this);
}
Bar.prototype = Object.create(Foo);
Bar.prototype.bar = function () {
    return "string";
};

module.exports = {
    Foo: Foo,
    Bar: Bar
};