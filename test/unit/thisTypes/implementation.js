/*declare namespace module {
    class Foo {
        foo(): this;
    }

    class Bar extends Foo {
        bar(): string;
    }
}*/
function Foo() {
    this.foo = function () {
        return new Foo(); // <- Wrong, doesn't return "this".
    }
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