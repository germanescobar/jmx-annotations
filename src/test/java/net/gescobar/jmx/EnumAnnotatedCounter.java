package net.gescobar.jmx;

import net.gescobar.jmx.annotation.ManagedAttribute;

public class EnumAnnotatedCounter {

	public enum State {	
		STARTED,
		STOPPED
	}
	
	private State state;

	@ManagedAttribute
	public State getState() {
		return state;
	}

	public void setState(State state) {
		this.state = state;
	}
	
}
