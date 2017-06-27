/*
interface Foo {
    str: "123";
}

declare let foo: Foo;
declare module "bar" {
export = foo;
}*/


module.exports = {
    str: "456" // <- is wrong
};