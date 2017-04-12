/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import gr.ait.tlop.*;
import popt4jlib.IdentifiableIntf;
import parallel.TaskObject;
import parallel.ComparableTaskObject;
import parallel.ThreadSpecificComparableTaskObject;
import parallel.FasterParallelAsynchBatchPriorityTaskExecutor;
import parallel.SimplePriorityMsgPassingCoordinator;
import java.util.List;
import java.util.ArrayList;
import java.io.Serializable;

/**
 *
 * @author itc
 */
public class Fib extends PoolableObject implements ThreadSpecificComparableTaskObject {
    private final static int _threshold = 20;
    private final static FibFactory _f = new FibFactory();
    private final static ReleaseFibObjectFactory _rf = new ReleaseFibObjectFactory();
    static FasterParallelAsynchBatchPriorityTaskExecutor _extor = null;  // set once from main
    private boolean _isDone;  // false
    private boolean _canBeReleased;  // false
    private int _threadid = Integer.MAX_VALUE;  // MAX_VALUE specifies "N/A". 
    // Any other number specifies the thread-id in which this object needs to be
    // run.
    int _n;  // zero
    private volatile long _result = -1;
    private Fib _fnm1;  // null
    private Fib _fnm2;  // null
    private volatile long _exiters = 0;  // debugging
    private volatile String _path;
    private List<Fib> _list = new ArrayList<Fib>(2);  // reserve space for 2 Fibs
    
    public Fib(int n, String path) {
        super(null);
        _n = n;
        _path = path;
    }
    public Fib(ThreadLocalObjectPool<Fib> p) {
        super(p);
    }
    public void setData(Object... args) {
        // no-op
    }
    public void setPData(int n, String path) {
        _n = n;
        _path = path;
        _isDone = false;
        _canBeReleased = false;
        _fnm1 = null;
        _fnm2 = null;
        _result = -1;
        _list.clear();
        _exiters = 0;
        _threadid = Integer.MAX_VALUE;
    }
    
