
export module module {
    function foo() : {[index: string]: number} // <- contains implementation error
    function bar() : {[index: string] : boolean[]} // <- Is correct.
}