interface Person {
    name: string;
    age: number;
    location: string;
}

type BooleanifiedPerson1 = {
    [P in "name" | "age" | "location"]: boolean
};

type BooleanifiedPerson2 = {
    [P in keyof Person]: boolean
};

type PartialPerson = Partial<Person>;

type ReadOnlyPerson = Readonly<Person>;

export module module {
    var booleanified1: BooleanifiedPerson1;
    var booleanified2: BooleanifiedPerson2;
    var partial: PartialPerson;
    var readOnly: ReadOnlyPerson;
}