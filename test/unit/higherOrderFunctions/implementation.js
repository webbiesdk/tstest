/*
export module module {
    function twice(
        a: number | string,
        b: (a: string) => string
    ): string;
}*/

module.exports = {
    twice: function (num, c) {
        return c(num) + c(num) + "";
    }
};