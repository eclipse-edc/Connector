/*
 *  Copyright (c) 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */
package org.eclipse.dataspaceconnector.iam.did.spi.resolver;

import java.util.LinkedHashMap;

/**
 * Resolves a DID against an external resolver service.
 */
public interface DidResolver {

    LinkedHashMap<String, Object> resolveDid(String didKey);

}
