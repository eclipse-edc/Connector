package org.eclipse.dataspaceconnector.boot;

import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.ObjectFactory;
import org.eclipse.dataspaceconnector.spi.system.InjectionContainer;
import org.eclipse.dataspaceconnector.spi.system.InjectionPointScanner;
import org.eclipse.dataspaceconnector.spi.system.Injector;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;

import static java.lang.String.format;

/**
 * This is a {@link ObjectFactory} that uses reflection and dependency injection to construct object instances.
 */
public class ReflectiveObjectFactory implements ObjectFactory {
    private final Injector injector;
    private final ServiceExtensionContext context;
    private final InjectionPointScanner injectionPointScanner;

    public ReflectiveObjectFactory(Injector injector, InjectionPointScanner injectionPointScanner, ServiceExtensionContext context) {
        this.injector = injector;
        this.injectionPointScanner = injectionPointScanner;
        this.context = context;
    }


    @Override
    public <T> @NotNull T constructInstance(Class<T> clazz) {
        var instance = getInstance(clazz); // will throw an exception e.g. if no suitable default CTor is found
        var ic = createInjectionContainer(instance);
        injector.inject(ic, context);
        return instance;
    }

    private <T> @NotNull InjectionContainer<T> createInjectionContainer(T instance) {
        var injectFields = injectionPointScanner.getInjectionPoints(instance);
        return new InjectionContainer<>(instance, injectFields);
    }

    @NotNull
    private <T> T getInstance(Class<T> clazz) {
        try {
            var defaultCtor = getDefaultCtor(clazz);
            defaultCtor.setAccessible(true);
            return defaultCtor.newInstance();
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException | NoSuchMethodException e) {
            throw new EdcException(e);
        }
    }

    /**
     * attempts to get the parameterless constructor for an object and throws an exception if none is found
     *
     * @param clazz The class of the object
     * @throws NoSuchMethodException if the specified class does not define a default ctor
     */
    @NotNull
    private <T> Constructor<T> getDefaultCtor(Class<T> clazz) throws NoSuchMethodException {
        return Arrays.stream(clazz.getConstructors()).filter(c -> c.getParameterCount() == 0)
                .findFirst()
                .map(c -> (Constructor<T>) c)
                .orElseThrow(() -> new NoSuchMethodException(format("Class %s does not have a parameterless public default constructor!", clazz)));
    }


}
