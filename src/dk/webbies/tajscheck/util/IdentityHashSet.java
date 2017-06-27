package dk.webbies.tajscheck.util;

import java.util.*;

/**
 * Created by erik1 on 08-12-2016.
 */
public class IdentityHashSet <T> implements Set<T> {
    private final Map<T, Void> map = new IdentityHashMap<>();

    public IdentityHashSet() {}

    public IdentityHashSet(Collection<T> collection) {
        this.addAll(collection);
    }

    @Override
    public int size() {
        return map.size();
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return map.containsKey(o);
    }

    @Override
    public Iterator<T> iterator() {
        return map.keySet().iterator();
    }

    @Override
    public Object[] toArray() {
        return map.keySet().toArray();
    }

    @Override
    public <T1> T1[] toArray(T1[] a) {
        return map.keySet().toArray(a);
    }

    @Override
    public boolean add(T t) {
        boolean contained = map.keySet().contains(t);
        map.put(t, null);
        return !contained;
    }

    @Override
    public boolean remove(Object o) {
        return map.keySet().remove(o);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return map.keySet().containsAll(c);
    }

    @Override
    public boolean addAll(Collection<? extends T> c) {
        int prevSize = size();
        c.forEach(this::add);
        return prevSize == size();
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return map.keySet().retainAll(c);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return map.keySet().removeAll(c);
    }

    @Override
    public void clear() {
        map.clear();
    }
}
