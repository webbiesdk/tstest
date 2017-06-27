type impossible = number & string & boolean & 1 & false;

export module module {
    function foo(c: () => impossible) : true
}