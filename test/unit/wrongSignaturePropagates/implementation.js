/*
export module module {
    function foo(c: (b: boolean) => boolean) : boolean;
    function bar(c: () => boolean) : boolean;
}*/

module.exports = {
    foo: function (c) {
        return c(true) || c(undefined); // <- The undefined is WRONG.
    },
    bar: function (c) {
        return c();
    }
};