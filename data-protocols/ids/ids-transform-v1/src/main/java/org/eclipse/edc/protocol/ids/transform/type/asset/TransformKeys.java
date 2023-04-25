/*
 *  Copyright (c) 2021 Daimler TSS GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Daimler TSS GmbH - Initial implementation
 *       Fraunhofer Institute for Software and Systems Engineering - refactoring
 *
 */

package org.eclipse.edc.protocol.ids.transform.type.asset;

import org.eclipse.edc.spi.CoreConstants;
import org.eclipse.edc.spi.types.domain.asset.Asset;

/**
 * IDS Transform Keys from the {@link Asset} properties.
 */
public class TransformKeys {
    public static final String KEY_ASSET_FILE_NAME = CoreConstants.EDC_NAMESPACE + "fileName";
    public static final String KEY_ASSET_BYTE_SIZE = CoreConstants.EDC_NAMESPACE + "byteSize";
    public static final String KEY_ASSET_FILE_EXTENSION = CoreConstants.EDC_NAMESPACE + "fileExtension";

}
