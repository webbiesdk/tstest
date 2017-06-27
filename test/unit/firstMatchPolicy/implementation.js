/*
declare module module {
    // Should both be matched, assert that after 100 runs.
    function foo(x: true): true;
    function foo(x: false): true;

    // This just returns !!x.marker2.
    function bar(x: B): true;
    function bar(x: A): false;

    // This always returns true
    function baz(x: A): true;
    function baz(x: A): false;

    class A {
        marker1: true;
    }
    class B extends A {
        marker2: true;
    }
    class C extends A {
        maker3: true;
    }
}
*/

module.exports = {
    A: function () {
        this.marker1 = true;
    },
    B: function () {
        this.marker1 = true;
        this.marker2 = true;
    },
    C: function () {
        this.marker1 = true;
        this.marker3 = true;
    },
    bar: function (x) {
        return !!x.marker2;
    },
    baz: function (x) {
        return false;
    },

    foo: (function () {
        var runs = 0;
        var trueHit = false;
        var falseHit = false;
        return function (x) {
            runs++;
            if (x) {
                trueHit = true;
            } else {
                falseHit = true;
            }
            if (runs < 100) {
                return true;
            } else {
                return trueHit && falseHit;
            }
        }
    })()
};