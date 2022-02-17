package org.eclipse.dataspaceconnector.boot.system;

import org.eclipse.dataspaceconnector.boot.system.injection.InjectorImpl;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.system.Feature;
import org.eclipse.dataspaceconnector.spi.system.Inject;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.system.injection.EdcInjectionException;
import org.eclipse.dataspaceconnector.spi.system.injection.FieldInjectionPoint;
import org.eclipse.dataspaceconnector.spi.system.injection.InjectionContainer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class InjectorTest {

    private InjectorImpl injector;
    private Monitor monitor;
    private ServiceExtensionContext context;

    @BeforeEach
    void setup() {
        injector = new InjectorImpl();
        monitor = mock(Monitor.class);
        context = mock(ServiceExtensionContext.class);
        when(context.getMonitor()).thenReturn(monitor);
    }

    @AfterEach
    void teardown() {
        verify(context).getMonitor();
        verifyNoMoreInteractions(context, monitor);
    }

    @Test
    @DisplayName("Testing ServiceExtension with no injection points")
    void templateWithNoInjectionPoints() {
        var serviceExtension = new EmptyTestExtension();
        var template = new InjectionContainer<>(serviceExtension, Collections.emptySet());

        injector.inject(template, context);

        verify(context, never()).getService(any(), anyBoolean());
    }

    @Test
    @DisplayName("All injection points of a service are satisfied")
    void allInjectionPointsSatisfied() throws NoSuchFieldException {
        var serviceExtension = new TestServiceExtension();
        var field = serviceExtension.getClass().getDeclaredField("someObject");
        var template = new InjectionContainer<>(serviceExtension, Set.of(new FieldInjectionPoint<>(serviceExtension, field, "edc:test:feature:someobject")));
        when(context.getService(eq(SomeObject.class), anyBoolean())).thenReturn(new SomeObject());

        injector.inject(template, context);

        assertThat(serviceExtension.someObject).isNotNull();
        verify(context).getService(eq(SomeObject.class), anyBoolean());
    }

    @Test
    @DisplayName("Injection point of a service is not satisfied")
    void notAllInjectionPointsSatisfied_shouldThrowException() throws NoSuchFieldException {
        var serviceExtension = new TestServiceExtension();
        var field = serviceExtension.getClass().getDeclaredField("someObject");
        var template = new InjectionContainer<>(serviceExtension, Set.of(new FieldInjectionPoint<>(serviceExtension, field, "edc:test:feature:someobject")));
        var rootCauseException = new EdcException("Service not found");
        when(context.getService(SomeObject.class, false)).thenThrow(rootCauseException);

        assertThatThrownBy(() -> injector.inject(template, context)).isInstanceOf(EdcInjectionException.class).hasCause(rootCauseException);
        assertThat(serviceExtension.someObject).isNull();

        verify(context).getService(SomeObject.class, false);
        verify(monitor).warning(anyString(), any());

    }

    @Test
    @DisplayName("Cannot set value of the injected field")
    void cannotSetInjectionPoint_shouldThrowException() throws NoSuchFieldException, IllegalAccessException {
        var serviceExtension = new TestServiceExtension();
        var field = serviceExtension.getClass().getDeclaredField("someObject");
        var injectionPoint = spy(new FieldInjectionPoint<>(serviceExtension, field, "edc:test:feature:someobject"));
        var template = new InjectionContainer<>(serviceExtension, Set.of(injectionPoint));

        var value = new SomeObject();
        when(context.getService(eq(SomeObject.class), anyBoolean())).thenReturn(value);

        doThrow(new IllegalAccessException("test")).when(injectionPoint).setTargetValue(value);

        assertThatThrownBy(() -> injector.inject(template, context)).isInstanceOf(EdcInjectionException.class).hasCauseInstanceOf(IllegalAccessException.class);
        assertThat(serviceExtension.someObject).isNull();
        verify(context).getService(eq(SomeObject.class), anyBoolean());
        verify(monitor).warning(anyString(), any());
    }

    private static class TestServiceExtension implements ServiceExtension {
        @Inject
        private SomeObject someObject;
    }

    @Feature("edc:test:feature:someobject")
    private static class SomeObject {
    }

    private static class EmptyTestExtension implements ServiceExtension {
    }
}
