/*
 *  Copyright (c) 2023 Fraunhofer-Gesellschaft zur Förderung der angewandten Forschung e.V.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer-Gesellschaft zur Förderung der angewandten Forschung e.V. - initial API and implementation
 *
 */

package org.eclipse.edc.protocol.dsp.spi.type;

/**
 * Dataspace protocol types and attributes for catalog request.
 */
public interface DspTransferProcessPropertyAndTypeNames {

    String DSPACE_TYPE_TRANSFER_REQUEST_MESSAGE_TERM = "TransferRequestMessage";
    String DSPACE_TYPE_TRANSFER_START_MESSAGE_TERM = "TransferStartMessage";
    String DSPACE_TYPE_TRANSFER_COMPLETION_MESSAGE_TERM = "TransferCompletionMessage";
    String DSPACE_TYPE_TRANSFER_SUSPENSION_MESSAGE_TERM = "TransferSuspensionMessage";
    String DSPACE_TYPE_TRANSFER_TERMINATION_MESSAGE_TERM = "TransferTerminationMessage";
    String DSPACE_TYPE_TRANSFER_PROCESS_TERM = "TransferProcess";
    String DSPACE_TYPE_TRANSFER_ERROR_TERM = "TransferError";
    String DSPACE_PROPERTY_CONTRACT_AGREEMENT_ID_TERM = "agreementId";
    String DSPACE_PROPERTY_DATA_ADDRESS_TERM = "dataAddress";

    String DSPACE_VALUE_TRANSFER_STATE_REQUESTED_TERM = "REQUESTED";
    String DSPACE_VALUE_TRANSFER_STATE_STARTED_TERM = "STARTED";
    String DSPACE_VALUE_TRANSFER_STATE_COMPLETED_TERM = "COMPLETED";
    String DSPACE_VALUE_TRANSFER_STATE_SUSPENDED_TERM = "SUSPENDED";
    String DSPACE_VALUE_TRANSFER_STATE_TERMINATED_TERM = "TERMINATED";
}
