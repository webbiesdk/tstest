/*export module module {
    function foo(a: true, b?: true, c?: 1 | 2 | 3, ...d: string[]): true;
}*/

var counter = 0;
var maxArgs = -1;

module.exports = {
    foo: function (a, b, c) {
        maxArgs = Math.max(maxArgs, arguments.length);
        counter++;

        if (counter >= 500) {
            if (maxArgs <= 4) {
                return "maxArgs: " + maxArgs;
            }
        }

        if (arguments.length === 0) {
            return "no arguments";
        }
        if (!a) {
            return "not a";
        }
        if (arguments.length >= 2) {
            if (!b) {
                return "not b";
            }
        }
        if (arguments >= 3) {
            if (typeof c !== "number") {
                return "c was not a number";
            }
        }
        if (arguments.length >= 4) {
            for (var i = 3; i < arguments.length; i++) {
                if (typeof arguments[i] !== "string") {
                    return "rest arg not string. "
                }
            }
        }
        return true;
    }
};