var Store = function (value) {
    this.value = value;
};
module.exports = {
    Store: Store,
    Index: function () {
        this.store = new Store(123); // <- Not a store of String.
    }
};