    public void copyFrom(TaskObject o) throws IllegalArgumentException {
        throw new IllegalArgumentException("not supported");
    }
    public synchronized boolean isDone() {
        return _isDone;
    }
    public boolean canBeReleased() {  // used to be synchronized
        return _canBeReleased;
    }
    public synchronized void setCanBeReleased() {
        _canBeReleased = true;
        notify();
    }
    public int getThreadIdToRunOn() {
        return _threadid;
    }
    public Serializable run() {
        ++_exiters;
        //System.err.println("Computing "+this);
        if (_exiters>=10000) {  // too much, print and exit
            System.err.println("data queue="+_extor.getDataQueueAsString());
            System.exit(-1);
        }
        if (_fnm1!=null) {  // has been submitted before
            if (_fnm1.isDone() && _fnm2.isDone()) {
                _result = _fnm1._result + _fnm2._result;
                synchronized (this) {
                    _isDone = true;
                    //notify();
                }
                _fnm1.setCanBeReleased();
                _fnm2.setCanBeReleased();
                //setCanBeReleased();  // I cannot yet be released safely
                //System.err.println("Done Computing Fib("+_n+","+_path+")");
                return new Long(_result);
            } else {  // send back to executor
                try {
                    _threadid = (int)((IdentifiableIntf)Thread.currentThread()).getId();
                    _extor.execute(this);
                    return null;  // no need to return anything
                }
                catch (Exception e) {
                    e.printStackTrace();
                    System.exit(-1);
                }
            }
        }
        if (_n<=_threshold) {
            _result = seqFib(_n);
            synchronized (this) {
                _isDone = true;
                //notify();
            }
            //System.err.println("Done Computing Fib("+_n+")");
            setCanBeReleased();
            return new Long(_result);
        } else {
            _fnm1 = PoolableObject.newInstance(_f, null);
            //_fnm1 = new Fib(_n-1,_path+"l");
            _fnm1.setPData(_n-1, _path+"l");
            _fnm2 = PoolableObject.newInstance(_f, null);
            //_fnm2 = new Fib(_n-2, _path+"r");
            _fnm2.setPData(_n-2, _path+"r");
            _list.clear();
            _list.add(_fnm1);
            _list.add(_fnm2);
            try {
                _extor.executeBatch(_list);
                // finally, submit the clean-up objects
		final int mythreadid = (int)((IdentifiableIntf)Thread.currentThread()).getId();
                if (_fnm1.isManaged()) {
                    ReleaseFibObject rfo1 = PoolableObject.<ReleaseFibObject>newInstance(_rf, null);
                    rfo1.setPData(_fnm1, mythreadid);
                    _extor.execute(rfo1);
                }
                if (_fnm2.isManaged()) {
                    ReleaseFibObject rfo2 = PoolableObject.<ReleaseFibObject>newInstance(_rf, null);
                    rfo2.setPData(_fnm2, mythreadid);
                    _extor.execute(rfo2);
                }
                // finally-finally, submit this guy
                _threadid = mythreadid;
                _extor.execute(this);
            }
            catch (Exception e) {
                e.printStackTrace();
                System.exit(-1);
            }
            return null;  // no need to return anything
        }
    }
    public int compareTo(Object other) {
        if (other instanceof Fib) {
            Fib of = (Fib) other;
            if (_n < of._n) return -1;  // lesser _n values go first
            else if (_n == of._n) return _path.compareTo(of._path);
            else return 1;
        }
        else {
            return -1;  // Fib go before ReleaseFibObject's
        }
    }
    public boolean equals(Object other) {
        return other==this;
        /*
        if (other instanceof Fib) {
            Fib o = (Fib) other;
            return _n == o._n;
        }
        return false;
        */
    }
    public int hashCode() {
        return _n;
    }
    public String toString() {
        String fib="Fib[";
        fib += "_n="+_n+",_path="+_path+",_result="+_result+",_isDone="+_isDone;
        fib += _fnm1!=null ? ", _fnm1._isDone="+_fnm1.isDone() : "";
        fib += _fnm2!=null ? ", _fnm2._isDone="+_fnm2.isDone() : "";
        fib += ",_exiters="+_exiters+"]";
        return fib;
    }
    
    
    private static long seqFib(int n) {
        if (n<=1) return n;
        else {
            long res = seqFib(n-1)+seqFib(n-2);
            return res;
        }
    }
    
    
    public static void main(String[] args) {
        int n = 50;
        try {
            long start = System.currentTimeMillis();
            _extor=FasterParallelAsynchBatchPriorityTaskExecutor.
                        newFasterParallelAsynchBatchPriorityTaskExecutor(8,
                                                                         false);
            Fib f = new Fib(n,"");
            _extor.execute(f);
            while (f.isDone()==false) {
                Thread.sleep(10);
            }
            long dur = System.currentTimeMillis()-start;
            System.out.println("result = "+f._result+" in "+dur+" msecs");
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
    
}

class FibFactory implements PoolableObjectFactory<Fib> {
    public Fib createObject(Object... args) {
        return new Fib(null);
    }
    public Fib createPooledObject(ThreadLocalObjectPool<Fib> pool, Object... args) {
        return new Fib(pool);
    }
    public final int getUniqueTypeId() { return 0; }
}
class ReleaseFibObjectFactory implements PoolableObjectFactory<ReleaseFibObject> {
    public ReleaseFibObject createObject(Object... args) {
        return new ReleaseFibObject(null);
    }
    public ReleaseFibObject createPooledObject(ThreadLocalObjectPool<ReleaseFibObject> pool, Object... args) {
        return new ReleaseFibObject(pool);
    }
    public final int getUniqueTypeId() { return 1; }
}

class ReleaseFibObject extends PoolableObject implements ThreadSpecificComparableTaskObject {
    private Fib _obj;
    private int _tid;
    private boolean _isDone = false;  // unused
    
    public ReleaseFibObject(Fib obj, int threadid) {
        super(null);
        _obj = obj;
        _tid = threadid;
    }
    public ReleaseFibObject(ThreadLocalObjectPool<ReleaseFibObject> pool) {
        super(pool);
    }

    public void setData(Object... args) {
        // no-op
    }
    public void setPData(Fib obj, int tid) {
        _obj = obj;
        _tid = tid;
        _isDone = false;
    }
    public void copyFrom(TaskObject o) throws IllegalArgumentException {
        throw new IllegalArgumentException("not supported");
    }
    public int compareTo(Object other) {
        if (other instanceof ReleaseFibObject) {
            ReleaseFibObject of = (ReleaseFibObject) other;
            if (_obj._n < of._obj._n) return -1;  // lesser _n values go first
            else if (_obj._n == of._obj._n) return 0;
            else return 1;
        }
        else {
            return 1;  // Fib's go before ReleaseFibObject's
        }
    }
    public boolean equals(Object other) {
        if (other instanceof ReleaseFibObject) {
            ReleaseFibObject o = (ReleaseFibObject) other;
            return _obj._n == o._obj._n;
        }
        return false;
    }
    public int hashCode() {
        return _obj._n;
    }
    public synchronized boolean isDone() {  // unused
        return _isDone;
    }

    
    public int getThreadIdToRunOn() {
        return _tid;
    }

    
    public Serializable run() {
        int tid = (int)((popt4jlib.IdentifiableIntf) Thread.currentThread()).getId();
        try {
            // 0. sanity test
            if (tid!=_tid) throw new parallel.ParallelException("tid!=_tid?");
            // 0.5 wait until task is actually done
            boolean timed_out = false;
            synchronized (_obj) {
                while (_obj.canBeReleased()==false && !timed_out) {
                    try {
                        _obj.wait(10);  // wait for up to a few msecs
                        if (_obj.canBeReleased()==false) {
                            timed_out = true;
                        }
                    }
                    catch (InterruptedException e) {
                        Thread.currentThread().interrupt();  // recommended practice
                    }
                }
            }
            if (timed_out) {  // do smth else
                Fib._extor.execute(this);
                return null;
            }
            // 1. release the Fib
            _obj.release();
            // 2. release myself!
            release();
        }
        catch (Exception e) {
            System.err.println("ReleaseFibObject="+this+" is running on a Thread("+tid+") other than the one in which it was created");
            System.exit(-1);
        }
        return null;
    }    
    
    
    public String toString() {
        String rfo = "RFO[_obj="+_obj+",_tid="+_tid+"]";
        return rfo;
    }
}

