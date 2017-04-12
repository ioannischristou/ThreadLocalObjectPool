
import gr.ait.tlop.PoolableObject;
import gr.ait.tlop.PoolableObjectFactory;
import gr.ait.tlop.ThreadLocalObjectPool;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author itc
 */
public class DLLPair extends DLPair {
    private long _l2;
    public DLLPair(double v, long l1, long l2) {
        super(l1,v);
        _l2 = l2;
    }
    
    public DLLPair(ThreadLocalObjectPool<DLPair> pool) {
        super(pool);
    }
    
    public void setData(Object... args) {
        if (args==null) return;  // guard against null var-args
        setData(((Long) args[0]).longValue(), ((Double) args[1]).doubleValue());
        _l2 = ((Long) args[2]).longValue();
    }
    
    public void setData(long id, long id2, double val) {
        setData(id,val);
        _l2 = id2;
    }
    
    public static void main(String[] args) {
        DLLPair dllp = (DLLPair) PoolableObject.newInstance(new DLLPairFactory(), null);
    }
}

class DLLPairFactory implements PoolableObjectFactory<DLPair> {
    public DLLPair createObject(Object... args) {
        return new DLLPair(null);
    }
    public DLLPair createPooledObject(ThreadLocalObjectPool<DLPair> pool, Object... args) {
        return new DLLPair(pool);
    }
    public int getUniqueTypeId() { return 0; }
}
