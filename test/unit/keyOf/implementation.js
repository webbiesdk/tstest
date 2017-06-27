
module.exports = {
    foo: function () {
        switch (Math.random() * 4 | 0) {
            case 0: return "name";
            case 1: return "age";
            case 2: return "location";
            case 3: return "notAProp"; // <- This is wrong
            default:
                throw new Error("What?!");
        }
    }
};