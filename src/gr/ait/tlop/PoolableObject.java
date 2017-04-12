/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package gr.ait.tlop;

import java.io.Serializable;  // needed if classes extending PoolableObject must 
                              // be transported across the network

/**
 * base class for objects that can be "managed" by thread-local object pools.
 * @author itc
 */
public abstract class PoolableObject implements Serializable {
    private ThreadLocalObjectPool _pool = null;
    private boolean _isUsed = false;

    
    /**
     * this factory method shall first try to obtain an object from the thread-
     * local object pool of objects, and if it can't find one, it will 
     * then produce an unmanaged one. Before returning the object, it always 
     * applies the method <CODE>setData(args)</CODE> on it.
     * @param T the type of objects to be created
     * @param f PoolableObjectFactory of T objects
     * @param args Object... if needed by factory
     * @return T
     */
    public final static <T extends PoolableObject> T newInstance(PoolableObjectFactory<T> f, Object... args) {
        T t = ThreadLocalObjectPool.<T>getObject(f, args);
        t.setData(args);
	return t;
    }
    
    
    /**
     * protected constructor specifying that the constructed object is "part"
     * of the given pool. Sub-classes will need to call <
     * CODE>super(null);</CODE> in their constructors for "unmanaged" objects,
     * and <CODE>super(pool);</CODE> in the constructors for "managed" objects,
     * that will have a pool argument as well.
     * @param pool the pool by which the constructed object is "managed".
     */
    protected PoolableObject(ThreadLocalObjectPool pool) {
        _pool = pool;
    }

    
    /**
     * called by the <CODE>newInstance(f,args)</CODE> method when returning a 
     * new instance of the implementing sub-class.
     * @param args 
     */
    public abstract void setData(Object... args);
    
    
    /**
     * indicate item is available for re-use by Object-Pool to which it belongs,
     * and resets its "major" data IFF it is a managed object. Otherwise, it's a
     * no-op.
     * @throws IllegalStateException if this object is managed, but is not 
     * currently "borrowed" from the pool. This would happen if this method is
     * called twice in the current thread on the same object. 
     */
    final public void release() throws IllegalStateException {
        if (_pool!=null) {
            if (_isUsed) {
                _isUsed=false;
		_pool.returnObjectToPool(this);
            } else throw new IllegalStateException("Object not currently used");
        }
    }
    /**
     * safer version of release, is same as <CODE>release()</CODE> method, but
     * also checks whether this object is actually managed by the thread-local
     * (current thread) object pool.
     * @param f
     * @throws IllegalStateException if this object is managed by another 
     * thread's thread-local object pool.
     */
    final public void safeRelease(PoolableObjectFactory f) throws IllegalStateException {
        ThreadLocalObjectPool cur_pool = ThreadLocalObjectPools.getThreadLocalPool(f);
        if (cur_pool != _pool) 
            throw new IllegalStateException("Not managed by current thread's pool");
        release();
    }
    /**
     * indicates that this "managed" object is borrowed from the pool and is
     * currently in use. Only called from the <CODE>getObjectFromPool()</CODE>
     * method of the <CODE>ThreadLocalObjectPool</CODE> class.
     */
    final protected void setIsUsed() {
        _isUsed = true;
    }
    /**
     * return true IFF the object is managed and "currently used", or un-managed.
     * @return boolean
     */
    final protected boolean isUsed() {
        return _isUsed;
    }
    /**
     * true IFF this object belongs to some pool.
     * @return true IFF this object belongs to some pool.
     */
    final protected boolean isManaged() {
    	return _pool!=null;
    }
    final ThreadLocalObjectPool getPool() {  // method exists for debugging purposes only
        return _pool;
    }
	
}
