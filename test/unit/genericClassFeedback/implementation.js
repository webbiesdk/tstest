/*
declare module module {
    class Foo<T> {
        constructor(t: T);
    value: T;
}
    function returnsFalse<T>(foo: Foo<T>): true;
}*/

function Foo(t) {
    this.value = t;
    this.private = true;
}

module.exports = {
    Foo: Foo,
    returnsFalse: function (foo) {
        if (!foo.private) {
            return "sanityCheck failed!";
        }
        return false;
    }
};