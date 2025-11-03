package test;

import jvn.*;
import proxy.JvnProxyFactory;

import java.util.Random;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class burstProxy {
    private static final int THREADS = 5;
    private static final int OPS_PER_THREAD = 3;
    private static final double WRITE_RATIO = 0.35;

    public static void main(String[] args) throws Exception {
        try { java.rmi.Naming.lookup("rmi://localhost:1099/JvnCoord"); }
        catch (Exception _e) { new JvnCoordImpl(); }

        JvnServerImpl s1 = JvnServerImpl.jvnGetServer();
        JvnServerImpl s2 = JvnServerImpl.jvnGetServer();

        final String NAME = "counter-" + System.currentTimeMillis();

        assert s1 != null;
        JvnObject o1 = s1.jvnLookupObject(NAME);



        if (o1 == null) {
            o1 = s1.jvnCreateObject(new Counter());
            s1.jvnRegisterObject(NAME, o1);
            System.out.println("LA VALEUR FINAL o1:" + o1);
        }

        JvnObject o2 = null;
        for (int i = 0; i < 100; i++) {
            o2 = s2.jvnLookupObject(NAME);
            System.out.println("LA VALEUR FINAL o2:" + o2);
            if (o2 != null) break;
            Thread.sleep(10);
        }
        if (o2 == null) throw new IllegalStateException("S2 lookup failed for " + NAME);

        int id1 = ((JvnObjectImpl) o1).jvnGetObjectId();
        int id2 = ((JvnObjectImpl) o2).jvnGetObjectId();
        System.out.println("OBJ IDs: S1=" + id1 + " S2=" + id2);
        if (id1 != id2) throw new IllegalStateException("Different object IDs!");

        // reset counter to 0 via direct write
        ((JvnObjectImpl) o1).jvnLockWrite();
        try {
            Counter st = (Counter) ((JvnObjectImpl) o1).jvnGetSharedObject();
            st.set(0);
            ((JvnObjectImpl) o1).overwriteSharedObject(st);
        } finally {
            ((JvnObjectImpl) o1).jvnUnLock();
        }

        CounterItf c1 = JvnProxyFactory.createProxy((JvnObjectImpl) o1, CounterItf.class);
        CounterItf c2 = JvnProxyFactory.createProxy((JvnObjectImpl) o2, CounterItf.class);

        ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        CyclicBarrier start = new CyclicBarrier(THREADS);
        CountDownLatch done = new CountDownLatch(THREADS);
        AtomicInteger committedWrites = new AtomicInteger();

        for (int t = 0; t < THREADS; t++) {
            final int tid = t;
            pool.submit(() -> {
                Random rnd = new Random(1337L ^ tid);
                CounterItf local = (tid % 2 == 0) ? c1 : c2;
                try { start.await(); } catch (Exception ignored) {}

                for (int i = 0; i < OPS_PER_THREAD; i++) {
                    boolean write = rnd.nextDouble() < WRITE_RATIO;
                    try {
                        if (write) {
                            local.inc();
                            committedWrites.incrementAndGet();
                        } else {
                            local.get();
                        }
                    } catch (Throwable e) {
                        System.err.println("T" + tid + " op#" + i + " failed: " + e);
                    }
                }
                done.countDown();
            });
        }

        done.await(10, TimeUnit.MINUTES);
        pool.shutdownNow();

        int finalVal = c2.get();
        System.out.println("LA VALEUR FINAL C2:" + c2);
        int committed = committedWrites.get();
        System.out.println("LA VALEUR commited commited:"+committedWrites);
        System.out.println("Committed writes: " + committed);
        System.out.println("Final counter:   " + finalVal);
        if (finalVal != committed) {
            throw new AssertionError("Lost updates! final=" + finalVal + " committed=" + committed);
        }
        System.out.println(" OK — cached version passed!");
    }
}
