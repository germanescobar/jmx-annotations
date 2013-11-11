package net.gescobar.jmx.impl;

import net.gescobar.jmx.annotation.DescriptorFields;
import static net.gescobar.jmx.util.StringUtils.capitalize;
import static net.gescobar.jmx.util.StringUtils.decapitalize;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import javax.management.DynamicMBean;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanConstructorInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanNotificationInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;

import net.gescobar.jmx.Management;
import net.gescobar.jmx.ManagementException;
import net.gescobar.jmx.annotation.Description;
import net.gescobar.jmx.annotation.Impact;
import net.gescobar.jmx.annotation.ManagedAttribute;
import net.gescobar.jmx.annotation.ManagedOperation;

/**
 * <p>
 * Factory of DynamicMBeans. Users can use this object directly to create
 * DynamicMBeans and then registering them with any MBeanServer. However, the
 * preferred approach is to use the {@link Management} class (which internally
 * uses this class).</p>
 *
 * @author German Escobar
 */
public class MBeanFactory {

    /**
     * Hide public constructor.
     */
    private MBeanFactory() {
    }
    
    /**
     * Creates a DynamicMBean from an object annotated with {@link ManagedBean}
     * exposing all methods and attributes annotated with
     * {@link ManagedOperation} and {@link ManagedAttribute} respectively.
     *
     * @param object the class representing the object type resolved by TargetObjectResolver.
     *
     * @return a constructed DynamicMBean object that can be registered with any
     * MBeanServer.
     */
    public static DynamicMBean createMBean(Object object) {
        if (object == null) {
            throw new IllegalArgumentException("No object specified.");
        }
        return createMBean(object.getClass(), new InstanceResolver(object));
    }

    /**
     * Creates a DynamicMBean from an object annotated with {@link ManagedBean}
     * exposing all methods and attributes annotated with
     * {@link ManagedOperation} and {@link ManagedAttribute} respectively.
     *
     * @param objectType the class representing the object type resolved by TargetObjectResolver.
     * @param targetResolver A resolver capable of resolving an instance of the given type.
     *
     * @return a constructed DynamicMBean object that can be registered with any
     * MBeanServer.
     */
    public static DynamicMBean createMBean(Class<?> objectType, AbstractInstanceResolver targetResolver) {
        if (objectType == null) {
            throw new IllegalArgumentException("No object class specified.");
        }

        // retrieve description
        String description = "";
        if (objectType.isAnnotationPresent(Description.class)) {
            description = objectType.getAnnotation(Description.class).value();
        }

        // build attributes and operations
        Method[] methods = objectType.getMethods();
        MethodHandler methodHandler = new MBeanFactory().new MethodHandler(objectType);
        for (Method method : methods) {
            methodHandler.handleMethod(method);
        }

        // build the MBeanInfo
        MBeanInfo mBeanInfo = new MBeanInfo(objectType.getName(), description, methodHandler.getMBeanAttributes(),
                new MBeanConstructorInfo[0], methodHandler.getMBeanOperations(), new MBeanNotificationInfo[0]);

        // create the MBean
        return new MBeanImpl(targetResolver, mBeanInfo);

    }

    /**
     * This class is used internally to handle the methods of the object that
     * the {@link MBeanFactory#createMBean(Object)} receives as an argument. It
     * creates a collection of MBeanAttributeInfo and MBeanOperationInfo from
     * the information of the methods that it handles.
     *
     * @author German Escobar
     */
    private class MethodHandler {

        /**
         * The class of the object.
         */
        private Class<?> objectType;

        /**
         * Holds the MBeanAttributeInfo objects that are created from the
         * methods annotated with {@link MBeanAttributeInfo}. Notice that the
         * relation is not 1-1. If the attribute is not readable or writable, it
         * will not get added.
         */
        private Collection<MBeanAttributeInfo> mBeanAttributes = new ArrayList<MBeanAttributeInfo>();

