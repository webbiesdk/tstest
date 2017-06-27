/*
interface Foo {
    (a: string, ...b: number[]): true;
    (a: number, ...b: boolean[]): true;
    (a: boolean, ...b: string[]): true;
}

export module module {
    function foo(a: Foo): void;
}*/

module.exports = {
    foo: function (callback) {
        callback("string", 1, 4, 7); // <- good
        callback("string", 1, 4, 7, false); // <- BAD!

        callback(123, true, false); // <- good
    },

    // All good here.
    bar: function (callback) {
        callback("string", 1, 4); // <- good
        callback("string", 1, 4, 7); // <- good

        callback(123, false); // <- good
        callback(123, true, false); // <- good
    }
};