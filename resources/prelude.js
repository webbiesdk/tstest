
var RealDate = Date;

// SeedRandom, from: http://cdnjs.cloudflare.com/ajax/libs/seedrandom/2.3.10/seedrandom.min.js
!function(a,b,c,d,e,f,g,h,i){function j(a){var b,c=a.length,e=this,f=0,g=e.i=e.j=0,h=e.S=[];for(c||(a=[c++]);d>f;)h[f]=f++;for(f=0;d>f;f++)h[f]=h[g=s&g+a[f%c]+(b=h[f])],h[g]=b;(e.g=function(a){for(var b,c=0,f=e.i,g=e.j,h=e.S;a--;)b=h[f=s&f+1],c=c*d+h[s&(h[f]=h[g=s&g+b])+(h[g]=b)];return e.i=f,e.j=g,c})(d)}function k(a,b){var c,d=[],e=typeof a;if(b&&"object"==e)for(c in a)try{d.push(k(a[c],b-1))}catch(f){}return d.length?d:"string"==e?a:a+"\0"}function l(a,b){for(var c,d=a+"",e=0;e<d.length;)b[s&e]=s&(c^=19*b[s&e])+d.charCodeAt(e++);return n(b)}function m(c){try{return o?n(o.randomBytes(d)):(a.crypto.getRandomValues(c=new Uint8Array(d)),n(c))}catch(e){return[+new Date,a,(c=a.navigator)&&c.plugins,a.screen,n(b)]}}function n(a){return String.fromCharCode.apply(0,a)}var o,p=c.pow(d,e),q=c.pow(2,f),r=2*q,s=d-1,t=c["seed"+i]=function(a,f,g){var h=[];f=1==f?{entropy:!0}:f||{};var o=l(k(f.entropy?[a,n(b)]:null==a?m():a,3),h),s=new j(h);return l(n(s.S),b),(f.pass||g||function(a,b,d){return d?(c[i]=a,b):a})(function(){for(var a=s.g(e),b=p,c=0;q>a;)a=(a+c)*d,b*=d,c=s.g(1);for(;a>=r;)a/=2,b/=2,c>>>=1;return(a+c)/b},o,"global"in f?f.global:this==c)};if(l(c[i](),b),g&&g.exports){g.exports=t;try{o=require("crypto")}catch(u){}}else h&&h.amd&&h(function(){return t})}(this,[],Math,256,6,52,"object"==typeof module&&module,"function"==typeof define&&define,"random");

// var random = eval("(Math.seedrandom(initialRandomness + ''), Math.random)");
Math.seedrandom(initialRandomness + '');

// Ensuring randomness of some crypto, from https://github.com/chromium/web-page-replay/blob/master/deterministic.js
if (typeof(crypto) == 'object' &&
    typeof(crypto.getRandomValues) == 'function') {
    crypto.getRandomValues = function(arr) {
        var scale = Math.pow(256, arr.BYTES_PER_ELEMENT);
        for (var i = 0; i < arr.length; i++) {
            arr[i] = Math.floor(Math.random() * scale);
        }
        return arr;
    };
}
// Ensuring randomness of Date, from https://github.com/chromium/web-page-replay/blob/master/deterministic.js
(function () {
    var date_count = 0;
    var date_count_threshold = 1; // Something new, every time.
    var orig_date = Date;
    var time_seed = 1483445485018; // 2017-01-03T12:11:25.018Z
    Date = function() {
        if (this instanceof Date) {
            date_count++;
            if (date_count > date_count_threshold){
                time_seed += 50;
                date_count = 1;
            }
            switch (arguments.length) {
                case 0: return new orig_date(time_seed);
                case 1: return new orig_date(arguments[0]);
                default: return new orig_date(arguments[0], arguments[1],
                    arguments.length >= 3 ? arguments[2] : 1,
                    arguments.length >= 4 ? arguments[3] : 0,
                    arguments.length >= 5 ? arguments[4] : 0,
                    arguments.length >= 6 ? arguments[5] : 0,
                    arguments.length >= 7 ? arguments[6] : 0);
            }
        }
        return new Date().toString();
    };
    Date.__proto__ = orig_date;
    Date.prototype = orig_date.prototype;
    Date.prototype.constructor = Date;
    orig_date.now = function() {
        return new Date().getTime();
    };
    orig_date.prototype.getTimezoneOffset = function() {
        var dst2010Start = 1268560800000;
        var dst2010End = 1289120400000;
        if (this.getTime() >= dst2010Start && this.getTime() < dst2010End)
            return 420;
        return 480;
    };
})();

var random = Math.random;

var require_cache = {};

function loadLibrary(path) {
    return require(path);
}

