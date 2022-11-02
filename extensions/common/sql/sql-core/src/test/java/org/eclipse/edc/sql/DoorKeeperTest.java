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
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.sql;

import org.eclipse.edc.spi.EdcException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

class DoorKeeperTest {

    @Test
    void closesCloseablesFromLastToFirst() throws Exception {
        var firstCloseable = mock(AutoCloseable.class);
        var secondCloseable = mock(AutoCloseable.class);
        var doorKeeper = new DoorKeeper();

        doorKeeper.takeCareOf(firstCloseable).takeCareOf(secondCloseable).close();

        var inOrder = inOrder(secondCloseable, firstCloseable);
        inOrder.verify(secondCloseable).close();
        inOrder.verify(firstCloseable).close();
    }

    @Test
    void wrapExtensionWithUncheckedEdcExtension() throws Exception {
        var closeable = mock(AutoCloseable.class);
        doThrow(Exception.class).when(closeable).close();
        var doorKeeper = new DoorKeeper();

        assertThatThrownBy(() -> doorKeeper.takeCareOf(closeable).close()).isInstanceOf(EdcException.class);
    }
}