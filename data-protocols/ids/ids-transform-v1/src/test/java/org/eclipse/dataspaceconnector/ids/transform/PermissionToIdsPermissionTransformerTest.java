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
 *       Daimler TSS GmbH - Initial Implementation
 *
 */

package org.eclipse.dataspaceconnector.ids.transform;

import org.eclipse.dataspaceconnector.ids.spi.IdsId;
import org.eclipse.dataspaceconnector.ids.spi.transform.TransformerContext;
import org.eclipse.dataspaceconnector.policy.model.Action;
import org.eclipse.dataspaceconnector.policy.model.Constraint;
import org.eclipse.dataspaceconnector.policy.model.Duty;
import org.eclipse.dataspaceconnector.policy.model.Permission;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PermissionToIdsPermissionTransformerTest {

    private static final URI PERMISSION_ID = URI.create("urn:permission:456uz984390236s");
    private static final String TARGET = "https://target.com";
    private static final URI TARGET_URI = URI.create(TARGET);
    private static final String ASSIGNER = "https://assigner.com";
    private static final URI ASSIGNER_URI = URI.create(ASSIGNER);
    private static final String ASSIGNEE = "https://assignee.com";
    private static final URI ASSIGNEE_URI = URI.create(ASSIGNEE);

    private PermissionToIdsPermissionTransformer transformer;

    private Permission permission;
    private TransformerContext context;

    @BeforeEach
    void setUp() {
        transformer = new PermissionToIdsPermissionTransformer();
        permission = mock(Permission.class);
        context = mock(TransformerContext.class);
    }

    @Test
    void testThrowsNullPointerExceptionForAll() {
        Assertions.assertThrows(NullPointerException.class, () -> {
            transformer.transform(null, null);
        });
    }

    @Test
    void testThrowsNullPointerExceptionForContext() {
        Assertions.assertThrows(NullPointerException.class, () -> {
            transformer.transform(permission, null);
        });
    }

    @Test
    void testReturnsNull() {
        var result = transformer.transform(null, context);

        Assertions.assertNull(result);
    }

    @Test
    void testSuccessfulSimple() {
        Action edcAction = mock(Action.class);
        de.fraunhofer.iais.eis.Action idsAction = de.fraunhofer.iais.eis.Action.READ;
        Constraint edcConstraint = mock(Constraint.class);
        de.fraunhofer.iais.eis.Constraint idsConstraint = mock(de.fraunhofer.iais.eis.Constraint.class);
        Duty edcDuty = mock(Duty.class);
        de.fraunhofer.iais.eis.Duty idsDuty = mock(de.fraunhofer.iais.eis.Duty.class);

        when(permission.getTarget()).thenReturn(TARGET);
        when(permission.getAssigner()).thenReturn(ASSIGNER);
        when(permission.getAssignee()).thenReturn(ASSIGNEE);

        when(permission.getConstraints()).thenReturn(Collections.singletonList(edcConstraint));
        when(permission.getDuties()).thenReturn(Collections.singletonList(edcDuty));
        when(permission.getAction()).thenReturn(edcAction);
        when(context.transform(eq(edcAction), eq(de.fraunhofer.iais.eis.Action.class))).thenReturn(idsAction);
        when(context.transform(eq(edcConstraint), eq(de.fraunhofer.iais.eis.Constraint.class))).thenReturn(idsConstraint);
        when(context.transform(eq(edcDuty), eq(de.fraunhofer.iais.eis.Duty.class))).thenReturn(idsDuty);
        when(context.transform(any(IdsId.class), eq(URI.class))).thenReturn(PERMISSION_ID);

        var result = transformer.transform(permission, context);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(PERMISSION_ID, result.getId());
        Assertions.assertEquals(1, result.getAssigner().size());
        Assertions.assertEquals(ASSIGNER_URI, result.getAssigner().get(0));
        Assertions.assertEquals(1, result.getAssignee().size());
        Assertions.assertEquals(ASSIGNEE_URI, result.getAssignee().get(0));
        Assertions.assertEquals(1, result.getAction().size());
        Assertions.assertEquals(idsAction, result.getAction().get(0));
        Assertions.assertEquals(1, result.getConstraint().size());
        Assertions.assertEquals(idsConstraint, result.getConstraint().get(0));
    }
}
