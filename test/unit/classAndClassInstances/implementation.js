/*
declare namespace module {
    interface Options {}
}
declare class module<T extends module.Options> {
    viewportSize: Object;
}
*/
module.exports = function () {
    this.viewportSize = {}; // It is correct.
};