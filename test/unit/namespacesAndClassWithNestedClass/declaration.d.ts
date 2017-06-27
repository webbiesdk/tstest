declare namespace module {
    export class Icon {}
    export namespace Icon {
        export class Default { // <- The bug is, that this one is wrongly types as a class instance, instead of a class.
            instF: string;
        }
    }
}