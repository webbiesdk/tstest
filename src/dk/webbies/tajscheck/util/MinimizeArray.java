package dk.webbies.tajscheck.util;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

/**
 * Created by erik1 on 23-01-2017.
 */
public class MinimizeArray {

    private static final ExecutorService pool = Executors.newCachedThreadPool();

    public static <T> T[] minimizeArray(Function<T[], Boolean> test, T[] array) throws IOException {
        return minimizeArray(1, test, array);
    }

    public static <T> T[] minimizeArray(int threads, Function<T[], Boolean> test, T[] array) throws IOException {
        for (int sz = array.length >>> 1; sz > 0; sz >>>= 1) {
            System.out.println("  chunk size " + sz);
            int nchunks = (int) Math.floor(array.length / sz);
            List<Function<T[], T[]>> runGroup = null;
            for (int i = nchunks - 1; i >= 0; --i) {
                if (runGroup == null || runGroup.size() >= threads) {
                    if (runGroup != null) {
                        array = runGroup(array, runGroup);
                    }
                    runGroup = new ArrayList<>();
                }
                int finalI = i;
                int finalSz = sz;
                runGroup.add((finalArray) -> removeChunk(test, finalArray, finalSz, nchunks, finalI));
            }
            array = runGroup(array, runGroup);
        }
        return array;
    }

    public static <T> T[] minimizeArrayQuick(int threads, Function<T[], T[]> test, T[] array) throws IOException {
        for (int sz = array.length >>> 1; sz > 0; sz >>>= 1) {
            System.out.println("  chunk size " + sz);
            int nchunks = (int) Math.floor(array.length / sz);
            List<Function<T[], T[]>> runGroup = null;
            for (int i = nchunks - 1; i >= 0; --i) {
                if (runGroup == null || runGroup.size() >= threads) {
                    if (runGroup != null) {
                        array = runGroup(array, runGroup);
                    }
                    runGroup = new ArrayList<>();
                }
                int finalI = i;
                int finalSz = sz;
                runGroup.add((finalArray) -> removeChunkQuick(test, finalArray, finalSz, nchunks, finalI));
            }
            array = runGroup(array, runGroup);
        }
        return array;
    }

    private static <T> T[] runGroup(T[] array, List<Function<T[], T[]>> runGroup) {
        CountDownLatch latch = new CountDownLatch(runGroup.size());
        AtomicReference<T[]> ref = new AtomicReference<>(array);
        for (Function<T[], T[]> callable : runGroup) {
            pool.submit(() -> {
                try {
                    T[] result = callable.apply(ref.get());
                    if (result.length < ref.get().length) {
                        ref.set(result);
                    }
                } catch (Exception e) {
                    throw new RuntimeException();
                } finally {
                    latch.countDown();
                }
            });
        }
        try {
            latch.await();
        } catch (InterruptedException e) {
            throw new RuntimeException();
        }
        array = ref.get();
        return array;
    }

    private static <T> T[] removeChunkQuick(Function<T[], T[]> test, T[] array, int sz, int nchunks, int i) {
        // try removing chunk i
        System.out.println("    chunk #" + i + " size(" + sz + ") arr(" + array.length + ")");
        int lo = i * sz;
        int hi = i == nchunks - 1 ? array.length : (i + 1) * sz;

        // avoid creating empty array if nonempty is set
        if (lo > 0 || hi < array.length) {
            T[] orgArray = array.clone();
            array = deleteFromArray(array, lo, hi - lo);

            T[] newArr = test.apply(array);
            if (newArr == null) {
                array = orgArray;
            } else {
                array = newArr;
            }

        }
        return array;
    }

    private static <T> T[] removeChunk(Function<T[], Boolean> test, T[] array, int sz, int nchunks, int i) {
        // try removing chunk i
        System.out.println("    chunk #" + i + " size(" + sz + ") arr(" + array.length + ")");
        int lo = i * sz;
        int hi = i == nchunks - 1 ? array.length : (i + 1) * sz;

        // avoid creating empty array if nonempty is set
        if (lo > 0 || hi < array.length) {
            T[] orgArray = array.clone();
            array = deleteFromArray(array, lo, hi - lo);

            if (!test.apply(array)) {
                array = orgArray;
            }
        }
        return array;
    }

    private static <T> T[] deleteFromArray(T[] array, int from, int length) {
        List<T> result = new ArrayList<>();

        for (int i = 0; i < Math.min(from, array.length); i++) {
            result.add(array[i]);
        }
        for (int i = from + length; i < array.length; i++) {
            result.add(array[i]);
        }

        return result.toArray((T[]) Array.newInstance(array.getClass().getComponentType(), 0));
    }

}
