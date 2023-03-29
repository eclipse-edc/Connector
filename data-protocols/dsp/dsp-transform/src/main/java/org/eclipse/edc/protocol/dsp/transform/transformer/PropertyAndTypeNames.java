/*
 *  Copyright (c) 2023 Fraunhofer Institute for Software and Systems Engineering
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer Institute for Software and Systems Engineering - initial API and implementation
 *
 */

package org.eclipse.edc.protocol.dsp.transform.transformer;

import static org.eclipse.edc.protocol.dsp.transform.transformer.Namespaces.DCAT_SCHEMA;
import static org.eclipse.edc.protocol.dsp.transform.transformer.Namespaces.DCT_SCHEMA;
import static org.eclipse.edc.protocol.dsp.transform.transformer.Namespaces.ODRL_SCHEMA;

/**
 * Collection of DCAT, DCT and ODRL type and attribute names.
 */
public interface PropertyAndTypeNames {
    
    String DCAT_CATALOG_TYPE = DCAT_SCHEMA + "Catalog";
    String DCAT_DATASET_TYPE = DCAT_SCHEMA + "Dataset";
    String DCAT_DISTRIBUTION_TYPE = DCAT_SCHEMA + "Distribution";
    String DCAT_DATA_SERVICE_TYPE = DCAT_SCHEMA + "DataService";
    
    String DCAT_DATA_SERVICE_ATTRIBUTE = DCAT_SCHEMA + "service";
    String DCAT_DATASET_ATTRIBUTE = DCAT_SCHEMA + "dataset";
    String DCAT_DISTRIBUTION_ATTRIBUTE = DCAT_SCHEMA + "distribution";
    String DCAT_ACCESS_SERVICE_ATTRIBUTE = DCAT_SCHEMA + "accessService";
    String ODRL_POLICY_ATTRIBUTE = ODRL_SCHEMA + "hasPolicy";
    
    String DCT_FORMAT_ATTRIBUTE = DCT_SCHEMA + "format";
    String DCT_TERMS_ATTRIBUTE = DCT_SCHEMA + "terms";
    String DCT_ENDPOINT_URL_ATTRIBUTE = DCT_SCHEMA + "endpointUrl";
    
    String ODRL_PERMISSION_ATTRIBUTE = ODRL_SCHEMA + "permission";
    String ODRL_PROHIBITION_ATTRIBUTE = ODRL_SCHEMA + "prohibition";
    String ODRL_OBLIGATION_ATTRIBUTE = ODRL_SCHEMA + "obligation";
    String ODRL_ACTION_ATTRIBUTE = ODRL_SCHEMA + "action";
    String ODRL_ACTION_TYPE_ATTRIBUTE = ODRL_SCHEMA + "type";
    String ODRL_CONSEQUENCE_ATTRIBUTE = ODRL_SCHEMA + "consequence";
    String ODRL_INCLUDED_IN_ATTRIBUTE = ODRL_SCHEMA + "includedIn";
    String ODRL_REFINEMENT_ATTRIBUTE = ODRL_SCHEMA + "refinement";
    String ODRL_CONSTRAINT_ATTRIBUTE = ODRL_SCHEMA + "constraint";
    String ODRL_LEFT_OPERAND_ATTRIBUTE = ODRL_SCHEMA + "leftOperand";
    String ODRL_OPERATOR_ATTRIBUTE = ODRL_SCHEMA + "operator";
    String ODRL_RIGHT_OPERAND_ATTRIBUTE = ODRL_SCHEMA + "rightOperand";
    String ODRL_DUTY_ATTRIBUTE = ODRL_SCHEMA + "duty";
    
}
