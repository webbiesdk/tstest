interface Person {
    name: string;
    age: number;
    location: string;
    // aprivate unspecified
}

export module module {
    function build() : Person
    function accessPrivate(p: Person): String
}