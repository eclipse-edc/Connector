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
public interface DspCatalogPropertyAndTypeNames {
    
    String DSPACE_PREFIX = "dspace";
    String DSPACE_SCHEMA = "https://w3id.org/dspace/v0.8/"; // TODO to be defined

    String DCT_PREFIX = "dct";
    String DCT_SCHEMA = "https://purl.org/dc/terms/";

    String DSPACE_TRANSFERPROCESS_REQUEST_TYPE = DSPACE_SCHEMA + "TransferProcessRequestMessage";

    String DSPACE_TRANSFER_START_TYPE = DSPACE_SCHEMA + "TransferStartMessage";

    String DSPACE_TRANSFER_COMPLETION_TYPE = DSPACE_SCHEMA + "TransferCompletionMessage";

    String DSPACE_TRANSFER_SUSPENSION_TYPE = DSPACE_SCHEMA + "TransferSuspensionMessage";

    String DSPACE_TRANSFER_TERMINATION_TYPE = DSPACE_SCHEMA + "TransferTerminationMessage";

    String DSPACE_CONTRACTAGREEMENT_TYPE = DSPACE_SCHEMA + "agreementId";

    String DSPACE_CALLBACKADDRESS_TYPE = DSPACE_SCHEMA + "callbackAddress";

    String DSPACE_PROCESSID_TYPE = DSPACE_SCHEMA + "processId";
}
