// Type definitions for Async 2.0.1
// Project: https://github.com/caolan/async
// Definitions by: Boris Yankov <https://github.com/borisyankov/>, Arseniy Maximov <https://github.com/kern0>, Joe Herman <https://github.com/Penryn>, Angus Fenying <https://github.com/fenying>, Pascal Martin <https://github.com/pascalmartin>
// Definitions: https://github.com/DefinitelyTyped/DefinitelyTyped

interface AsyncFunction<T, E> { (callback: (err?: E, result?: T) => void): void; }
interface Async {
    reflect<T, E>(fn: AsyncFunction<T, E>) : (callback: (err: void, result: {error?: Error, value?: T}) => void) => void;
}
declare var async: Async