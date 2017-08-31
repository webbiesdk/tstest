/*
declare module module {
    class Foo {
        constructor(n: number);
        passed: true;
    }
    class Bar extends Foo {
        // no constructor, in this case, the constructor is inherited from Foo.
    }
    class Baz extends Foo {
        constructor(s: string) // this overrides the one in Foo.
    }
    function foo(foo: typeof Foo, bar: typeof Bar, baz: typeof Baz): true;
}
*/


var counter = 0;
var hasSeenConstructed = false;

var Foo = function (n) {
    this.passed = typeof n === "number";
};
var Bar = function (n) {
    this.passed = typeof n === "number";
};
var Baz = function (n) {
    this.passed = typeof n === "string";
};
module.exports = {
    Foo: Foo,
    Bar: Bar,
    Baz: Baz,
    foo: function (foo, bar, baz) {
        counter++;
        if (foo !== Foo && foo !== Bar && foo !== Baz) {
            hasSeenConstructed = true;
        }
        if (!foo || !bar || !baz) {
            return "an argument was not defined";
        }
        if (counter > 100 && !hasSeenConstructed) {
            return "did not see a constructed value in 100 tries";
        }
        if (foo !== Foo && foo !== Bar && foo !== Baz) {
            new foo(123); // <- valid constructor call
        }

        if (baz !== Baz) {
            new baz("string"); // <- valid constructor call

            new baz(123); // invalid constructor call
        }

        if (bar !== Bar) {
            new bar(123); // valid constructor call

            new bar(); // invalid constructor call.
        }
        return true;
    }
};