
module.exports = {
    foo: function () {
        // Whatever, it will never be called. 
        throw new Error();
    }
};