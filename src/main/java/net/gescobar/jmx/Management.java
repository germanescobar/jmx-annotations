package net.gescobar.jmx;

import java.lang.management.ManagementFactory;

import javax.management.DynamicMBean;
import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import net.gescobar.jmx.annotation.ManagedAttribute;
import net.gescobar.jmx.annotation.ManagedBean;
import net.gescobar.jmx.annotation.ManagedOperation;
import net.gescobar.jmx.impl.MBeanFactory;


/**
 * <p>Provides methods to register and unregister objects as JMX MBeans.</p>
 * 
 * @author German Escobar
 */
public final class Management {
	
	/**
	 * Hide public constructor.
	 */
	private Management() {}

	/**
	 * <p>Registers an object with the specified <code>name</code> in the default <code>MBeanServer</code> (which is 
	 * retrieved using the <code>ManagementFactory.getPlatformServer()</code> method).</p>
	 * 
	 * <p>All the public attributes and methods annotated with {@link ManagedAttribute} and {@link ManagedOperation} 
	 * of the object's class (and the classes it descends from) will be exposed. The object must be annotated with 
	 * {@link ManagedBean}.</p>
	 * 
	 * @param object the object that will be exposed as an MBean.
	 * @param name the name used to expose the object in the MBeanServer (see 
	 * 		  <a href="http://docs.oracle.com/javase/6/docs/api/javax/management/ObjectName.html">
	 * 		  http://docs.oracle.com/javase/6/docs/api/javax/management/ObjectName.html</a> for more information).
	 * 
	 * @throws InstanceAlreadyExistsException if the MBean is already registered.
	 * @throws ManagementException if there is a problem creating or registering the MBean.
	 */
    public static void register(Object object, String name) throws InstanceAlreadyExistsException, ManagementException {
    	
    	if (object == null) {
    		throw new IllegalArgumentException("No object specified.");
    	}
    	
    	if (name == null || "".equals(name)) {
    		throw new IllegalArgumentException("No name specified.");
    	}
    	
    	MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
    	if (mBeanServer == null) {
    		throw new ManagementException("No MBeanServer found.");
    	}
    		
    	DynamicMBean mBean = MBeanFactory.createMBean(object);
		
    	try { 
    		mBeanServer.registerMBean( mBean, new ObjectName(name) );
    	} catch (InstanceAlreadyExistsException e) {
    		throw e;
    	} catch (Exception e) {
    		throw new ManagementException(e);
    	}
    	
    }

    /**
     * <p>Unregisters an MBean with the specified <code>name</code> if it exists in the default 
     * <code>MBeanServer</code> (which is retrieved using the <code>ManagementFactory.getPlatformServer()</code> 
     * method).</p>
     * 
     * @param name the name with which the MBean was registered.
     * 
     * @throws ManagementException wraps any unexpected exception unregistering the MBean.
     */
    public static void unregister(String name) throws ManagementException {
    	
    	if (name == null || "".equals(name)) {
    		throw new IllegalArgumentException("No name specified.");
    	}
    	
    	MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
    	if (mBeanServer == null) {
    		throw new ManagementException("No MBeanServer found.");
    	}
    	
    	try {
    		mBeanServer.unregisterMBean( new ObjectName(name) );
    	} catch (InstanceNotFoundException e) {
    		
    	} catch (Exception e) {
    		throw new ManagementException(e);
    	}
    	
    }
    
    public boolean isRegistered(String name) throws ManagementException {
    	
    	if (name == null || "".equals(name)) {
    		throw new IllegalArgumentException("No name specified.");
    	}
    	
    	MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
    	if (mBeanServer == null) {
    		throw new ManagementException("No MBeanServer found.");
    	}
    	
    	try {
    		return mBeanServer.isRegistered( new ObjectName(name) );
    	} catch (Exception e) {
    		throw new ManagementException(e);
    	}
    	
    }
    
}
