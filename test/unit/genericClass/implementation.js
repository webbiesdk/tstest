function Container(t) {
    this.value = t;
}
Container.prototype.setValue = function (t) {
    this.value = t;
};
Container.prototype.getValue = function () {
    return this.value;
};
Container.create = function (a) {
    return new Container("foo"); // <- Wrong!
};


module.exports = {
    Container: Container,
    createStringContainer: function (str) {
        return new Container(str);
    },
    createNumberContainer: function (num) {
        return new Container(num);
    }
};