declare module module {
    class Disposer<R> {
        constructor(value: R);
        value: R;
    }

    function using<R>(disposer: Disposer<R>): true; // <- returns false
}