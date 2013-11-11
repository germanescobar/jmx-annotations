package net.gescobar.jmx.impl;

/**
 * Abstraction to allow for lazy-resolution of proxy targets.
 *
 * @author bvarner
 */
public abstract class AbstractInstanceResolver {
    
    /**
     * Return an object to have methods or attributes invoked upon it.
     * 
     * @return 
     */
    public abstract Object resolve();
}
