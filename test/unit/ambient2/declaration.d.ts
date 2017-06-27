interface Foo {
    str: "123";
}

declare let foo: Foo;
declare module "bar" {
    export = foo;
}