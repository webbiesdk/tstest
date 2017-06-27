/*
declare module async {
    function memoize(fn: Function, hasher?: Function): Function;
    function unmemoize(fn: Function): Function;
}*/

module.exports = {
    memoize: function (fn, hasher) {
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
        return fn.unmemoized || fn;
    }
};