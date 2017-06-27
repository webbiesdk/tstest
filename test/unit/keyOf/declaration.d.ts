interface Person {
    name: string;
    age: number;
    location: string;
}

export module module {
    function foo() : keyof Person
}