package net.gescobar.jmx.annotation;

import static java.lang.annotation.ElementType.METHOD;
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
@Target(value={METHOD})
public @interface ManagedAttribute {
	boolean readable() default true;
    boolean writable() default true;
    String description() default "";
}
