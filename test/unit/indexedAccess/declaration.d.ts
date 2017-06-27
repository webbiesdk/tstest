interface Person {
    name: string;
    age: number;
    location: string;
}

export module module {
    function foo() : Person["name" | "age"] // The same as string | number
}