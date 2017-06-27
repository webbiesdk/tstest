/*
export module module {
    interface Foo<T> {
        bar: Foo<T[]>;
        value: T
    }
    function test(x: Foo<true>): never;
}*/

module.exports = {
    test: function (x) {
        try {
            if (!x) {
                return "no x"
            }
            if (!x.value) {
                return 2;
            }

            var anyArr = x.bar.bar.value;
            if (!(anyArr instanceof Array)) {
                return "not array";
            }
        } catch (e) {
            return e + "";
        }
        if (anyArr.length == 0) {
            throw new Error(); // Only valid output!
        }
        var any = anyArr[0];

        if (any._any) {
            return "was any, good!";
        } else {
            return "NOT AN ANY";
        }


        // {_any: {}}
        /*var FooTArr = {};
        FooTArr.value = [{_any: {}}, {_any: {}}];
        FooTArr.bar =  FooTArr;

        var FooTrueArr = {
            value: [true, true],
            bar:FooTArr
        };
        var FooTrue = {
            value: true,
            bar: FooTrueArr
        }*/

    }
};