        /**
         * Holds the MBeanOperationInfo objects that are created from the
         * methods annotated with {@link MBeanOperationInfo}. The relation is
         * 1-1.
         */
        private Collection<MBeanOperationInfo> mBeanOperations = new ArrayList<MBeanOperationInfo>();

        /**
         * Constructor. Initializes the object with the specified class.
         *
         * @param objectType the class of the object that the MBeanFactory is
         * handling.
         */
        public MethodHandler(Class<?> objectType) {
            this.objectType = objectType;
        }

        /**
         * Called once for each method of the object that the
         * {@link MBeanFactory#createMBean(Object)} receives as an argument. If
         * the method is annotated with {@link ManagedAttribute} it will try to
         * create a MBeanAttributeInfo. If the method is annotated with
         * {@link ManagedOperation} it will create a MBeanOperationInfo.
         * Otherwise, it will do nothing with the method.
         *
         * @param method the method we are handling.
         *
         * @throws ManagementException wraps anyting that could go wrong.
         */
        public void handleMethod(Method method) throws ManagementException {

            boolean hasManagedAttribute = method.isAnnotationPresent(ManagedAttribute.class);
            boolean hasManagedOperation = method.isAnnotationPresent(ManagedOperation.class);

            if (hasManagedAttribute && hasManagedOperation) {
                throw new ManagementException("Method " + method.getName() + " cannot have both ManagedAttribute and "
                        + "ManagedOperation annotations.");
            }

            if (hasManagedAttribute) {
                handleManagedAttribute(method);
            }

            if (hasManagedOperation) {
                handleManagedOperation(method);
            }

        }

        /**
         * Called after the {@link #handleMethod(Method)} is called for all the
         * methods of the <code>objectType</code>. Retrieves the exposed
         * attributes.
         *
         * @return an array of initialized MBeanAttributeInfo objects. It will
         * never return null.
         */
        public MBeanAttributeInfo[] getMBeanAttributes() {
            return mBeanAttributes.toArray(new MBeanAttributeInfo[0]);
        }

        /**
         * Called after the {@link #handleMethod(Method)} is called for all the
         * methods of the <code>objectType</code>. Retrieves the exposed
         * operations.
         *
         * @return an array of initialized MBeanOperationInfo objects. It will
         * never return null.
         */
        public MBeanOperationInfo[] getMBeanOperations() {
            return mBeanOperations.toArray(new MBeanOperationInfo[0]);
        }

        /**
         * Helper method. Handles a method that has a {@link ManagedAttribute}
         * annotation. Notice that the mehtod is not necessarily a valid
         * getter/setter. We actually need to find out.
         *
         * @param method the method that is annotated with
         * {@link ManagedAttribute}
         */
        private void handleManagedAttribute(Method method) {

            // validate if the method is a getter or setter
            Method getterMethod = isGetterMethod(method) ? method : null;
            Method setterMethod = isSetterMethod(method) ? method : null;

            if (getterMethod == null && setterMethod == null) {
                // not a getter or setter
                throw new ManagementException("Method " + method.getName() + " is annotated as ManagedAttribute "
                        + "but doesn't looks like a valid getter or setter.");
            }

            // retrieve the attribute name from the method name
            String attributeName = method.getName().startsWith("is")
                    ? decapitalize(method.getName().substring(2)) : decapitalize(method.getName().substring(3));

            // retrieve the attribute type from the setter argument type or the getter return type
            Class<?> attributeType = setterMethod != null
                    ? method.getParameterTypes()[0] : method.getReturnType();

            // find the missing method
            getterMethod = getterMethod == null ? findGetterMethod(objectType, attributeName) : getterMethod;
            setterMethod = setterMethod == null ? findSetterMethod(objectType, attributeName, attributeType) : setterMethod;

            boolean existsAttribute = existsAttribute(mBeanAttributes, attributeName, attributeType);
            if (!existsAttribute) {

                // add the MBeanAttribute to the collection
                MBeanAttributeInfo mBeanAttribute = buildMBeanAttribute(attributeName, attributeType, getterMethod,
                        setterMethod, method);
                if (mBeanAttribute != null) { // it can be null if it is neither readable or writable
                    mBeanAttributes.add(mBeanAttribute);
                }

            } else {
                // both getter and setter are annotated ... throw exception
                throw new ManagementException("Both getter and setter are annotated for attribute "
                        + attributeName + ". Please remove one of the annotations.");
            }

        }
        
