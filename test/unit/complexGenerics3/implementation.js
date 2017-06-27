/*export type Reducer<S> = (state: S) => S;
export interface StoreCreator {
<S>(): Reducer<S>;
}
export const createStore: StoreCreator;*/


module.exports = function (y) {
    return function (x) {
        return x; // doesn't fail if i return "y" instead, although they should be the same type.
    }
};