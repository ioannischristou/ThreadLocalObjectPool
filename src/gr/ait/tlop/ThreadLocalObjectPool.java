/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package gr.ait.tlop;

import java.io.Serializable;  // may be needed if classes extending PoolableObject
                              // must be transported over the network

/**
 *
 * @author itc
 */
public final class ThreadLocalObjectPool<T extends PoolableObject> implements Serializable {
    private static volatile int _NUMOBJS = 100000;  // default cache size
    
    private T[] _pool;  // the container of the objects
    private int _topAvailPos = -1;
    
    
    /**
     * acts as replacement for constructor that would otherwise need to "escape
     * this". Only called from the 
     * <CODE>ThreadLocalObjectPools.getThreadLocalPool(f,args)</CODE> method.
     * @param <T>
     * @param f
     * @param args
     * @return 
     */
    static <T extends PoolableObject> ThreadLocalObjectPool<T> 
        newThreadLocalObjectPool(PoolableObjectFactory<T> f, Object... args) {
        ThreadLocalObjectPool<T> p = new ThreadLocalObjectPool<>();
        p.initialize(f, args);
        return p;
    }
    
    
    /**
     * private constructor constructs the actual pool as an array of 
     * <CODE>_NUMOBJS</CODE> objects, initially all set to null, then 
     * initialized by the <CODE>initialize()</CODE> method.
     */
    private ThreadLocalObjectPool() {
        //System.err.println("ThreadLocalObjectPool<init>: Creating Pool...");
        _pool = (T[]) new PoolableObject[_NUMOBJS];
        //System.err.println("ThreadLocalObjectPool<init>: Done.");
    }
    
    
    /**
     * returns an object from the pool if one is available else asks the factory
     * to create one and return it. Only called from the 
     * <CODE>PoolableObject.newInstance(f,args)</CODE> method.
     * @param <T>
     * @param f
     * @param args
     * @return 
     */
    static <T extends PoolableObject> T getObject(PoolableObjectFactory<T> f, Object... args) {
        ThreadLocalObjectPool<T> pool = ThreadLocalObjectPools.<T>getThreadLocalPool(f, args);
        T p = pool.getObjectFromPool();
        if (p!=null) {  // ok, return managed object
            return p;
        } else  // oops, create new unmanaged object
            return f.createObject(args);
    }
    
    
    /**
     * returns the argument to this pool, IFF it belongs to this pool. Only 
     * called from the <CODE>ind.release()</CODE> method.
     * @param ind T
     */
    void returnObjectToPool(T ind) {
        _pool[++_topAvailPos] = ind;
    }
    
    
    /**
     * called only once, immediately after a pool is constructed.
     * @param f
     * @param args 
     */
    private void initialize(PoolableObjectFactory<T> f, Object... args) {
        for (int i=0; i<_NUMOBJS; i++) {
            _pool[i] = f.createPooledObject(this,args);
        }
        _topAvailPos = _NUMOBJS-1;        
    }

	
    /** 
     * return an managed "free" object from the pool, or null if it cannot find
     * one.
     * @return T
     */
    private T getObjectFromPool() {
        if (_topAvailPos>=0) {
            T obj = _pool[_topAvailPos--];
            _pool[_topAvailPos+1] = null;  // avoid memory leaks in case it's never returned
            obj.setIsUsed();
            return obj;
	}
	return null;
    }

	
    /**
     * sets the number of objects in the thread-local pools to the size specified.
     * Must only be called once, before any pool is actually constructed (should
     * only be called from the <CODE>ThreadLocalObjectPools</CODE> class).
     * @param num int
     * @throws IllegalArgumentException if the argument is &lte 0
     */
    static void setPoolSize(int num) throws IllegalArgumentException {
    	if (num <= 0) 
            throw new IllegalArgumentException("setPoolSize(n): n<=0");
	_NUMOBJS = num;
    }
    
    
    /**
     * return the number of objects in the thread-local pools. May be called at
     * any time.
     * @return int
     */
    static int getPoolSize() {
        return _NUMOBJS;
    }

}

