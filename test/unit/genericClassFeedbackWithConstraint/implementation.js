/*
declare module module {
    class Foo<T> {
        constructor(t: T);
        value: T;
    }

    interface Bar {
        marker: true;
    }

    function gen<T extends Bar>(t: T): Foo<T>;

    function returnsFalse<T>(foo: Foo<T>): true;
}*/

function Foo(t) {
    this.value = t;
    this.private = true;

}

module.exports = {
    Foo: Foo,
    gen: function (t) {
        return new Foo(t);
    },
    returnsFalse: function (foo) {
        if (!foo) {
            return "santityCheck gone wrong";
        }
        if (!foo.private) {
            return "sanityCheck private failed!";
        }
        if (!foo.value) {
            return "santityCheck gone wrong2"
        }
        if (!foo.value.marker) {
            return "santityCheck gone wrong3"
        }
        return false;
    }
};