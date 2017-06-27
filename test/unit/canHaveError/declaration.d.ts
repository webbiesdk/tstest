
export module module {
    function foo(a: true & {a: true} & number & string): never; // <- It is very impossible to construct that argument.  
}