        /**
         * Helper method. Tells if the method is a getter or not. It checks if
         * the method name starts with "get" or "is", that the method has no
         * parameters and returns something different than <code>void</code>.
         *
         * @param method the method that we are testing to see if it is a
         * getter.
         *
         * @return true if the method is a getter, false otherwise.
         */
        private boolean isGetterMethod(Method method) {
            return (method.getName().startsWith("get") || method.getName().startsWith("is"))
                    && (!method.getReturnType().equals(Void.TYPE) && method.getParameterTypes().length == 0);
        }

        /**
         * Helper method. Tells if the method is a setter or not. It checks if
         * the method name starts with "set", that the return type is
         * <code>void</code> and that it has exactly one parameter.
         *
         * @param method the method that we are testing to see if it is a
         * setter.
         *
         * @return true if the method is a setter, false otherwise.
         */
        private boolean isSetterMethod(Method method) {
            return method.getName().startsWith("set") && method.getReturnType().equals(Void.TYPE)
                    && method.getParameterTypes().length == 1;
        }

        /**
         * Helper method. Tries to find a getter method for the specified
         * <code>objectType</code> and <code>attributeName</code>.
         *
         * @param objectType the class from which we are going to find the
         * getter method.
         * @param attributeName the name of the attribute we are looking for.
         *
         * @return a java.lang.reflect.Method object representing the getter or
         * null if not found.
         */
        private Method findGetterMethod(Class<?> objectType, String attributeName) {

            try {
                return objectType.getMethod("get" + capitalize(attributeName));
            } catch (NoSuchMethodException e) {
            }

            try {
                return objectType.getMethod("is" + capitalize(attributeName));
            } catch (NoSuchMethodException e) {
            }

            return null;
        }

        /**
         * Helper method. Tries to find a setter method for the specified
         * <code>objectType</code>, <code>attributeName</code> and
         * <code>attributeType</code>
         *
         * @param objectType the class from which we are going to find the
         * setter method.
         * @param attributeName the name of the attribute we are looking for.
         * @param attributeType the type of the attribute we are looking for.
         *
         * @return a java.lang.reflect.Method object representing the setter or
         * null if not found.
         */
        private Method findSetterMethod(Class<?> objectType, String name, Class<?> attributeType) {

            try {
                return objectType.getMethod("set" + capitalize(name), attributeType);
            } catch (NoSuchMethodException e) {
                return null;
            }

        }

        /**
         * Helper method. Tells if the collection of MBeanAttributeInfo holds an
         * attribute with the specified name and type.
         *
         * @param mBeanAttributes the collection of MBeanAttributeInfo in which
         * we are searching the attribute.
         * @param attributeName the name of the attribute we are searching for.
         * @param attributeType the type of the attribute we are searching for.
         *
         * @return true if the collections holds the attribute, false otherwise.
         */
        private boolean existsAttribute(Collection<MBeanAttributeInfo> mBeanAttributes, String attributeName, Class<?> attributeType) {

            for (MBeanAttributeInfo mBeanAttribute : mBeanAttributes) {
                if (mBeanAttribute.getName().equals(attributeName)
                        && mBeanAttribute.getType().equals(attributeType.getName())) {
                    return true;
                }
            }

            return false;

        }

