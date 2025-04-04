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

package org.eclipse.edc.protocol.dsp.http.spi.types;

import org.eclipse.edc.protocol.dsp.spi.version.DspVersions;

/**
 * Provides the name to be used as reference to the dataspace protocol in remote messages.
 */
public class HttpMessageProtocol {

    // When not explicit the default will be v0.8 for backward compatibility
    public static final String DATASPACE_PROTOCOL_HTTP = "dataspace-protocol-http";
    public static final String DATASPACE_PROTOCOL_HTTP_SEPARATOR = ":";
    public static final String DATASPACE_PROTOCOL_HTTP_V_2024_1 = DATASPACE_PROTOCOL_HTTP + DATASPACE_PROTOCOL_HTTP_SEPARATOR + DspVersions.V_2024_1_VERSION;
    public static final String DATASPACE_PROTOCOL_HTTP_V_2025_1 = DATASPACE_PROTOCOL_HTTP + DATASPACE_PROTOCOL_HTTP_SEPARATOR + DspVersions.V_2025_1_VERSION;

}