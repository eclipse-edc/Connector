package org.eclipse.dataspaceconnector.boot.system;

import org.eclipse.dataspaceconnector.boot.system.injection.InjectorImpl;
import org.eclipse.dataspaceconnector.boot.system.injection.ReflectiveObjectFactory;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.system.Inject;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.system.injection.InjectionPointScanner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ReflectiveObjectFactoryTest {

    private ReflectiveObjectFactory factory;

    @BeforeEach
    void setUp() {
        var mockedInjector = new InjectorImpl();
        var mockedContext = mock(ServiceExtensionContext.class);
        when(mockedContext.getService(eq(SomeService.class), anyBoolean())).thenReturn(new SomeService());

        factory = new ReflectiveObjectFactory(mockedInjector, new InjectionPointScanner(), mockedContext);
    }

    @Test
    void constructInstance() {
        var handler = factory.constructInstance(TestTargetObject.class);
        assertThat(handler).isNotNull()
                .extracting(TestTargetObject::getObject).isNotNull();
    }

    @Test
    void constructInstance_noDefaultCtor() {
        assertThatThrownBy(() -> factory.constructInstance(NoDefaultCtor.class)).isInstanceOf(EdcException.class).hasRootCauseInstanceOf(NoSuchMethodException.class);
    }

    @Test
    void constructInstance_nothingToInject() {
        var instance = factory.constructInstance(NoInjectionPoints.class);
        assertThat(instance).isNotNull();
    }

    public static class TestTargetObject {
        @Inject
        private SomeService obj;

        public SomeService getObject() {
            return obj;
        }
    }

    public static class NoDefaultCtor {
        private final String id;
        @Inject
        private SomeService obj;

        public NoDefaultCtor(String id) {
            this.id = id;
        }

        public SomeService getObject() {
            return obj;
        }
    }

    public static class NoInjectionPoints {

    }

    public static class SomeService {
    }
}
