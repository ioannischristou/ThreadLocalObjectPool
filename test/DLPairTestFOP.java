/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */


/**
 * test the FastObjectPool from the free public domain...
 * @author itc
 */
public class DLPairTestFOP {
    static boolean _USE_FOPOOL = true;
    static FastObjectPool<DLPairFOP> _pool = null;
    /**
     * args are: <total_num_objs> <num_threads> [do_pools(true)]
     * @param args 
     */
    public static void main(String[] args) {
        long start = System.currentTimeMillis();
        long total_num_objs = 800000000;
        if (args.length>0) total_num_objs = Long.parseLong(args[0]);
        int num_threads = 8;
        if (args.length>1) num_threads = Integer.parseInt(args[1]);
        if (args.length>2) _USE_FOPOOL = Boolean.parseBoolean(args[2]);
        if (_USE_FOPOOL) {
            _pool = new FastObjectPool<DLPairFOP>(new DLPairFOPFactory(), num_threads*100000);
        }
        long start_ind = 0;
        long work_per_thread = total_num_objs/num_threads;
        DLPFOPThread[] threads = new DLPFOPThread[num_threads];
        for (int i=0; i<num_threads-1; i++) {
            threads[i] = new DLPFOPThread(i, start_ind, work_per_thread);
            start_ind += work_per_thread;
        }
        long remaining = total_num_objs - start_ind;
        threads[threads.length-1] = new DLPFOPThread(threads.length-1, start_ind, remaining);
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

class DLPairFOP {
    private long _id;
    private double _v;
    public DLPairFOP(long l, double v) {
        _id = l;
        _v = v;
    }
    public double getVal() { return _v; }
    public void setData(long id, double v) {
        _id = id;
        _v = v;
    }
}

class DLPairFOPFactory implements FastObjectPool.PoolFactory<DLPairFOP> {
    public DLPairFOP create() {
        return new DLPairFOP(0,0.0);
    }
}

class DLPFOPThread extends Thread {
    private int _id;
    private long _numObjs;
    private long _offset;
    private double _sum;
    private static DLPairFOPFactory _f = new DLPairFOPFactory();
    public DLPFOPThread(int id, long offset, long numobjs) {
        _numObjs = numobjs;
        _offset = offset;
        _id = id;
    } 
    
    public void run() {
        System.out.println("running thread-id="+_id+" with _numObjs="+_numObjs+" and _offset="+_offset);
        FastObjectPool.Holder[] pis = new FastObjectPool.Holder[10000];
        _sum = 0.0;
        int ind = 0;
        final long up_to = _offset+_numObjs;
        for (long i=_offset; i<up_to; i++) {
            FastObjectPool.Holder<DLPairFOP> pi = null;
            if (DLPairTestFOP._USE_FOPOOL)
                pi = DLPairTestFOP._pool.take();
            else pi = new FastObjectPool.Holder<>(new DLPairFOP(0,0));
            //pi.setData(i, Math.pow(i, 2.0));
            pi.getValue().setData(i, 2.0*i);
            _sum += pi.getValue().getVal();
            pis[ind++] = pi;
            if (ind == pis.length) {
                for (int j=0; j<pis.length; j++) {
                    // pis[j].release();
                    try {
                        if (DLPairTestFOP._USE_FOPOOL)
                            DLPairTestFOP._pool.release(pis[j]);
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                        System.exit(-1);
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

