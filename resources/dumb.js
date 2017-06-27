if (isBrowser) {
    setTimeout(function () {
        testStuff();
    }, 100);
} else {
    testStuff();
}

function dumbMessages() {
    setTimeout(function () {
        dumbCoverage();

        if (isBrowser) {
            printForReal("close");
        }
    });
}