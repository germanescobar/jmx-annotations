package net.gescobar.jmx.annotation;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Used to add a JMX description to a class or parameter.
 * 
 * @author German Escobar
 */
@Documented
@Retention(value = RUNTIME)
@Target(value = { TYPE, PARAMETER })
public @interface Description {
  String value();
}