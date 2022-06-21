/*
 *  Copyright (c) 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.dataspaceconnector.spi.observe;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ObservableImplTest {

    private final Observable<Object> observable = new ObservableImpl<>();

    @Test
    void shouldRegisterListener() {
        var listener = new TestListener();

        observable.registerListener(listener);

        assertThat(observable.getListeners()).containsOnly(listener);
    }

    @Test
    void shouldRegisterMultipleListeners() {
        var listener = new TestListener();
        var listener2 = new TestListener();
        observable.registerListener(listener);
        observable.registerListener(listener2);

        assertThat(observable.getListeners()).hasSize(2).containsOnly(listener, listener2);
    }

    @Test
    void shouldNotRegisterDuplicatedListeners() {
        var listener = new TestListener();
        observable.registerListener(listener);
        observable.registerListener(listener);

        assertThat(observable.getListeners()).hasSize(1).containsOnly(listener);
    }

    @Test
    void shouldUnregisterListener() {
        var listener = new TestListener();
        observable.registerListener(listener);

        observable.unregisterListener(listener);

        assertThat(observable.getListeners()).doesNotContain(listener);
    }

    @Test
    void shouldNotUnregisterUnregisteredListener() {
        var listener = new TestListener();
        observable.registerListener(listener);

        var listener2 = new TestListener();
        observable.unregisterListener(listener2);

        assertThat(observable.getListeners()).doesNotContain(listener2).containsOnly(listener);
    }

    private static class TestListener {
    }

}
