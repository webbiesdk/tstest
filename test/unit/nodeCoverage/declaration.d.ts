declare module module {
    function memoize(fn: Function, hasher?: Function): Function;
    function unmemoize(fn: Function): Function;
}