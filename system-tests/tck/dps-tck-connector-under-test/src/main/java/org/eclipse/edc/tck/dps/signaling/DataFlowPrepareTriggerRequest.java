/*
 *  Copyright (c) 2026 Think-it GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Think-it GmbH - initial API and implementation
 *
 */

package org.eclipse.edc.tck.dps.signaling;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Request body sent by the TCK to trigger a DataFlowPrepareMessage dispatch.
 */
public record DataFlowPrepareTriggerRequest(
        @JsonProperty("processId") String processId,
        @JsonProperty("agreementId") String agreementId,
        @JsonProperty("datasetId") String datasetId,
        @JsonProperty("dataPlaneUrl") String dataPlaneUrl,
        @JsonProperty("dspUrl") String dspUrl
) {
}
