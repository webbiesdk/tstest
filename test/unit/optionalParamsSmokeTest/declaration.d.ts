declare namespace React {
    function createClass<P, S>(): ClassicComponentClass<P>;
    interface ClassicComponentClass<P>  {
        (props?: P, context?: any): void;
    }
}