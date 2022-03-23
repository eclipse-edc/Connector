/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
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

-- insert
INSERT INTO contract_negotiation (id, correlation_id, counterparty_id, counterparty_address)
VALUES ('test-cn1', 'corr1', 'other-id', 'http://other.com');

-- find by ID
SELECT *
FROM contract_negotiation
         LEFT OUTER JOIN contract_agreement ON contract_negotiation.contract_agreement_id = contract_agreement.id
WHERE contract_negotiation.id = 'test-cn1';


-- find for correlation id
SELECT *
FROM contract_negotiation
WHERE correlation_id = 'corr1';


-- find contract agreement
SELECT *
FROM contract_agreement
WHERE id = 'contract1';


-- delete - only if no contract
DELETE
FROM contract_negotiation
WHERE id = 'test-cn1'
  AND contract_agreement_id IS NULL;

-- acquire lease
-- 1. create lease
INSERT INTO lease (lease_id, leased_by, leased_at, lease_duration)
VALUES ('lease-1', 'yomama', NOW, default);

-- 2. update contract negotiation
UPDATE contract_negotiation
SET lease_id='lease-1'
WHERE id IN ('test-cn1');

-- break lease
DELETE
FROM lease
WHERE lease_id = (SELECT lease_id FROM contract_negotiation WHERE id = 'test-cn1');

-- break lease only when expired
DELETE
FROM lease
WHERE lease_id = (SELECT lease_id FROM contract_negotiation WHERE id = 'test-cn2')
  AND (NOW > (leased_at + lease.lease_duration));


-- update
UPDATE contract_negotiation
SET state=200,
    state_count=3,
    state_timestamp=12345,
    error_detail=NULL,
    contract_offers='{}',
    trace_context='{}'
WHERE id = 'test-cn1';


/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
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


-- select next for state
SELECT *
FROM contract_negotiation
WHERE state = 200
  AND (lease_id IS NULL OR
       lease_id IN (SELECT lease_id FROM lease WHERE (NOW > (leased_at + lease_duration))))
LIMIT 5;