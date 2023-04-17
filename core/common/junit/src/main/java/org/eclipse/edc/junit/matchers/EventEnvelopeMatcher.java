/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - Initial implementation
 *
 */

package org.eclipse.edc.junit.matchers;

import org.eclipse.edc.spi.event.Event;
import org.eclipse.edc.spi.event.EventEnvelope;
import org.mockito.ArgumentMatcher;

public class EventEnvelopeMatcher<T extends Event> implements ArgumentMatcher<EventEnvelope<T>> {

    private final Class<T> eventKind;

    private EventEnvelopeMatcher(Class<T> eventKind) {
        this.eventKind = eventKind;
    }

    public static <T extends Event> EventEnvelopeMatcher<T> isEnvelopeOf(Class<T> klass) {
        return new EventEnvelopeMatcher<>(klass);
    }

    public static <T extends B, B extends Event> EventEnvelopeMatcher<B> isEnvelopeOf(Class<T> klass, Class<B> baseKlass) {
        return new EventEnvelopeMatcher<>(baseKlass);
    }

    @Override
    public boolean matches(EventEnvelope<T> argument) {
        return eventKind.isAssignableFrom(argument.getPayload().getClass());
    }
}
