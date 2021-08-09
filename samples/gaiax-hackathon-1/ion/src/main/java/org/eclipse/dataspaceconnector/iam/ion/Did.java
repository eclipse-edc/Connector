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
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.iam.ion;

import org.eclipse.dataspaceconnector.iam.ion.dto.DidState;
import org.eclipse.dataspaceconnector.iam.ion.model.DidOperation;
import org.eclipse.dataspaceconnector.iam.ion.model.IonRequest;

public interface Did {

    DidState getState();

    DidOperation getOperation(int index);

    String getUri();

    String getUriShort();

    String getSuffix();

    IonRequest create(Object options);

    IonRequest update(Object options);

    IonRequest recover(Object options);

    IonRequest deactivate();

//    IonRequest generateRequest(int operationIndex, Object options);
}
