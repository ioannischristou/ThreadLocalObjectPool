/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import gr.ait.tlop.PoolableObject;
import gr.ait.tlop.ThreadLocalObjectPool;


/**
 *
 * @author itc
 */
public class DLPair extends PoolableObject {
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
