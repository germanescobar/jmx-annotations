# JMX Annotations

[![Build Status](https://buildhive.cloudbees.com/job/germanescobar/job/jmx-annotations/badge/icon)](https://buildhive.cloudbees.com/job/germanescobar/job/jmx-annotations/)

A lightweight library (with no external dependencies) that simplifies the creation and registration of JMX MBeans by providing some annotations that you can easily add to your classes.

Take a look at the following class:

```java
@ManagedBean
public class Statistics {
	
	public int counter;
	
	@ManagedAttribute
	public int getCounter() {
		return this.counter;
	}
	
	public void setCounter(int counter) {
		this.counter = counter;
	}
	
	@ManagedOperation
	public void resetCounter() {
		this.counter = 0;
	}
	
}
```

As you can see the class is annotated with `net.gescobar.jmx.annotation.ManagedBean`, the `getCounter()` method with `net.gescobar.jmx.annotation.ManagedAttribute` and the `resetCounter()` method with `net.gescobar.jmx.annotation.ManagedOperation`. We can now register the JMX MBean in one single line of code:

```java
Management.register(new Statistics(), "org.test:type=Statistics");
```

This will create a DynamicMBean from the object and will register it in the default MBeanServer that is retrieved using `ManagementFactory.getPlatformMBeanServer()` method.

That's it. Enjoy!