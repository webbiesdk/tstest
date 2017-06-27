/*
interface Foo {
    bar: Foo;
    baz: boolean;
}
declare var module: Foo;
*/

module.exports = {
    bar: {
        bar: {
            bar: 123,
            baz: false
        },
        baz: true
    },
    baz: true
};