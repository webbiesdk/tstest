declare module module {
    class Bluebird<R> {
        constructor(value: R);
        nodeify(callback: (err: any, value?: R) => void): this;
        static each<R>(): Bluebird<R[]>;
    }
}