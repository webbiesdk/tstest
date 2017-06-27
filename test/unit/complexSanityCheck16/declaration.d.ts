interface ChainableBase {
    at<T>(index: void|Array<number>): ChainableBase;
    chars<T>(eachCharFn?: (arr: Array<string>) => ChainableBase): void;
}
declare var module: ChainableBase;