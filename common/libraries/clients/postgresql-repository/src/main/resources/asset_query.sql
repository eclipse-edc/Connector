--
--  Copyright (c) 2021 Daimler TSS GmbH
--
--  This program and the accompanying materials are made available under the
--  terms of the Apache License, Version 2.0 which is available at
--  https://www.apache.org/licenses/LICENSE-2.0
--
--  SPDX-License-Identifier: Apache-2.0
--
--  Contributors:
--       Daimler TSS GmbH - Initial SQL Query
--

SELECT DISTINCT p.asset_id id
FROM properties p
WHERE asset_id IN (
    SELECT p.asset_id
    FROM properties p
    WHERE (p.k = ? and p.v = ?)
)
