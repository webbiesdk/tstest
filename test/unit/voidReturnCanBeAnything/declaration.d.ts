export module module {
    function foo() : void; // <- actually any. 
    function bar(): boolean | void; // <- Not any.
}