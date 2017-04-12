/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package gr.ait.tlop;

/**
 * interface for factory creating objects of a class whose instances may be 
 * "managed" or "unmanaged" by thread-local object pools; all such classes must 
 * extend the <CODE>PoolableObject</CODE> base abstract class.
 * @author itc
 */
public interface PoolableObjectFactory<T extends PoolableObject> {
    /**
     * compile-time constant specifies the maximum number of types that will
     * need thread-local pooling in the program.
     */
    public static final int _MAX_NUM_POOLED_TYPES = 10;
    /**
     * create an "unmanaged" object, using the args passed in.
     * @param args
     * @return 
     */
    T createObject(Object... args);
    /**
     * create a "managed" object (using the args passed in) that will belong to 
     * the specified pool.
     * @param pool
     * @param args
     * @return 
     */
    T createPooledObject(ThreadLocalObjectPool<T> pool, Object... args);
    /**
     * return an id that is unique for each "poolable" type T in the program.
     * This id will be used to dereference the right pool of 
     * <CODE>PoolableObject</CODE>s existing in the <CODE>ThreadLocal</CODE>
     * variable of the current thread when getting an object of type T. 
     * @return int must be within {0,..._MAX_NUM_POOLED_TYPES}.
     */
    int getUniqueTypeId();
}
