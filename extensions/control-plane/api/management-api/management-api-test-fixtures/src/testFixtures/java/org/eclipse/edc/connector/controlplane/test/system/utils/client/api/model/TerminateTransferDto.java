/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.connector.controlplane.test.system.utils.client.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * DTO representation of a Terminate Transfer request.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class TerminateTransferDto extends Typed {

    private final String reason;

    public TerminateTransferDto(String reason) {
        super("TerminateTransfer");
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }

}
