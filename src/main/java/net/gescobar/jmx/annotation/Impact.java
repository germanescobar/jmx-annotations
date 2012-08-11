package net.gescobar.jmx.annotation;

import javax.management.MBeanOperationInfo;

/**
 * An enum that maps to <code>impact</code> options of the <code>MBeanOperationInfo</code>.
 * 
 * @author German Escobar
 */
public enum Impact {

	ACTION(MBeanOperationInfo.ACTION),
    
    INFO(MBeanOperationInfo.INFO),
    
    ACTION_INFO(MBeanOperationInfo.ACTION_INFO),
    
    UNKNOWN(MBeanOperationInfo.UNKNOWN);
    
    private int code;
    
    private Impact(int code) {
    	this.code = code;
    }

    public int getCode() {
        return code;
    }
}
