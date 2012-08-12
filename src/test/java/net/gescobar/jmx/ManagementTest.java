package net.gescobar.jmx;

import java.lang.management.ManagementFactory;

import javax.management.Attribute;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import net.gescobar.jmx.annotation.ManagedAttribute;
import net.gescobar.jmx.annotation.ManagedOperation;

import org.testng.Assert;
import org.testng.annotations.Test;

public class ManagementTest {

	@Test
	public void shouldRegisterAndUnregisterAnnotatedObject() throws Exception {
		
		String name = "org.test:type=AnnotatedCounter9w83793";
		
		AnnotatedCounter counter = new AnnotatedCounter();
		Management.register(counter, name);
		
		MBeanInfo mBeanInfo = ManagementFactory.getPlatformMBeanServer().getMBeanInfo( new ObjectName(name) );
		Assert.assertNotNull( mBeanInfo );
		Assert.assertEquals( mBeanInfo.getClassName(), AnnotatedCounter.class.getName() );
		Assert.assertEquals( mBeanInfo.getDescription(), "Annotated" );
		Assert.assertEquals( mBeanInfo.getConstructors().length, 0 );
		Assert.assertEquals( mBeanInfo.getAttributes().length, 1 );
		Assert.assertEquals( mBeanInfo.getOperations().length, 2 );
		
		MBeanAttributeInfo mBeanAttribute = mBeanInfo.getAttributes()[0];
		Assert.assertNotNull( mBeanAttribute );
		Assert.assertEquals( mBeanAttribute.getName(), "counter" );
		Assert.assertEquals( mBeanAttribute.getType(), "int" );
		
		boolean resetCounter = false;
		boolean addCounter = false;
		for (MBeanOperationInfo mBeanOperation : mBeanInfo.getOperations()) {
			
			Assert.assertNotNull( mBeanOperation );
			
			if (mBeanOperation.getName().equals("resetCounter")) {
				resetCounter = true;
				Assert.assertEquals( mBeanOperation.getReturnType(), "void" );
				Assert.assertEquals( mBeanOperation.getSignature().length, 0 );
			} else if (mBeanOperation.getName().equals("addCounter")) {
				addCounter = true;
				Assert.assertEquals( mBeanOperation.getReturnType(), "boolean" );
				Assert.assertEquals( mBeanOperation.getSignature().length, 1 );
			}
			
		}
		
		Assert.assertTrue(resetCounter);
		Assert.assertTrue(addCounter);
		
		Management.unregister(name);
		Assert.assertFalse( ManagementFactory.getPlatformMBeanServer().isRegistered(new ObjectName(name)) );
		
	}
	
	@Test(dependsOnMethods="shouldRegisterAndUnregisterAnnotatedObject")
	public void shouldInstrumentObject() throws Exception {
		
		String name = "org.test:type=AnnotatedCounter7284746";
		
		AnnotatedCounter counter = new AnnotatedCounter();
		Management.register(counter, name);
		
		ObjectName on = new ObjectName(name);
		MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
		
		Assert.assertEquals( mBeanServer.getAttribute(on, "counter"), 0);
		
		mBeanServer.setAttribute(on, new Attribute("counter", 10));
		Assert.assertEquals( mBeanServer.getAttribute(on, "counter"), 10);
		
		mBeanServer.invoke(on, "resetCounter", new Object[0], new String[0]);
		Assert.assertEquals( mBeanServer.getAttribute(on, "counter"), 0);
		
		mBeanServer.invoke(on, "addCounter", new Object[] { 20 } , new String[] { "int" });
		Assert.assertEquals( mBeanServer.getAttribute(on, "counter"), 20);
		
	}
	
	@Test(dependsOnMethods="shouldRegisterAndUnregisterAnnotatedObject")
	public void shouldInstrumentObjectWithEnum() throws Exception {
		
		String name = "org.test:type=EnumAnnotatedCounter424974";
		
		EnumAnnotatedCounter counter = new EnumAnnotatedCounter();
		Management.register(counter, name);
		
		ObjectName on = new ObjectName(name);
		MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
	
		Assert.assertEquals( mBeanServer.getAttribute(on, "state"), null);
		
		mBeanServer.setAttribute( on, new Attribute("state", EnumAnnotatedCounter.State.STOPPED) );
		Assert.assertEquals( mBeanServer.getAttribute(on, "state"), EnumAnnotatedCounter.State.STOPPED);
		
	}
	
	@Test(expectedExceptions=IllegalArgumentException.class)
	public void shouldNotRegisterNullObject() throws Exception {
		Management.register(null, "org.test:type=Counter7464789");
	}
	
	@Test(expectedExceptions=IllegalArgumentException.class)
	public void shouldNotRegisterNullName() throws Exception {
		Management.register( new AnnotatedCounter(), null);
	}
	
	@Test(expectedExceptions=ManagementException.class)
	public void shouldFailWithMethodAnnotatedAsAttributeAndOperation() throws Exception {
	
		String name = "org.test:type=AnnotatedCounter39645";
		
		WrongAnnotatedCounter counter = new WrongAnnotatedCounter();
		Management.register(counter, name);
		
	}
	
	@Test
	public void shouldNotAddAttributeIfNotReadableOrWritable() throws Exception {
		
		String name = "org.test:type=AnnotatedCounter3456";
		
		AnnotatedCounterNoAttributes counter = new AnnotatedCounterNoAttributes();
		Management.register(counter, name);
		
		MBeanInfo mBeanInfo = ManagementFactory.getPlatformMBeanServer().getMBeanInfo( new ObjectName(name) );
		Assert.assertNotNull( mBeanInfo );
		Assert.assertEquals( mBeanInfo.getAttributes().length, 0 );
		
	}
	
	private class AnnotatedCounterNoAttributes {
		
		@ManagedAttribute(readable=false)
		public int getCounter() { return 0; }
		
	}
	
	private class WrongAnnotatedCounter {
		
		@ManagedAttribute
		@ManagedOperation
		public int getCounter() { return 0; }
	}
	
}
