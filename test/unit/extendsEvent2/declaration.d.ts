interface BaseJQueryEventObject extends Event {
    target: Element;
}
declare function module(): BaseJQueryEventObject;