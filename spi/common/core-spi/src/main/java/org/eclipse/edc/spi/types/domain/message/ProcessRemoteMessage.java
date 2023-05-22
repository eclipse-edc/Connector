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
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.spi.types.domain.message;

import org.jetbrains.annotations.NotNull;

/**
 * A remote message that conveys state modifications of a process. These messages are idempotent.
 */
public interface ProcessRemoteMessage extends RemoteMessage {

    /**
     * Returns the unique message id.
     *
     * @return the id;
     */
    @NotNull
    String getId();

    /**
     * Returns the process id.
     *
     * @return the processId.
     */
    @NotNull
    String getProcessId();

    void setProtocol(String protocol);
}
