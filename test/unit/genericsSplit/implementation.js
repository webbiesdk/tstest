/*declare module module {
    class Disposer<R> {
        constructor(value: T);
        value: R;
    }

    function using<R>(disposer: Disposer<R>): true; // <- returns false
}*/



module.exports = {
    Disposer: function (value) {
        this.value = value;
    },
    using: function () {
        return false;
    }
};