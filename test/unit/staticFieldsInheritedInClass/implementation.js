/*
declare module module {
    class Foo {
        static marker1: string;
    }
    class Bar extends Foo {
        static marker2: string;
    }
}
*/


var Foo = function () {

};
Foo.marker1 = "string";
var Bar = function () {

};
// Not that Bar does not inherit the static field from Foo.
Bar.marker2 = "otherString";
module.exports = {
    Foo: Foo,
    Bar: Bar
};