

interface IPipelineFunction {
    (token:string):number;
    (token:string, tokenIndex:number):string;
    (token:string, tokenIndex:number, tokens:string[]):boolean;
}

export module module {
    function run(callback: IPipelineFunction): true; // I run the assertions in the implementation.
}