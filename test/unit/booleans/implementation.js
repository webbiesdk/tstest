/*
interface CustomBoolean {
    quz: true;
}

export module module {
    var foo: Boolean;
    var bar: boolean;
    var baz: CustomBoolean;
}*/

var res = new Boolean(true);
res.quz = true;

module.exports = {
    foo: new Boolean(true),
    bar: true,
    baz: res
};