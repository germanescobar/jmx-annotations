package net.gescobar.jmx.annotation;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * 
 * @author German Escobar
 */
@Documented
@Retention(value=RUNTIME)
@Target(value={TYPE})
public @interface ManagedBean {
	String description() default "";
}
