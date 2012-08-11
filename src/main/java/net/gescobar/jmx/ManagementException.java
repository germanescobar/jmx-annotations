package net.gescobar.jmx;

/**
 * @author German Escobar
 *
 * An unchecked exception that wraps any exception captured during the creation, registration and unregistration of an 
 * MBean.
 */
public class ManagementException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ManagementException(String message) {
		super(message);
	}

	/**
     * Creates a ManagementException that wraps the actual java.lang.Exception with a detail message.
     * 
     * @param exception the wrapped exception
     * @param message the detail message
     */
    public ManagementException(Exception exception, String message) {
    	super(message, exception);
    }

    /**
     * Creates an ManagementException that wraps the actual java.lang.Exception
     * 
     * @param exception the wrapped exception
     */
    public ManagementException(Exception exception) {
    	super(exception);
    }
    
}
