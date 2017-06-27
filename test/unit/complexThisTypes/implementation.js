/*

interface HeadAndTail<T> {
    head: T,
    tail: T
}

declare namespace module {
    class Foo {
        getHead(): this;
        getTail(): this;
        getHeadAndTail(): HeadAndTail<this>;
    }

    class Bar extends Foo {
        bar(): string;
    }
}
*/


function Foo() {
    this.getHead = this.getTail = function () {
        return this;
    };
    this.getHeadAndTail = function () {
        return {
            head: this,
            tail: new Foo() // <- WRONG.
        }
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