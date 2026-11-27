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

package org.eclipse.edc.spi.telemetry;

/**
 * The feature that the extensions providing metrics are bound to. They are only loaded if {@link #FEATURE_KEY} is set to
 * {@link #ENABLED}.
 */
public final class ObservabilityFeature {

    public static final String FEATURE_KEY = "edc.feature.observability";
    public static final String ENABLED = "true";

    private ObservabilityFeature() {
    }
}
