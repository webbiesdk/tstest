interface JQueryXHR extends JQueryGenericPromise<any> {
    then<R>(foo: () => any): void;
    then<R>(foo: null): void;
}
interface JQueryGenericPromise<T> {
    then<U>(a: () => void, b: true): void;
}
declare function module(): JQueryXHR;