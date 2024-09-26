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

package org.eclipse.edc.web.spi.configuration;

/**
 * Core Api Context constants
 */
public interface ApiContext {

    String MANAGEMENT = "management";
    String CONTROL = "control";
    String PROTOCOL = "protocol";
    String PUBLIC = "public";
    String VERSION = "version";
    String STS = "sts";
    String STS_ACCOUNTS = "accounts";

    @Deprecated(since = "0.6.4")
    String SIGNALING = "signaling";

}
