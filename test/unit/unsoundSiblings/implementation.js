/*
declare module module {
  export class b2Shape {
    setSibling(other: b2Shape): void;
    getSibling(): b2Shape;
  }
  export class b2CircleShape extends b2Shape {
    setSibling(other: b2CircleShape): void;
    getSibling(): b2CircleShape;
  }
}
*/


module.exports = {
  b2Shape: function () {
    this.setSibling = function (a) {
        this.value = a;
    }
    this.getSibling = function () {
        return this.value;
    };
    this.value = this;
  },
  b2CircleShape: function () {
    module.exports.b2Shape.call(this);
    this.getRadius = function () {
      return 4;// <- dice roll.
    }
  }
};