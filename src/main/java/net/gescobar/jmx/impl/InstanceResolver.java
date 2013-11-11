package net.gescobar.jmx.impl;

/**
 * A generic AbstractInstanceResolver which resolve the same object every time.
 * 
 * @author bvarner
 */
public class InstanceResolver extends AbstractInstanceResolver {
    
    private Object target;
   
    /**
     * Creates a new InstanceResolver which returns the given object.
     */
    public InstanceResolver(Object obj) {
        this.target = obj;
    }
    
    /**
     * Returns the object this InstanceResolver was constructed to wrap
     * 
     * @return 
     */
    @Override
    public Object resolve() {
        return target;
    }
}
