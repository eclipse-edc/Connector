package org.eclipse.dataspaceconnector.spi.system;

import org.jetbrains.annotations.NotNull;

/**
 * Factory object that is used to generate instances of objects based on their class.
 * One way to implement this is to generate a new instance on demand using reflection and dependency injection.
 */
@FunctionalInterface
@Feature("edc:core:object-factory")
public interface ObjectFactory {
    /**
     * Creates a new instance of a commandHandler
     *
     * @param clazz The object's class
     * @throws RuntimeException if a new instance could not be created
     */
    @NotNull <T> T constructInstance(Class<T> clazz);
}
