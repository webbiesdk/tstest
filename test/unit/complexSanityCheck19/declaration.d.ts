export interface ActionCreator<A> {}
export interface ActionCreatorsMapObject {
    [key: string]: ActionCreator<any>;
}
declare module module {
    function bindActionCreators<M extends ActionCreatorsMapObject,N extends ActionCreatorsMapObject>(actionCreators: M): N;
}