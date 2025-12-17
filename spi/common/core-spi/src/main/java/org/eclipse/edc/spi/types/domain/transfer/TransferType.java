/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.spi.types.domain.transfer;

import org.jetbrains.annotations.Nullable;

/**
 * Represent the transfer type.
 *
 * @param destinationType     the destination data address type designation.
 * @param responseChannelType an optional type designation for the response channel
 * @param flowType            the flow type.
 */
public record TransferType(String destinationType, FlowType flowType, @Nullable String responseChannelType) {
    public TransferType(String destinationType, FlowType flowType) {
        this(destinationType, flowType, null);
    }

    public String asString() {
        var responseChannel = responseChannelType == null ? "" : "-" + responseChannelType;
        return destinationType + "-" + flowType.name() + responseChannel;
    }
}
