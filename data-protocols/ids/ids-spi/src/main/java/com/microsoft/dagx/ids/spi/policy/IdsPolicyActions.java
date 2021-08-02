/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package com.microsoft.dagx.ids.spi.policy;

import com.microsoft.dagx.policy.model.Action;

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
