/*
interface Foo {
    bar: {
        baz: true // <- sometimes false
    }
}

export module module {
    function foo() : Foo[];
}*/


module.exports = {
    foo: function () {
        var res = {
            bar: {
                baz: Math.random() > 0.1
            }
        };
        return [res, res, res];
    }
};