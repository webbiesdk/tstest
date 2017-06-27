
export module module {
    interface CustomArray extends Array<string[]>{
        marker: true;
    }

    function bar(c : CustomArray): true;
}