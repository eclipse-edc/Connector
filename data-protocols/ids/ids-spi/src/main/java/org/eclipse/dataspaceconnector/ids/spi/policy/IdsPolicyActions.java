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

package org.eclipse.dataspaceconnector.ids.spi.policy;

import org.eclipse.dataspaceconnector.policy.model.Action;

/**
 * Actions defined by IDS.
 */
public interface IdsPolicyActions {

    String USE = "idsc:USE";
    String COMPENSATE = "idsc:COMPENSATE";
    String DELETE = "idsc:DELETE";
    String GRANT_USE = "idsc:GRANT_USE";
    String DISTRIBUTE = "idsc:DISTRIBUTE";
    String READ = "idsc:READ";
    String RETRIEVE = "ids:retrieveOperation";
    String ANONYMIZE = "idsc:ANONYMIZE";
    String ENCRYPT = "idsc:ENCRYPT";

    Action USE_ACTION = Action.Builder.newInstance().type(USE).build();
    Action COMPENSATE_ACTION = Action.Builder.newInstance().type(COMPENSATE).build();
    Action DELETE_ACTION = Action.Builder.newInstance().type(DELETE).build();
    Action GRANT_USE_ACTION = Action.Builder.newInstance().type(GRANT_USE).build();
    Action DISTRIBUTE_ACTION = Action.Builder.newInstance().type(DISTRIBUTE).build();
    Action READ_ACTION = Action.Builder.newInstance().type(READ).build();
    Action RETRIEVE_ACTION = Action.Builder.newInstance().type(RETRIEVE).build();
    Action ANONYMIZE_ACTION = Action.Builder.newInstance().type(ANONYMIZE).build();
    Action ENCRYPT_ACTION = Action.Builder.newInstance().type(ENCRYPT).build();

}
