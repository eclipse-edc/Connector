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

CREATE TABLE IF NOT EXISTS assets
(
    id VARCHAR(255) NOT NULL,
    PRIMARY KEY (id)
);

---

CREATE TABLE IF NOT EXISTS properties
(
    asset_id VARCHAR(255) NOT NULL,
    k        VARCHAR(255) NOT NULL,
    v        VARCHAR(65535),
    PRIMARY KEY (asset_id, k),
    FOREIGN KEY (asset_id) REFERENCES assets (id) ON DELETE CASCADE
);

---

CREATE INDEX IF NOT EXISTS kv_index ON properties(k, v);
