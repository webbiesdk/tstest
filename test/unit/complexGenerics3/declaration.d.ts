export type Reducer<S> = (state: S) => S;
export interface StoreCreator {
    <S>(s: S): Reducer<S>;
}
export const createStore: StoreCreator;