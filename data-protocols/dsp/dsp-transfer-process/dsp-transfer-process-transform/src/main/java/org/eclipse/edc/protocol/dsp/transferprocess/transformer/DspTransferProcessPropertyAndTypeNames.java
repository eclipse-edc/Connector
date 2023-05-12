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

import static org.eclipse.edc.jsonld.spi.Namespaces.DSPACE_SCHEMA;

/**
 * Dataspace protocol types and attributes for catalog request.
 */
public interface DspTransferProcessPropertyAndTypeNames {

    String DSPACE_TRANSFER_PROCESS_REQUEST_TYPE = DSPACE_SCHEMA + "TransferRequestMessage";

    String DSPACE_TRANSFER_START_TYPE = DSPACE_SCHEMA + "TransferStartMessage";

    String DSPACE_TRANSFER_COMPLETION_TYPE = DSPACE_SCHEMA + "TransferCompletionMessage";

    String DSPACE_TRANSFER_TERMINATION_TYPE = DSPACE_SCHEMA + "TransferTerminationMessage";

    String DSPACE_TRANSFER_PROCESS_TYPE = DSPACE_SCHEMA + "TransferProcess";

    String DSPACE_CONTRACT_AGREEMENT_ID = DSPACE_SCHEMA + "agreementId";

    String DSPACE_CALLBACK_ADDRESS = DSPACE_SCHEMA + "callbackAddress";

    String DSPACE_PROCESS_ID = DSPACE_SCHEMA + "processId";

    String DSPACE_DATA_ADDRESS = DSPACE_SCHEMA + "dataAddress";

    String DSPACE_CORRELATION_ID = DSPACE_SCHEMA + "correlationId";

    String DSPACE_STATE = DSPACE_SCHEMA + "state";

    String DSPACE_REASON = DSPACE_SCHEMA + "reason";

    String DSPACE_CODE = DSPACE_SCHEMA + "code";
}
