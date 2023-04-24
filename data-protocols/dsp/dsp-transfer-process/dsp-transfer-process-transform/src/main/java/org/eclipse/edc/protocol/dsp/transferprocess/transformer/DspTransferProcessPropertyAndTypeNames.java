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

package org.eclipse.edc.protocol.dsp.transferprocess.transformer;

/**
 * Dataspace protocol types and attributes for catalog request.
 */
public interface DspTransferProcessPropertyAndTypeNames {
    
    String DSPACE_PREFIX = "dspace";

    String DSPACE_SCHEMA = "https://w3id.org/dspace/v0.8/"; // TODO to be defined

    String DSPACE_TRANSFERPROCESS_REQUEST_TYPE = DSPACE_SCHEMA + "TransferRequestMessage";

    String DSPACE_TRANSFER_START_TYPE = DSPACE_SCHEMA + "TransferStartMessage";

    String DSPACE_TRANSFER_COMPLETION_TYPE = DSPACE_SCHEMA + "TransferCompletionMessage";

    String DSPACE_TRANSFER_TERMINATION_TYPE = DSPACE_SCHEMA + "TransferTerminationMessage";

    String DSPACE_TRANSFERPROCESS_TYPE = DSPACE_SCHEMA + "TransferProcess";

    String DSPACE_CONTRACTAGREEMENT_TYPE = DSPACE_SCHEMA + "agreementId";

    String DSPACE_CALLBACKADDRESS_TYPE = DSPACE_SCHEMA + "callbackAddress";

    String DSPACE_PROCESSID_TYPE = DSPACE_SCHEMA + "processId";

    String DSPACE_DATAADDRESS_TYPE = DSPACE_SCHEMA + "dataAddress";

    String DSPACE_CORRELATIONID_TYPE = DSPACE_SCHEMA + "correlationId";

    String DSPACE_STATE_TYPE = DSPACE_SCHEMA + "state";
}
