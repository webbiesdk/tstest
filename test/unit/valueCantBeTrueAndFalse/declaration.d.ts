
export module module {
    function foo(a: true & false): never; // <- The argument is impossible, which throws an RuntimeError, which should give an error back to me. 
}