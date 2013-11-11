package net.gescobar.jmx.impl;

import static net.gescobar.jmx.util.StringUtils.capitalize;

import java.lang.reflect.Method;
import java.util.Iterator;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.DynamicMBean;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.ReflectionException;
import javax.management.RuntimeOperationsException;

/**
 * This is the DynamicMBean implementation that is returned from the {@link MBeanFactory#createMBean(Object)} method.
 *
 * @author German Escobar
 */
public class MBeanImpl implements DynamicMBean {

    /**
     * The object that is being instrumented.
     */
    private AbstractInstanceResolver resolver;

    /**
     * Describes the exposed information of the object.
     */
    private MBeanInfo mBeanInfo;

    /**
     * Constructor. Creates an instance using the Object instance that is going to be instrumented and the MBeanInfo that describes
     * the exposed information from the object.
     *
     * @param object the object that is going to be instrumented.
     * @param mBeanInfo describes the exposed information of the object.
     */
    public MBeanImpl(AbstractInstanceResolver resolver, MBeanInfo mBeanInfo) {
        this.resolver = resolver;
        this.mBeanInfo = mBeanInfo;
    }

    @Override
    public Object getAttribute(String attributeName) throws AttributeNotFoundException, MBeanException,
            ReflectionException {

        // check attribute_name is not null to avoid NullPointerException later on
        if (attributeName == null) {
            throw new RuntimeOperationsException(new IllegalArgumentException("Attribute name cannot be null"),
                    "Cannot invoke a getter of " + mBeanInfo.getClassName() + " with null attribute name");
        }

        MBeanAttributeInfo mBeanAttribute = findMBeanAttribute(attributeName);
        if (mBeanAttribute == null) {
            throw new AttributeNotFoundException("Cannot find " + attributeName + " attribute in "
                    + mBeanInfo.getClassName());
        }

        Method getterMethod = findGetterMethod(mBeanAttribute);
        if (getterMethod == null) {
            throw new AttributeNotFoundException("Cannot find " + attributeName + " attribute or equivalent getter in "
                    + mBeanInfo.getClassName());
        }

        try {
            return getterMethod.invoke(resolver.resolve());
        } catch (Exception e) {
            throw new MBeanException(e);
        }
    }

