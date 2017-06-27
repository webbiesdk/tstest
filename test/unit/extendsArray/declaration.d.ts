
export module module {
    function foo() : CustomArray;

    interface CustomArray extends Array<string>{
        marker: true;
    }

    function bar(c: CustomArray): true;
}