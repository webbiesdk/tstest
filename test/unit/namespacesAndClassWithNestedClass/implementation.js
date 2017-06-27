/*
declare namespace module {
    export class Icon {}
    export namespace Icon {
        export class Default {
            instF: string;
        }
    }
}
*/

var Icon = function () {};
Icon.Default = function () {
    this.instF = "this is a string";
};
module.exports = {
    Icon: Icon,
};