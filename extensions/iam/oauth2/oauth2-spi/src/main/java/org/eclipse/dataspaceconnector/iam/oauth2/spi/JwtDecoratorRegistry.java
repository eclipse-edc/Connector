/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Amadeus - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.iam.oauth2.spi;

import java.util.Collection;

public interface JwtDecoratorRegistry {
    String FEATURE = "edc:iam:oauth2:jwtdecorator-registry";

    void register(JwtDecorator decorator);

    void unregister(JwtDecorator decorator);

    Collection<JwtDecorator> getAll();
}
