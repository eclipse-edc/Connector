package org.eclipse.dataspaceconnector.core.system;

import org.eclipse.dataspaceconnector.spi.system.InjectionContainer;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;

/**
 * Sets all fields of a {@link ServiceExtension}, that are annotated with {@code @Inject} by attempting to get the
 * corresponding service from the {@link ServiceExtensionContext}.
 * <p>
 * Injectors must throw an {@link EdcInjectionException} should they fail to set any field's value.
 */
@FunctionalInterface
public interface Injector {
    /**
     * Attempts to set all fields (i.e. {@link InjectionContainer#getInjectionPoints()}) of a service extension (i.e. {@link InjectionContainer#getInjectionTarget()})
     * by attempting to resolve as service of the field's type from the context.
     */
    <T> T inject(InjectionContainer<T> serviceExtension, ServiceExtensionContext context);
}
