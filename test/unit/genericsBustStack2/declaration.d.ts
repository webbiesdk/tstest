declare var module: _.LoDashImplicitObjectWrapper<void>;
declare namespace _ {
    interface LoDashImplicitObjectWrapper<T> {
        merge(): LoDashImplicitObjectWrapper<T & void>;
        identity(): T;
    }
}