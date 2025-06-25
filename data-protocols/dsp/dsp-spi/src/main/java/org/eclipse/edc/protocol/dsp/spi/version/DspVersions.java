/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.protocol.dsp.spi.version;

import org.eclipse.edc.protocol.spi.ProtocolVersion;

public interface DspVersions {

    String V_2024_1_VERSION = "2024/1";
    String V_2024_1_PATH = "/" + V_2024_1_VERSION;
    ProtocolVersion V_2024_1 = new ProtocolVersion(V_2024_1_VERSION, V_2024_1_PATH);

    String V_08_VERSION = "v0.8";
    String V_08_PATH = "/";
    ProtocolVersion V_08 = new ProtocolVersion(V_08_VERSION, V_08_PATH);


    String V_2025_1_VERSION = "2025-1";
    String V_2025_1_PATH = "/" + V_2025_1_VERSION;
    ProtocolVersion V_2025_1 = new ProtocolVersion(V_2025_1_VERSION, V_2025_1_PATH);

}
