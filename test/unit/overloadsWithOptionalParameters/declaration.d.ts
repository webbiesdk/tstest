interface IPipelineFunction {
    (a: number, b?: boolean): 0;
    (a: boolean, b?: number): 1;
    (a: string): 2;
    (a: string, b: string): 3;
}

export module module {
    function run(callback: IPipelineFunction): true; // I run the assertions in the implementation.
}