interface KnockoutObservableArrayFunctions<T> {
    reverse: KnockoutObservableArray<T>;
}
interface KnockoutObservableArray<T> extends  KnockoutObservableArrayFunctions<T> {}
declare var module: KnockoutObservableArrayFunctions<void>;