    @Override
    public AttributeList getAttributes(String[] attributesNames) {

        // check attributeNames is not null to avoid NullPointerException later on
        if (attributesNames == null) {
            throw new RuntimeOperationsException(new IllegalArgumentException("attributeNames[] cannot be null"),
                    "Cannot invoke a getter of " + mBeanInfo.getClassName());
        }

        AttributeList resultList = new AttributeList();

        // if attributeNames is empty, return an empty result list
        if (attributesNames.length == 0) {
            return resultList;
        }

        // build the result attribute list
        for (int i = 0; i < attributesNames.length; i++) {
            try {
                Object value = getAttribute(attributesNames[i]);
                resultList.add(new Attribute(attributesNames[i], value));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        return resultList;

    }

    @Override
    public void setAttribute(Attribute attribute) throws AttributeNotFoundException, InvalidAttributeValueException,
            MBeanException, ReflectionException {

        // check attribute is not null to avoid NullPointerException later on
        if (attribute == null) {
            throw new RuntimeOperationsException(new IllegalArgumentException("Attribute cannot be null"),
                    "Cannot invoke a setter of " + mBeanInfo.getClassName() + " with null attribute");
        }

        String attributeName = attribute.getName();
        Object value = attribute.getValue();

        if (attributeName == null) {
            throw new RuntimeOperationsException(new IllegalArgumentException("Attribute name cannot be null"),
                    "Cannot invoke the setter of " + mBeanInfo.getClassName() + " with null attribute name");
        }

        if (value == null) {
            throw (new InvalidAttributeValueException("Cannot set attribute " + attributeName + " to null"));
        }

        // try to set from the setter method
        MBeanAttributeInfo mBeanAttribute = findMBeanAttribute(attributeName);
        if (mBeanAttribute == null) {
            throw new AttributeNotFoundException("Cannot find " + attributeName + " attribute in "
                    + mBeanInfo.getClassName());
        }

        Class<?> type = null;
        try {
            type = findClass(mBeanAttribute.getType());
            if (!isAssignable(type, value.getClass())) {
                throw new InvalidAttributeValueException("Cannot set attribute " + attributeName + " to a "
                        + value.getClass().getName() + " object, " + type.getName() + " expected");
            }
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

        Method setterMethod = findSetterMethod(mBeanAttribute, type);
        if (setterMethod == null) {
            throw new RuntimeException("No setter method for attribute " + attributeName);
        }

        try {
            setterMethod.invoke(resolver.resolve(), value);
        } catch (Exception e) {
            throw new MBeanException(e);
        }

    }

    @Override
    public AttributeList setAttributes(AttributeList attributes) {

        // check attributes is not null to avoid NullPointerException later on
        if (attributes == null) {
            throw new RuntimeOperationsException(new IllegalArgumentException("AttributeList attributes cannot be null"),
                    "Cannot invoke a setter of " + mBeanInfo.getClassName());
        }

        AttributeList resultList = new AttributeList();

        // if attributeNames is empty, nothing more to do
        if (attributes.isEmpty()) {
            return resultList;
        }

        // for each attribute, try to set it and add to the result list if successful
        for (Iterator<Object> i = attributes.iterator(); i.hasNext();) {
            Attribute attr = (Attribute) i.next();
            try {
                setAttribute(attr);
                String name = attr.getName();
                Object value = getAttribute(name);
                resultList.add(new Attribute(name, value));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        return resultList;
    }

    @Override
    public Object invoke(String actionName, Object[] params, String[] signature) throws MBeanException,
            ReflectionException {

        // check operationName is not null to avoid NullPointerException later on
        if (actionName == null) {
            throw new RuntimeOperationsException(new IllegalArgumentException("Operation name cannot be null"),
                    "Cannot invoke a null operation in " + mBeanInfo.getClassName());
        }

        MBeanOperationInfo mBeanOperation = findMBeanOperation(actionName, signature);
        if (mBeanOperation == null) {
            throw new ReflectionException(new NoSuchMethodException(actionName),
                    "Cannot find the operation " + actionName + " with specified signature in "
                    + mBeanInfo.getClassName());
        }

        try {

            Method method = resolver.resolve().getClass().getMethod(actionName, getParametersTypes(signature));
            return method.invoke(resolver.resolve(), params);

        } catch (NoSuchMethodException e) {
            throw new ReflectionException(e);
        } catch (Exception e) {
            throw new ReflectionException(e);
        }
    }

    private Class<?>[] getParametersTypes(String[] signature) throws ClassNotFoundException {

        if (signature == null) {
            return null;
        }

        Class<?>[] paramTypes = new Class<?>[signature.length];
        for (int i = 0; i < signature.length; i++) {
            paramTypes[i] = findClass(signature[i]);
        }

        return paramTypes;
    }

    @Override
    public MBeanInfo getMBeanInfo() {
        return mBeanInfo;
    }

    /**
     * Helper method. Tries to find the MBeanAttributeInfo from the mBeanInfo instance variable.
     *
     * @param attributeName the name of the attribute we are looking for.
     *
     * @return the MBeanAttributeInfo for the attribute name or null if not found.
     */
    private MBeanAttributeInfo findMBeanAttribute(String attributeName) {

        MBeanAttributeInfo[] mBeanAttributes = mBeanInfo.getAttributes();
        for (MBeanAttributeInfo mBeanAttribute : mBeanAttributes) {

            if (mBeanAttribute.getName().equals(attributeName)) {
                return mBeanAttribute;
            }

        }

        return null;
    }

    /**
     * Helper method. Tries to find the
     * <code>MBeanOperationInfo</code> from the
     * <code>mBeanInfo</code> instance variable.
     *
     * @param operationName the name of the operation we are looking for.
     * @param receivedSignature the signature of the operation we are looking for.
     *
     * @return the MBeanOperationInfo that matches the <code>operationName</code> and <code>receivedSignature</code>.
     */
    private MBeanOperationInfo findMBeanOperation(String operationName, String[] receivedSignature) {

        MBeanOperationInfo[] mBeanOperations = mBeanInfo.getOperations();
        for (MBeanOperationInfo mBeanOperation : mBeanOperations) {

            if (mBeanOperation.getName().equals(operationName)
                    && isAssignableSignature(mBeanOperation.getSignature(), receivedSignature)) {
                return mBeanOperation;
            }

        }

        return null;
    }

    /**
     * Helper method. Validates if the
     * <code>receivedSignature</code> is assignable to the
     * <code>operationSignature</code>.
     *
     * @param operationSignature the signature of the operation.
     * @param receivedSignature the received signature.
     *
     * @return true if the <code>receivedSignature</code> is assignable to the <code>operationSignature</code>.
     */
    private boolean isAssignableSignature(MBeanParameterInfo[] operationSignature, String[] receivedSignature) {

        if (operationSignature == null && receivedSignature == null) {
            return true;
        }

        if ((operationSignature == null && receivedSignature != null)
                || (operationSignature != null && receivedSignature == null)) {
            return false;
        }

        if (operationSignature.length != receivedSignature.length) {
            return false;
        }

        try {
            for (int i = 0; i < operationSignature.length; i++) {

                Class<?> operationParamClass = findClass(operationSignature[i].getType());
                Class<?> receivedParamClass = findClass(receivedSignature[i]);

                if (!isAssignable(operationParamClass, receivedParamClass)) {
                    return false;
                }
            }
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

        return true;
    }

    /**
     * Helper method. Tries to find a getter method from the instrumented object using the
     * <code>mBeanAttribute<code>
     * info.
     *
     * @param mBeanAttribute the MBeanAttributeInfo from which we are trying to find the getter method.
     *
     * @return a java.lang.reflect.Method object that represents the getter method; null if no match.
     */
    private Method findGetterMethod(MBeanAttributeInfo mBeanAttribute) {
        String prefix = "get";
        if (mBeanAttribute.isIs()) {
            prefix = "is";
        }

        try {
            return resolver.resolve().getClass().getMethod(prefix + capitalize(mBeanAttribute.getName()));
        } catch (NoSuchMethodException e) {
            return null;
        }

    }

    /**
     * Helper method. Tries to find a setter method from the instrumented object using the
     * <code>mBeanAttribute</code> and the
     * <code>paramType</code>.
     *
     * @param mBeanAttribute the MBeanAttributeInfo from which we are trying to find the setter method.
     * @param attributeType the class of the attribute.
     *
     * @return a java.lang.reflect.Method object that represents the setter method; null if no match.
     */
    private Method findSetterMethod(MBeanAttributeInfo mBeanAttribute, Class<?> attributeType) {

        try {
            return resolver.resolve().getClass().getMethod("set" + capitalize(mBeanAttribute.getName()), attributeType);
        } catch (NoSuchMethodException e) {
            return null;
        }

    }

    /**
     * Helper method. Finds a class from its class name.
     *
     * @param className
     * @return
     * @throws ClassNotFoundException
     */
    private Class<?> findClass(String className) throws ClassNotFoundException {

        if (className == null) {
            throw new ClassNotFoundException(className);
        }

        if (Integer.TYPE.getName().equals(className)) {
            return Integer.TYPE;
        } else if (Byte.TYPE.getName().equals(className)) {
            return Byte.TYPE;
        } else if (Short.TYPE.getName().equals(className)) {
            return Short.TYPE;
        } else if (Long.TYPE.getName().equals(className)) {
            return Long.TYPE;
        } else if (Float.TYPE.getName().equals(className)) {
            return Float.TYPE;
        } else if (Double.TYPE.getName().equals(className)) {
            return Double.TYPE;
        } else if (Boolean.TYPE.getName().equals(className)) {
            return Boolean.TYPE;
        } else if (Character.TYPE.getName().equals(className)) {
            return Character.TYPE;
        }

        return Class.forName(className);

    }

    /**
     * Helper method. Checks if a class is assignable to another (i.e. the class must be the same or a subclass). Actually, the test
     * is done using the Class.isAssignableFrom(Class) method.
     *
     * @param to the class to which we are assigning.
     * @param from the class from which we are assigning.
     *
     * @return true if the <code>from</code> class is assignable to the <code>to</code> class.
     */
    private boolean isAssignable(final Class<?> to, final Class<?> from) {

        if (to == null) {
            throw new IllegalArgumentException("no to class specified");
        }

        if (from == null) {
            throw new IllegalArgumentException("no from class specified");
        }

        Class<?> toClass = to;
        if (toClass.isPrimitive()) {
            toClass = fromPrimitiveToObject(toClass);
        }

        Class<?> fromClass = from;
        if (fromClass.isPrimitive()) {
            fromClass = fromPrimitiveToObject(fromClass);
        }

        return toClass.isAssignableFrom(fromClass);
    }

    /**
     * Returns the wrapper class of the primitive class.
     *
     * @param primitive the primitive class for which we are looking the wrapper.
     *
     * @return the wrapper class of the primitive or the same class if not a primitive.
     */
    private Class<?> fromPrimitiveToObject(Class<?> primitive) {

        if (primitive.equals(Integer.TYPE)) {
            return Integer.class;
        } else if (primitive.equals(Byte.TYPE)) {
            return Byte.class;
        } else if (primitive.equals(Short.TYPE)) {
            return Short.class;
        } else if (primitive.equals(Long.TYPE)) {
            return Long.class;
        } else if (primitive.equals(Float.TYPE)) {
            return Float.class;
        } else if (primitive.equals(Double.TYPE)) {
            return Double.class;
        } else if (primitive.equals(Boolean.TYPE)) {
            return Boolean.class;
        } else if (primitive.equals(Character.TYPE)) {
            return Character.class;
        }

        return primitive;
    }
}
