/*
 *  Copyright (c) 2023 Fraunhofer Institute for Software and Systems Engineering
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer Institute for Software and Systems Engineering - initial API and implementation
 *
 */

package org.eclipse.edc.protocol.dsp.type;

import static org.eclipse.edc.jsonld.spi.Namespaces.DSPACE_SCHEMA;

/**
 * This class provides generic dsp property and type names.
 */
public interface DspPropertyAndTypeNames {

    String DSPACE_PROPERTY_CODE = DSPACE_SCHEMA + "code";
    String DSPACE_PROPERTY_REASON = DSPACE_SCHEMA + "reason";
}
