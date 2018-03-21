/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package gr.ait.tlop;


/**
 * Test of the Thread-Local Object Pool functionality, via the Profiler facility
 * of the NetBeans IDE. 
 * @author itc
 */
public class DLPTest {
    static boolean _USE_POOLS = true;
    
    /**
     * args are: [total_num_objs(800,000,000)] [num_threads(8)] [do_pools(true)]
     * @param args 
     */
    public static void main(String[] args) {
        long start = System.currentTimeMillis();
        long total_num_objs = 800000000;
        if (args.length>0) total_num_objs = Long.parseLong(args[0]);
        int num_threads = 8;
        if (args.length>1) num_threads = Integer.parseInt(args[1]);
        if (args.length>2) _USE_POOLS = Boolean.parseBoolean(args[2]);
        long start_ind = 0;
        long work_per_thread = total_num_objs/num_threads;
        DLPThread[] threads = new DLPThread[num_threads];
        for (int i=0; i<num_threads-1; i++) {
            threads[i] = new DLPThread(i, start_ind, work_per_thread);
            start_ind += work_per_thread;
        }
        long remaining = total_num_objs - start_ind;
        threads[threads.length-1] = new DLPThread(threads.length-1, start_ind, remaining);
        for (int i=0; i<threads.length; i++) {
            threads[i].start();
        }
        double total_val = 0.0;
        for (int i=0; i<threads.length; i++) {
            try {
                threads[i].join();
                total_val += threads[i].getSum();
            }
            catch (InterruptedException e) {
                e.printStackTrace();
                Thread.currentThread().interrupt();
            }
        }
        long dur = System.currentTimeMillis() - start;
        System.out.println("Total val="+total_val+" in "+dur+" msecs.");
    }
}

class DLPair extends PoolableObject {
    private long _id;
    private double _val;
    
    public DLPair(long id, double val) {
        super(null);
        _id = id;
        _val = val;
    }
    
    public DLPair(ThreadLocalObjectPool<DLPair> pool) {
        super(pool);
    }
    
    public void setData(Object... args) {
        if (args==null) return;  // guard against null var-args
        _id = ((Long) args[0]).longValue();
        _val = ((Double) args[1]).doubleValue();
    }
    
    public void setData(long id, double val) {
        _id = id;
        _val = val;
    }
    
    double getVal() { return _val; }
}

class DLPairFactory implements PoolableObjectFactory<DLPair> {
    public DLPair createObject(Object... args) {
        return new DLPair(null);
    }
    public DLPair createPooledObject(ThreadLocalObjectPool<DLPair> pool, Object... args) {
        return new DLPair(pool);
    }
    public int getUniqueTypeId() { return 0; }
}

class DLPThread extends Thread {
    private int _id;
    private long _numObjs;
    private long _offset;
    private double _sum;
    private static DLPairFactory _f = new DLPairFactory();
    public DLPThread(int id, long offset, long numobjs) {
        _numObjs = numobjs;
        _offset = offset;
        _id = id;
    } 
    
    public void run() {
        System.out.println("running thread-id="+_id+" with _numObjs="+_numObjs+" and _offset="+_offset);
        DLPair[] pis = new DLPair[10000];
        _sum = 0.0;
        int ind = 0;
        final long up_to = _offset+_numObjs;
        for (long i=_offset; i<up_to; i++) {
            DLPair pi = null;
            if (DLPTest._USE_POOLS) 
                pi = PoolableObject.newInstance(_f, null);
            else pi = new DLPair(0,0);
            //pi.setData(i, Math.pow(i, 2.0));
            pi.setData(i, 2.0*i);
            _sum += pi.getVal();
            pis[ind++] = pi;
            if (ind == pis.length) {
                if (DLPTest._USE_POOLS) {
                    for (int j=0; j<pis.length; j++) {
                        pis[j].release();
                    }
                }
                ind = 0;
            }
        }
    }
    
    public double getSum() {
        return _sum;
    }
}