function createFailDescription(path, expected, actual, iteration, sequence, descrip) {
    var failDescription = path + ": (iteration: " + iteration + ")\n";
    failDescription += "    Here I expected: " + expected + ", but instead I got: \n";
    failDescription += "        descrip: " + descrip + "\n";
    failDescription += "        typeof: " + typeof actual + "\n";
    try {
        var string = JSON.stringify(actual + "");
        failDescription += "        toString: " + string.substring(1, string.length - 1) + "\n";
    } catch (e) {
        failDescription += "        toString: [ERROR] \n";
    }
    try {
        var json = JSON.stringify(actual);
        if (json.length < 200) {
            failDescription += "        JSON: " + json + "\n";
        } else {
            failDescription += "        JSON: LONG!\n";
        }
    } catch (e) {
    }
    // failDescription += "        sequence: " + failure.sequence.toString() + "\n";
    failDescription += "\n";
    return failDescription;
}

var print = console.log.bind(console);


var isBrowser = (function () {
    try {
        return this===window;
    }catch(e){
        return false;
    }
})();

if (isBrowser) {
    var orgPrint = print;
    print = function (message) {
        orgPrint(message);
        sendResultToChecker(message);
    };

    window.onbeforeunload = function () {
        return "Please don't navigate away.";
    };
    window.alert = function () {};
    window.confirm = function () {};
}

function error(msg) {
    print("error: " + msg);
}

var runsWithCoverage = (function () {
    try {
        // Runing coverage inside a browser.
        if (__coverage__) {
            return true;
        }
    } catch (ignored) {
        try {
            // Running coverage with NODE
            var istanbulKey = Object.keys(global).filter(function (key) {
                return key.indexOf("$$cov") !== -1;
            })[0];
            return !!istanbulKey;
        } catch (ignored) {
            return false;
        }
    }
})();

var printForReal = print;
if (runsWithCoverage) {
    print = function () {
        // Nothing.
    }
}

function post(host, port, path, value) {
    var request = require('sync-request');

    if (typeof value !== "string") {
        value = JSON.stringify(value);
    }

    request('POST', "http://" + host + ":" + port + path, {body: value});
}
function dumbCoverage() {
    if (runsWithCoverage) {
        if (isBrowser) {
            printForReal(("::COVERAGE::" + JSON.stringify(__coverage__)) + "::/COVERAGE::");
        } else {
            var istanbulKey = Object.keys(global).filter(function (key) {
                return key.indexOf("$$cov") !== -1;
            })[0];

            var ISTANBUL_PORT_FOR_PARTIAL_RESULTS = 0;

            // The first "0:" is to emulate the sequencer.
            post("localhost", ISTANBUL_PORT_FOR_PARTIAL_RESULTS, "/post", "0:::COVERAGE::" + JSON.stringify(global[istanbulKey]) + "::/COVERAGE::");
        }
    }
}


var no_value = {noValueMarker: true};
var testOrderRecording = [];
var seenFailures = new Set();
function assert(cond, path, expected, actual, iteration, descrip) {
    if (!failOnAny && typeof actual === "object" && actual && actual._any) {
        return true;
    }
    if (path === "mockFunctionForFirstMatchPolicy") {
        return cond;
    }
    if (!cond) {
        var failDescription = createFailDescription(
            path, expected, actual, iteration, testOrderRecording.slice(), descrip
        );
        var key = createFailDescription(
            path, expected, actual, 0, [], descrip
        );
        if (!seenFailures.has(key)) {
            print(failDescription);
            seenFailures.add(key);
        }
    }
    return cond;
}

print("Initial random: " + JSON.stringify(initialRandomness));

try {
    process.on('uncaughtException', function (err) {
        error((err && err.stack) ? err.stack : err);
    });
} catch (e) {
    // ignored.
}

for (var key in console) {
    console[key] = function () {};
}

function RuntimeError(message) {
    error("RuntimeError: " + message);
    message = " " + message;
    this.message = message;
    Error.call(this, message);
}
RuntimeError.prototype = Object.create(Error.prototype);

// Utility functions.

