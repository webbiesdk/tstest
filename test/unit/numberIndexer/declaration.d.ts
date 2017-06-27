
export module module {
    function foo() : {[index: number]: number} // <- contains implementation error
    function bar() : {[index: number] : boolean[]} // <- Is correct.
}