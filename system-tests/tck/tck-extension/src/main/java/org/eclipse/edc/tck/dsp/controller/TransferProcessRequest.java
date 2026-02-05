/*
 *  Copyright (c) 2025 Metaform Systems, Inc.
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

package org.eclipse.edc.tck.dsp.controller;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Initiates a transfer request.
 */
public record TransferProcessRequest(@JsonProperty("providerId") String providerId,
                                     @JsonProperty("connectorAddress") String connectorAddress,
                                     @JsonProperty("agreementId") String agreementId,
                                     @JsonProperty("format") String format) {
}
