package org.eclipse.dataspaceconnector.spi.system;

/**
 * Represents an auto-injectable property. Possible implementors are field injection points, constructor injection points, etc.
 *
 * @param <T> the type of the target object
 */
public interface InjectionPoint<T> {
    T getInstance();

    String getFeatureName();

    Class<?> getType();

    boolean isRequired();

    void setTargetValue(Object service) throws IllegalAccessException;
}
