/*
export module module {
    function foo(a: string, ...b: number[]): true;
}
*/

var b3WasHit = false;
var runs = 0;

module.exports = {
    foo: function (str, b1, b2, b3, b4) {
        runs++;
        if (b1 instanceof Array) {
            return "not array";
        }

        if (b3) {
            b3WasHit = true;
        }

        if (runs > 20) {
            return b3WasHit || "b3WasNotHit";
        }

        return true;
    }
};