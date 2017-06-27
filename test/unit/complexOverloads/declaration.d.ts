interface IPipelineFunction {
    (a: symbol): 0;
    (a: number): 1;
    (a: string): 2;
    (a: number, b: string): 3;
    (a: boolean): 4;
    (a: boolean, b: number): 5;
    (a: boolean, b: number, c: number): 6;
    (a: boolean, b: number, c: boolean): 7;
}

export module module {
    function run(callback: IPipelineFunction): true; // I run the assertions in the implementation.
}