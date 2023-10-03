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

package org.eclipse.edc.identitytrust;

public interface VcConstants {
    String VC_NAMESPACE_V1 = "https://www.w3.org/ns/credentials/v1/";
    String VC_NAMESPACE_V2 = "https://www.w3.org/ns/credentials/v2/";

    String VC_NAMESPACE = VC_NAMESPACE_V2; //todo: should we default to V1 or V2?
}
