
module.exports = {
    foo: function () {
        switch (Math.random() * 3 | 0) {
            case 0: return 123;
            case 1: return "string";
            case 2: return true; // <- Wrong
            default:
                throw new Error("What?!");
        }
    }
};