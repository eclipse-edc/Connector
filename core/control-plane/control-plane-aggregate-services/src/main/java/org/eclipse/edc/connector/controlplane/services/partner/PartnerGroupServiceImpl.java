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

package org.eclipse.edc.connector.controlplane.services.partner;

import org.eclipse.edc.connector.controlplane.partner.spi.PartnerGroup;
import org.eclipse.edc.connector.controlplane.partner.spi.store.PartnerGroupStore;
import org.eclipse.edc.connector.controlplane.partner.spi.store.PartnerStore;
import org.eclipse.edc.connector.controlplane.services.query.QueryValidator;
import org.eclipse.edc.connector.controlplane.services.spi.partner.PartnerGroupService;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.transaction.spi.TransactionContext;

import java.util.List;

import static java.lang.String.format;
import static org.eclipse.edc.connector.controlplane.partner.spi.store.PartnerGroupStore.PARTNER_GROUP_NOT_FOUND;
import static org.eclipse.edc.participantcontext.spi.types.ParticipantResource.queryByParticipantContextId;
import static org.eclipse.edc.spi.query.Criterion.criterion;

public class PartnerGroupServiceImpl implements PartnerGroupService {

    private final PartnerGroupStore partnerGroupStore;
    private final PartnerStore partnerStore;
    private final TransactionContext transactionContext;
    private final QueryValidator queryValidator;

    public PartnerGroupServiceImpl(PartnerGroupStore partnerGroupStore, PartnerStore partnerStore,
                                   TransactionContext transactionContext, QueryValidator queryValidator) {
        this.partnerGroupStore = partnerGroupStore;
        this.partnerStore = partnerStore;
        this.transactionContext = transactionContext;
        this.queryValidator = queryValidator;
    }

    @Override
    public ServiceResult<PartnerGroup> findById(String participantContextId, String id) {
        return transactionContext.execute(() -> {
            var group = partnerGroupStore.findById(participantContextId, id);
            return group == null
                    ? ServiceResult.notFound(format(PARTNER_GROUP_NOT_FOUND, id, participantContextId))
                    : ServiceResult.success(group);
        });
    }

    @Override
    public ServiceResult<List<PartnerGroup>> search(QuerySpec query) {
        return queryValidator.validate(query)
                .flatMap(validation -> validation.failed()
                        ? ServiceResult.badRequest(format("Error validating schema: %s", validation.getFailureDetail()))
                        : ServiceResult.success(transactionContext.execute(() -> partnerGroupStore.query(query))));
    }

    @Override
    public ServiceResult<PartnerGroup> create(PartnerGroup group) {
        return transactionContext.execute(() -> validate(group)
                .compose(v -> ServiceResult.from(partnerGroupStore.create(group)).map(it -> group)));
    }

    @Override
    public ServiceResult<PartnerGroup> update(PartnerGroup group) {
        return transactionContext.execute(() -> validate(group)
                .compose(v -> ServiceResult.from(partnerGroupStore.update(group)).map(it -> group)));
    }

    @Override
    public ServiceResult<PartnerGroup> delete(String participantContextId, String id) {
        return transactionContext.execute(() -> {
            var group = partnerGroupStore.findById(participantContextId, id);
            if (group == null) {
                return ServiceResult.notFound(format(PARTNER_GROUP_NOT_FOUND, id, participantContextId));
            }
            var referencing = queryByParticipantContextId(participantContextId)
                    .filter(criterion("groupIds", "contains", id))
                    .limit(1)
                    .build();
            if (!partnerStore.query(referencing).isEmpty()) {
                return ServiceResult.conflict(format("PartnerGroup %s cannot be deleted as it is referenced by at least one partner", id));
            }
            return ServiceResult.from(partnerGroupStore.deleteById(participantContextId, id)).map(it -> group);
        });
    }

    private ServiceResult<Void> validate(PartnerGroup group) {
        if (group.getName().isBlank()) {
            return ServiceResult.badRequest("PartnerGroup name must not be blank");
        }
        if (group.getParticipantContextId() == null) {
            return ServiceResult.badRequest("PartnerGroup must belong to a participant context");
        }
        return ServiceResult.success();
    }
}
