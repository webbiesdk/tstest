/*
interface Foo {
    bar: number;
}
declare module module {
    function gen(): Foo;
    function test(a: Foo): true;
}
*/


module.exports = {
    gen: function () {
        return {
            bar: 123,
            mySpecialMarker: true // <- With this one, i will know the value came from here.
        }
    },
    test: function (a) {
        if (!a.mySpecialMarker) {
            return true;
        }
        return a.bar == 123;
    }
};