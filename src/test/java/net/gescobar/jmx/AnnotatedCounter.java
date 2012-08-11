package net.gescobar.jmx;

import net.gescobar.jmx.annotation.Impact;
import net.gescobar.jmx.annotation.ManagedAttribute;
import net.gescobar.jmx.annotation.ManagedBean;
import net.gescobar.jmx.annotation.ManagedOperation;

@ManagedBean(description="Annotated")
public class AnnotatedCounter {
	
	private int counter;
	
	@ManagedOperation(impact=Impact.ACTION)
	public void resetCounter() {
		this.counter = 0;
	}
	
	@ManagedOperation(impact=Impact.ACTION)
	public boolean addCounter(int value) {
		counter += value;
		return true;
	}

	@ManagedAttribute
	public int getCounter() {
		return counter;
	}
	
	public void setCounter(int counter) {
		this.counter = counter;
	}
	
}
