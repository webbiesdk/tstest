/*
declare module module {
    class Bluebird<R> {
        nodeify(callback: (err: any, value?: R) => void): this;
        static each<R>(): Bluebird<R[]>;
    }
}*/


// This actually satisfies the declaration.
var Bluebird = function (value) {
    this.nodeify = function (callback) {
        callback(null, value);
        return this;
    }
};
Bluebird.each = function () {
    return new Bluebird([]);
};

module.exports = {
    Bluebird: Bluebird
};