function extend(result) {
    var changedBase = false;

    if (arguments.length == 1) {
        throw new RuntimeError("IntersectionType: nothing to intersect")
    }

    // A pre-check, to see if we are trying to construct the same primitive multiple times. In principle unsound, but it only happens (that i know of) when we have recursively defined intersection types, where there in reality is only one primitive, it is just duplicated.
    var typesOfs = {};
    for (var i = 1; i < arguments.length; i++) {
        var type = typeof arguments[i];
        var prevValue = typesOfs[type];
        if (prevValue && !(type === "function" || type === "object")) {
            throw new RuntimeError("IntersectionType: Cannot intersect primitives.")
        }
        typesOfs[type] = prevValue ? prevValue + 1 : 1;
    }
    if (Object.keys(typesOfs).length == 1 && !typesOfs.object && !typesOfs.function) {
        return arguments[1]; // <- Just returning the first of them, since they are kinda equal.
    }
    if (Object.keys(typesOfs).length > 1) {
        if (!(Object.keys(typesOfs).length == 2 && typesOfs.object && typesOfs.function)) {
            throw new RuntimeError("IntersectionType of primitives, will not do this.");
        }
    }

    for (var i = 1; i < arguments.length; i++) {
        var obj = arguments[i];
        if (obj.__proto__.constructor != Object) {
            if (changedBase) {
                throw new RuntimeError("Cannot construct this IntersectionType")
            }
            changedBase = true;
            result = obj;
        }
    }


    for (var i = 1; i < arguments.length; i++) {
        var obj = arguments[i];
        if (obj !== result) {
            for (var key in obj) {
                if (obj.hasOwnProperty(key)) {
                    result[key] = obj[key];
                }
            }
        }
    }
    return result;
}

function numberIndexCheck(obj, check) {
    for (var key in obj) {
        //noinspection JSUnfilteredForInLoop (It is supposed to be that way, only the object-prototype is excluded).
        if (Number(key) + "" === key && !check(obj[key])) {
            return false;
        }
    }
    return true;
}

function getAllKeys(obj) {
    var result = [];
    for (var key in obj) {
        result.push(key);
    }
    return result;
}

function stringIndexCheck(obj, check) {
    for (var key in obj) {
        //noinspection JSUnfilteredForInLoop (It is supposed to be that way, the object-prototype is exluded because it's properties are non-enumerable).
        if (!check(obj[key])) {
            return false;
        }
    }
    return true;
}

function checkRestArgs(args, fromIndex, check) {
    for (var i = fromIndex; i < args.length; i++) {
        if (!check(args[i])) {
            return false;
        }
    }

    return true;
}

// The below is library code, that enables me to only run the tests, that are actually able to run.

var testsThatCanRun = []; // list of test-indexes
var testsWithUnmetDependencies = {}; // value-index -> {testIndex: number, requirements: []}[]

/**
 *
 * @param index the test index.
 * @param requirements a list of requirements, each requirement is a list of possible value-indexes that satisfy that requirement.
 */
function registerTest (index, requirements) {
    if (requirements.length == 0) {
        testsThatCanRun.push(index);
    } else {
        var registration = {
            testIndex: index,
            requirements: requirements
        };
        for (var i = 0; i < requirements.length; i++) {
            var requirementList = requirements[i];
            for (var j = 0; j < requirementList.length; j++) {
                var requirement = requirementList[j];
                if (!testsWithUnmetDependencies[requirement]) {
                    testsWithUnmetDependencies[requirement] = [];
                }
                testsWithUnmetDependencies[requirement].push(registration);
            }
        }
    }
}
function registerValue(valueIndex) {
    var testList = testsWithUnmetDependencies[valueIndex];
    if (!testList || testList.length == 0) {
        return;
    }
    for (var i = 0; i < testList.length; i++) {
        var test = testList[i];
        test.requirements = test.requirements.filter(function (valueIndexes) {
            return valueIndexes.indexOf(valueIndex) === -1;
        });
    }
    testsWithUnmetDependencies[valueIndex] = testList.filter(function (test) {
        var isEmpty = test.requirements.length == 0;
        if (isEmpty) {
            var testIndex = test.testIndex | 0;
            if (testsThatCanRun.indexOf(testIndex) === -1) {
                testsThatCanRun.push(testIndex);
            }
        }
        return !isEmpty;
    });
}

var startTime = +new RealDate();

var alreadyCalled = {};
function testCalled(number) {
    if (alreadyCalled[number]) {
        return;
    }
    alreadyCalled[number] = true;
    print("Test called: " + number);
}

var i = 0;
function selectTest() {
    var timeSpent = (+new RealDate()) - startTime;
    if (timeSpent > maxTime) {
        return -1;
    }

    if (maxIterations >= 0 && i >= maxIterations) {
        return -1;
    }

    var index = i++;

    if ([1,10,50,200].indexOf(index) !== -1 || index % 1000 === 1) {
        dumbCoverage();
    }

    if (runRecording) {
        var result = recording[index];
        if (typeof result === "undefined") {
            return -1;
        }
        return result;
    } else {
        return testsThatCanRun[Math.floor(Math.random() * testsThatCanRun.length)];
    }

}
