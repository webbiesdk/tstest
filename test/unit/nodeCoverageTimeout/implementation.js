/*
declare module async {
    function memoize(fn: Function, hasher?: Function): Function;
    function unmemoize(fn: Function): Function;
}*/

var counter = 0;
function stop() {
    counter++;
    if (counter > 10) {
        while(true) {
            // Do nothing.
        }
    }
}

module.exports = {
    memoize: function (fn, hasher) {
        stop();
        hasher = hasher ||
                 function (a) {return JSON.stringify(a)};
        var cache = {};
        var result = function () {
            var key = hasher(arguments);
            return cache[key] ||
                (cache[key] = fn.apply(this, arguments));
        };
        result.unmemoized = fn;
        return result
    },
    unmemoize: function (fn) {
        stop();
        return fn.unmemoized || fn;
    }
};