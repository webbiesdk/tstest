declare var module : LoDashStatic<{}>;
interface LoDashStatic<T> {
    throttle: T & Cancelable;
    functionsIn: ("foo")[];
}
interface Cancelable {
    cancel: void;
}