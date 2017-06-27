/*
interface Foo {
    str: "123";
}

declare let foo: Foo;
declare module "bar" {
    const num: 123;
    export = foo;
}*/


module.exports = {
    str: "456", // <- is wrong
    num: 456 // <- also wrong
};