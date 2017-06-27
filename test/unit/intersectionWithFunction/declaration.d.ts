declare module module {
    interface Cancelable {
        cancel(): void;
    }

    function throttle<T extends Function>(
        func: T,
        wait: number): T & Cancelable;
}