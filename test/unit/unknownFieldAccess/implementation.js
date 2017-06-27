function Person() {
    this.name = "a name";
    this.age = "an age";
    this.location = "a location";
    this.aprivate = "a private";
}


module.exports = {
    build: function () {
        console.log("Build called");
        return new Person();
    },
    accessPrivate: function (p) {
        p.location;
        p.aprivate;

        p.location = "";
        p.aprivate = "";

        p.hasOwnProperty("aprivate");
        p.hasOwnProperty("location");

        Object.defineProperty(p, "aprivate", {value: ""});
        Object.defineProperty(p, "location", {value: ""});


        return "Done";
    }
};