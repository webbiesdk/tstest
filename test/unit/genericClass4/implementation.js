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
    stringContainer: new Container("string")
};