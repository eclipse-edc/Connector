/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.spi;

public interface CoreConstants {
    String JSON_LD = "json-ld";
    String EDC_PREFIX = "edc";
    //todo: this must be replaced once we have a default EDC schema!
    String EDC_NAMESPACE = "https://foo.bar.org/ds/schema/";
}
