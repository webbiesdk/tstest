interface KnockoutSubscribable<T> {
    subscribe(callback: (newValue: T) => void): void;
}
interface KnockoutObservableArray<T> extends KnockoutObservable<T[]> {
    subscribe(callback: (newValue: T[]) => void): void;
}
interface KnockoutObservable<T> extends KnockoutSubscribable<T>{}
declare module module {
    function arrayPushAll(array: KnockoutObservableArray<boolean>): KnockoutObservableArray<boolean>;
}