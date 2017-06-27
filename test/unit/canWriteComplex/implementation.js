/*
interface Foo {
    value: number[];
}
declare module module {
    function gen(): Foo;
    function test(a: Foo): true;
}*/

module.exports = {
    gen: function () {
        return {
            value: [1],
            mySpecialMarker: true // <- With this one, i will know the value came from here.
        }
    },
    test: function (a) {
        if (!a.mySpecialMarker) {
            return true;
        }
        return a.value[0] == 1;
    }
};