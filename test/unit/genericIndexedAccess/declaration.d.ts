interface Person {
    name: string;
    age: number;
    location: string;
}

export module module {
    function get<T extends Person, K extends keyof T>(obj: T, propertyName: K): T[K];
}