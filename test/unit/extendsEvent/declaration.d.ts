interface BaseJQueryEventObject extends Event {
    currentTarget: Element;
}
declare function module(): BaseJQueryEventObject;