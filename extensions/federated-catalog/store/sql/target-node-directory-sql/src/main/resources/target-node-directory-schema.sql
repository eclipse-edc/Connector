/*
 *  Copyright (c) 2024 Amadeus IT Group
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Amadeus IT Group - initial API and implementation
 *
 */

-- only intended for and tested with Postgres!
CREATE TABLE IF NOT EXISTS edc_target_node_directory
(
    id                      VARCHAR PRIMARY KEY NOT NULL,
    name                    VARCHAR NOT NULL,
    target_url              VARCHAR NOT NULL,
    supported_protocols     JSON
);

COMMENT ON COLUMN edc_target_node_directory.supported_protocols IS 'List<String> serialized as JSON';