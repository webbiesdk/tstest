/*
export module module {
    interface Container<T> {
        value: T;
    }
    function one<T>(t: T): Container<T>;
    function two<T>(t: T): Container<T>;
    function three<T>(t: T): Container<T>;
}*/

function Container(t) {
    this.value = t;
}
Container.create = function (a) {
    return new Container("foo"); // <- Wrong!
};


module.exports = {
    Container: Container,
    one: function () {
        return new Container("String");
    },
    two: function () {
        return new Container("String");
    },
    three: function () {
        return new Container("String");
    }
};