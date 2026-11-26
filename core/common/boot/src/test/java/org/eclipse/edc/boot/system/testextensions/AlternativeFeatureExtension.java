/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.boot.system.testextensions;

import org.eclipse.edc.boot.system.TestObject;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.spi.system.ServiceExtension;

@Extension(value = "AlternativeFeatureExtension", featureKey = "edc.feature.test", featureValue = "alternative")
public class AlternativeFeatureExtension implements ServiceExtension {

    @Provider
    public TestObject testObject() {
        return new TestObject("alternative");
    }
}
