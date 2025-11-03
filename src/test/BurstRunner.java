package test;

import java.io.Serializable;
import java.rmi.Naming;
import java.util.Random;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class BurstRunner {
    private static final int THREADS = 5;
    private static final int OPS_PER_THREAD = 2;
    private static final double WRITE_RATIO = 0.35;

    static class Counter implements Serializable { int v; }

    public static void main(String[] args) throws Exception {
        try { Naming.lookup("rmi://localhost:1099/JvnCoord"); }
        catch (Exception _e) { new jvn.JvnCoordImpl(); }

        jvn.JvnServerImpl s1 = jvn.JvnServerImpl.jvnGetServer();
        jvn.JvnServerImpl s2 = jvn.JvnServerImpl.jvnGetServer();

        final String NAME = "counter";
        jvn.JvnObject o1 = s1.jvnLookupObject(NAME);
        if (o1 == null) {
            o1 = s1.jvnCreateObject(new Counter());
            s1.jvnRegisterObject(NAME, o1);
        }
        final jvn.JvnObject o2 = s2.jvnLookupObject(NAME);

        // reset to 0
        o1.jvnLockWrite();
        try {
            Counter c = (Counter) o1.jvnGetSharedObject();
            c.v = 0;
            ((jvn.JvnObjectImpl) o1).overwriteSharedObject(c);
        } finally {
            o1.jvnUnLock();
        }

        ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        CyclicBarrier start = new CyclicBarrier(THREADS);
        CountDownLatch done = new CountDownLatch(THREADS);
        AtomicInteger committedWrites = new AtomicInteger();


        for (int t = 0; t < THREADS; t++) {
            final int tid = t;
            jvn.JvnObject finalO = o1;
            pool.submit(new Runnable() {
                @Override public void run() {
                    Random r = new Random(1337L ^ tid);
                    final jvn.JvnObject local = (tid % 2 == 0) ? finalO : o2;
                    try { start.await(); } catch (Exception ignored) {}

                    for (int i = 0; i < OPS_PER_THREAD; i++) {
                        boolean w = r.nextDouble() < WRITE_RATIO;
                        try {
                            if (w) {
                                local.jvnLockWrite();
                                try {
                                    Counter c = (Counter) local.jvnGetSharedObject();
                                    c.v++;
                                    ((jvn.JvnObjectImpl) local).overwriteSharedObject(c); // <-- cast to JvnObjectImpl
                                    committedWrites.incrementAndGet();
                                } finally {
                                    local.jvnUnLock();
                                }
                            } else {
                                local.jvnLockRead();
                                try {
                                    Counter c = (Counter) local.jvnGetSharedObject();
                                    if (c.v < 0) throw new IllegalStateException("impossible");
                                } finally {
                                    local.jvnUnLock();
                                }
                            }
                        } catch (Throwable e) {
                            System.err.println("T" + tid + " " + (w?"W":"R") + " failed: " + e);
                        }
                    }
                    done.countDown();
                }
            });
        }

        done.await(10, TimeUnit.MINUTES);
        pool.shutdownNow();

        o2.jvnLockRead();
        int finalVal;
        try {
            Counter c = (Counter) o2.jvnGetSharedObject();
            finalVal = c.v;
        } finally {
            o2.jvnUnLock();
        }

        System.out.println("Committed writes: " + committedWrites.get());
        System.out.println("Final counter: " + finalVal);
        if (finalVal != committedWrites.get()) {
            throw new AssertionError("Lost updates: final=" + finalVal + " committed=" + committedWrites.get());
        }
        System.out.println("OK");
    }
}
