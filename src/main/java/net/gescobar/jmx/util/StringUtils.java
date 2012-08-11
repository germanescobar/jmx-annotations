package net.gescobar.jmx.util;

public class StringUtils {
	
	/**
	 * Hide public constructor.
	 */
	private StringUtils() {}

	/**
     * Helper method. Upper case the first letter of the received argument.
     * 
     * @param string the string that we want to capitalize.
     * 
     * @return the same string but with the first letter upper cased.
     */
    public static String capitalize(String string) {
    	return Character.toUpperCase( string.charAt(0) ) + string.substring(1);
    }
    
    /**
     * Helper method. Lower case the first letter of the received argument.
     * 
     * @param string the string that we want to decapitalize.
     * 
     * @return the same string but with the first letter lower cased.
     */
    public static String decapitalize(String string) {
    	return Character.toLowerCase( string.charAt(0) ) + string.substring(1);
    }
    
}
