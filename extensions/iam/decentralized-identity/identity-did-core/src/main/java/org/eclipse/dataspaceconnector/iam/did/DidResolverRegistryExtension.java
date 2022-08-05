/*
 *  Copyright (c) 2022 Amadeus
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

package org.eclipse.dataspaceconnector.iam.did;

import org.eclipse.dataspaceconnector.iam.did.resolution.DidResolverRegistryImpl;
import org.eclipse.dataspaceconnector.iam.did.spi.resolution.DidResolverRegistry;
import org.eclipse.dataspaceconnector.spi.system.Provider;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;


public class DidResolverRegistryExtension implements ServiceExtension {

    @Provider
    public DidResolverRegistry didResolverRegistry() {
        return new DidResolverRegistryImpl();
    }
}