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

package org.eclipse.edc.protocol.dsp.spi.type;

import static org.eclipse.edc.jsonld.spi.Namespaces.DSPACE_SCHEMA;

/**
 * This class provides generic dsp property and type names.
 */
public interface DspPropertyAndTypeNames {

    String DSPACE_PROPERTY_CODE_TERM = "code";
    @Deprecated(since = "0.14.0")
    String DSPACE_PROPERTY_CODE_IRI = DSPACE_SCHEMA + DSPACE_PROPERTY_CODE_TERM;
    String DSPACE_PROPERTY_REASON_TERM = "reason";
    @Deprecated(since = "0.14.0")
    String DSPACE_PROPERTY_REASON_IRI = DSPACE_SCHEMA + DSPACE_PROPERTY_REASON_TERM;
    String DSPACE_PROPERTY_CONSUMER_PID_TERM = "consumerPid";
    @Deprecated(since = "0.14.0")
    String DSPACE_PROPERTY_CONSUMER_PID_IRI = DSPACE_SCHEMA + DSPACE_PROPERTY_CONSUMER_PID_TERM;
    String DSPACE_PROPERTY_PROVIDER_PID_TERM = "providerPid";
    @Deprecated(since = "0.14.0")
    String DSPACE_PROPERTY_PROVIDER_PID_IRI = DSPACE_SCHEMA + DSPACE_PROPERTY_PROVIDER_PID_TERM;
    @Deprecated(since = "0.14.0")
    String DSPACE_PROPERTY_PROCESS_ID = DSPACE_SCHEMA + "processId";
    String DSPACE_PROPERTY_CALLBACK_ADDRESS_TERM = "callbackAddress";
    @Deprecated(since = "0.14.0")
    String DSPACE_PROPERTY_CALLBACK_ADDRESS_IRI = DSPACE_SCHEMA + DSPACE_PROPERTY_CALLBACK_ADDRESS_TERM;
    String DSPACE_PROPERTY_STATE_TERM = "state";
    @Deprecated(since = "0.14.0")
    String DSPACE_PROPERTY_STATE_IRI = DSPACE_SCHEMA + DSPACE_PROPERTY_STATE_TERM;
}
