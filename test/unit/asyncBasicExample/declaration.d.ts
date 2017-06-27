interface Async {
    memoize(fn: Function, hasher?: Function): Function;
    unmemoize(fn: Function): Function;
}
declare var module: Async;