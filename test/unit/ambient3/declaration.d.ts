interface Foo {
    str: "123";
}

declare let foo: Foo;
declare module "bar" {
    const num: 123;
    export = foo;
}