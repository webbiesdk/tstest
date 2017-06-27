
interface AsyncPriorityQueue<T> {
    workersList: T[];
}

declare function module<T>(): AsyncPriorityQueue<T>;