        /**
         * Helper method. Builds an MBeanAttributeInfo. As a precondition we
         * know that there is public getter or setter method for the attribute
         * that it's annotated with {@link ManagedAttribute}.
         *
         * @param attributeName the name of the attribute for which we are
         * trying to build the MBeanAttributeInfo.
         * @param attributeType the class of the attribute for which we are
         * trying to build the MBeanAttributeInfo.
         * @param getterMethod the getter method of the attribute ... can be
         * null.
         * @param setterMethod the setter method of the attribute ... can be
         * null.
         * @param annotatedMethod the method that is annotated with
         * {@link ManagedAttribute} ... can't be null.
         *
         * @return a constructed MBeanAttributeInfo object or null if the
         * attribute is neither readable or writable.
         */
        private MBeanAttributeInfo buildMBeanAttribute(String attributeName,
                Class<?> attributeType, Method getterMethod, Method setterMethod, Method annotatedMethod) {

            ManagedAttribute managedAttribute = annotatedMethod.getAnnotation(ManagedAttribute.class);

            // it's readable if the annotation has readable=true (which is the default) and the getter method exists
            boolean readable = managedAttribute.readable() && getterMethod != null;

            // it's writable if the annotation has writable=true (which is the default) and the setter method exists.
            boolean writable = managedAttribute.writable() && setterMethod != null;

            // it's IS if the getter method exists and starts with "is".
            boolean isIs = getterMethod != null && getterMethod.getName().startsWith("is");

            // only add the attribute if it is readable and writable
            if (readable || writable) {
                return new MBeanAttributeInfo(attributeName, attributeType.getName(), managedAttribute.description(),
                        readable, writable, isIs);
            }

            return null;

        }

        /**
         * Helper method. Handles a method that has a {@link ManagedOperation}
         * annotation. It creates an MBeanOperationInfo from the method.
         *
         * @param method the method that is annotated with
         * {@link ManagedOperation}
         */
        private void handleManagedOperation(Method method) {

            // build the MBeanParameterInfo array from the parameters of the method
            MBeanParameterInfo[] mBeanParameters = buildMBeanParameters(method);

            ManagedOperation managedOperation = method.getAnnotation(ManagedOperation.class);
            Impact impact = managedOperation.impact();

            mBeanOperations.add(new MBeanOperationInfo(method.getName(), managedOperation.description(),
                    mBeanParameters, method.getReturnType().getName(), impact.getCode()));

        }

        /**
         * Helper method. Builds an array of MBeanParameterInfo objects that
         * represent the parameters, using the DescriptorFields annotation style from the JMX 2.0 specification.
         *
         * @param method the method having parameter info build for it.
         *
         * @return an array of MBeanParameterInfo objects describing the Method.
         */
        private MBeanParameterInfo[] buildMBeanParameters(Method method) {
            Class[] paramsTypes = method.getParameterTypes();

            HashMap<String, String> paramNames = new HashMap<String, String>();
            HashMap<String, String> paramDesc = new HashMap<String, String>();
            
            boolean hasDescriptorFields = method.isAnnotationPresent(DescriptorFields.class);
            if (hasDescriptorFields) {
                String[] pairs = method.getAnnotation(DescriptorFields.class).value();
                for (int i = 0; i < pairs.length; i++) {
                    String[] keydesc = pairs[i].split(";");
                    String[] keyval = keydesc[0].split("=");
                    
                    String desc = "";
                    if (keydesc.length > 1) {
                        desc = keydesc[1];
                    }
                    
                    paramNames.put(keyval[0], keyval[1]);
                    paramDesc.put(keyval[0], desc);
                }
            }

            MBeanParameterInfo[] mBeanParameters = new MBeanParameterInfo[paramsTypes.length];

            for (int i = 0; i < paramsTypes.length; i++) {
                String name = "p" + i;
                String description = "";
                
                if (paramNames.containsKey(name)) {
                    name = paramNames.get(name);
                }
                
                if (paramDesc.containsKey(name)) {
                    description = paramDesc.get(name);
                }
                
                MBeanParameterInfo parameterInfo = new MBeanParameterInfo(name, paramsTypes[i].getName(), description);
                mBeanParameters[i] = parameterInfo;
            }

            return mBeanParameters;
        }
    }
}
