/*
 *  Copyright (c) 2022 Fraunhofer Institute for Software and Systems Engineering
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

package org.eclipse.dataspaceconnector.ids.core.policy;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.fraunhofer.iais.eis.Constraint;
import de.fraunhofer.iais.eis.ConstraintImpl;
import de.fraunhofer.iais.eis.LeftOperand;
import de.fraunhofer.iais.eis.util.RdfResource;

import java.net.URI;
import java.util.Objects;
import javax.validation.constraints.NotNull;

/**
 * EDC extension of {@link de.fraunhofer.iais.eis.ConstraintImpl}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName("ids:Constraint")
@JsonDeserialize(using = CustomIdsConstraintDeserializer.class)
public class IdsConstraintImpl extends ConstraintImpl {

    @NotNull
    @JsonAlias({"ids:leftOperand", "leftOperand"})
    protected String leftOperand;

    public void setId(URI id) {
        super.id = id;
    }

    @Override
    @JsonIgnore
    public LeftOperand getLeftOperand() {
        return super._leftOperand;
    }

    @NotNull
    @JsonProperty("ids:leftOperand")
    public String getLeftOperandAsString() {
        return leftOperand;
    }

    public void setLeftOperand(String leftOperand) {
        this.leftOperand = leftOperand;
    }

    @Override
    public Constraint deepCopy() {
        IdsConstraintBuilder builder = new IdsConstraintBuilder();
        builder.leftOperand(this.leftOperand);
        builder.operator(this._operator);
        if (this._rightOperand != null) {
            builder.rightOperand(new RdfResource(this._rightOperand.getValue(), URI.create(this._rightOperand.getType())));
        }
        if (this._rightOperandReference != null) {
            builder.rightOperandReference(URI.create(this._rightOperandReference.toString()));
        }
        if (this._unit != null) {
            builder.unit(URI.create(this._unit.toString()));
        }
        if (this._pipEndpoint != null) {
            builder.pipEndpoint(URI.create(this._pipEndpoint.toString()));
        }
        return builder.build();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj == null) {
            return false;
        } else if (this.getClass() != obj.getClass()) {
            return false;
        } else {
            IdsConstraintImpl other = (IdsConstraintImpl) obj;
            return Objects.equals(this.leftOperand, other.leftOperand) &&
                    Objects.equals(this._operator, other._operator) &&
                    Objects.equals(this._rightOperand, other._rightOperand) &&
                    Objects.equals(this._rightOperandReference, other._rightOperandReference) &&
                    Objects.equals(this._unit, other._unit) &&
                    Objects.equals(this._pipEndpoint, other._pipEndpoint);
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(this._leftOperand,
                this._operator,
                this._rightOperand,
                this._rightOperandReference,
                this._unit,
                this._pipEndpoint);
    }

}
