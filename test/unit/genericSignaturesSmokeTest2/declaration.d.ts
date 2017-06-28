declare function createFactory<P>(): ClassType<P>;
type ClassType<P> = (new (a: number) => true);