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

import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.spi.system.ServiceExtension;

@Extension(value = SwitchFeatureExtension.NAME, featureKey = SwitchFeatureExtension.FEATURE_KEY, featureValue = "true")
public class SwitchFeatureExtension implements ServiceExtension {
    public static final String NAME = "SwitchFeatureExtension";
    public static final String FEATURE_KEY = "edc.feature.test.switch";
}
