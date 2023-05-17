/*
 *  Copyright (c) 2023 Fraunhofer Institute for Software and Systems Engineering
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer Institute for Software and Systems Engineering - initial API and implementation
 *
 */

package org.eclipse.edc.protocol.dsp.type;

import static org.eclipse.edc.jsonld.spi.Namespaces.DSPACE_SCHEMA;

/**
 * Dataspace protocol types and attributes for catalog request.
 */
public interface DspTransferProcessPropertyAndTypeNames {

    String DSPACE_TYPE_TRANSFER_REQUEST_MESSAGE = DSPACE_SCHEMA + "TransferRequestMessage";
    String DSPACE_TYPE_TRANSFER_START_MESSAGE = DSPACE_SCHEMA + "TransferStartMessage";
    String DSPACE_TYPE_TRANSFER_COMPLETION_MESSAGE = DSPACE_SCHEMA + "TransferCompletionMessage";
    String DSPACE_TYPE_TRANSFER_TERMINATION_MESSAGE = DSPACE_SCHEMA + "TransferTerminationMessage";
    String DSPACE_TYPE_TRANSFER_PROCESS = DSPACE_SCHEMA + "TransferProcess";
    String DSPACE_TYPE_TRANSFER_ERROR = DSPACE_SCHEMA + "TransferError";

    String DSPACE_PROPERTY_CONTRACT_AGREEMENT_ID = DSPACE_SCHEMA + "agreementId";
    String DSPACE_PROPERTY_DATA_ADDRESS = DSPACE_SCHEMA + "dataAddress";
    String DSPACE_PROPERTY_CORRELATION_ID = DSPACE_SCHEMA + "correlationId";
}
