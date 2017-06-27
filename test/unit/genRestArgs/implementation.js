/*
interface Foo {
    (a: string, ...b: number[]): true;
}

export module module {
    function foo(a: Foo): void;
}
*/


module.exports = {
    foo: function (callback) {
        callback("string", 1); // <- good
        callback("string", 1, 4); // <- good
        callback("string", 1, 4, 7); // <- good
        callback("string", 1, 4, 7, false); // <- BAD!
    }
};