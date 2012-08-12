# JMX Annotations

[![Build Status](https://buildhive.cloudbees.com/job/germanescobar/job/jmx-annotations/badge/icon)](https://buildhive.cloudbees.com/job/germanescobar/job/jmx-annotations/)

A lightweight library (with no external dependencies) that simplifies the creation and registration of JMX MBeans by providing some annotations that you can easily add to your classes.

## How it works

Take a look at the following class:

```java
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

As you can see the `getCounter()` method is annotated with `ManagedAttribute` and the `resetCounter()` method with `ManagedOperation`. We can now register the JMX MBean in one single line of code:

```java
Management.register(new Statistics(), "org.test:type=Statistics");
```

This will create a DynamicMBean from the object and will register it in the default MBeanServer that is retrieved using `ManagementFactory.getPlatformMBeanServer()` method.

That's it. Enjoy!

[Browse Javadocs](http://germanescobar.net/projects/jmx-annotations/api/1.0.0/)

## Configuration

If you are using Maven, you just need to add the dependency and the repository to your pom.xml file:

```xml

    <dependencies>
        <dependency>
            <groupId>net.gescobar</groupId>
            <artifactId>jmx-annotations</artifactId>
            <version>1.0.0</version>
        </dependency>
    </dependencies>

    <repositories>
        <repository>  
            <id>elibom</id>  
            <url>http://repository.elibom.net/nexus/content/repositories/releases</url>  
        </repository>
    </repositories>

```