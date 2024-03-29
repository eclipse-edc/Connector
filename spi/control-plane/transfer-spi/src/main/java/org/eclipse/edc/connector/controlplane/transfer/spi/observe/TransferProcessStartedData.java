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

package org.eclipse.edc.connector.controlplane.transfer.spi.observe;

import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess;
import org.eclipse.edc.spi.types.domain.DataAddress;

/**
 * Additional data when calling {@link TransferProcessListener#started} not stored in the {@link TransferProcess}
 */
public class TransferProcessStartedData {

    private DataAddress dataAddress;

    private TransferProcessStartedData() {
    }

    public DataAddress getDataAddress() {
        return dataAddress;
    }

    public static class Builder {

        private final TransferProcessStartedData data;

        private Builder(TransferProcessStartedData data) {
            this.data = data;
        }

        public static Builder newInstance() {
            return new Builder(new TransferProcessStartedData());
        }

        public Builder dataAddress(DataAddress dataAddress) {
            data.dataAddress = dataAddress;
            return this;
        }

        public TransferProcessStartedData build() {
            return data;
        }
    }
}
