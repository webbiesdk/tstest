
export module module {
    function foo(c: (b: boolean) => boolean) : boolean; // <- The callback also recieves undefined
    function bar(c: () => boolean) : boolean;
}