/*
declare module module {
    function time<T>(
        offset: number,
        k: (result: number) => T
): T
}*/
module.exports = {
    time: function (o, k) {
        return k(new Date() + o);
    }
};

/*

var foo = {
  time: function (o, k) {
    return k(new Date() + o);
  }
}
 */