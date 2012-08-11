h1. JMX Annotations

Simplifies the creation and registration of MBeans by providing some annotations that you can easily add to your classes.

For example, take a look at the following class:

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

We can now register it in one single line of code:

```java
Management.register(new Statistics(), "org.test:type=Statistics");
```

In this case the bean will be registered in the default MBeanServer that is retrieved using `ManagementFactory.getPlatformMBeanServer()`.

That's it. Enjoy!