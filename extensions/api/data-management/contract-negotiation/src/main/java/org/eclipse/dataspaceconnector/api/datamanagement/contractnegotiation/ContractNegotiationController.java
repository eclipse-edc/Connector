/*
 * Copyright (c) 2022 cluetec GmbH
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 * Contributors:
 *   cluetec GmbH - Initial API and Implementation
 */


package org.eclipse.dataspaceconnector.api.datamanagement.contractnegotiation;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.eclipse.dataspaceconnector.spi.contract.negotiation.store.ContractNegotiationStore;
import org.jetbrains.annotations.NotNull;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Consumes({MediaType.APPLICATION_JSON})
@Produces({MediaType.APPLICATION_JSON})
@Path("/v1/contractnegotitations")
public class ContractNegotiationController {
	private final ContractNegotiationStore contractNegotiationStore;

	// TODO remove dummy constructor of the ContractNegotiationDto class after controller logic was implemented
	public ContractNegotiationController(@NotNull ContractNegotiationStore contractNegotiationStore) {
		this.contractNegotiationStore = Objects.requireNonNull(contractNegotiationStore);
	}

	@GET
	@Path("/")
	public List<ContractNegotiationDto> getNegotiations() {
		var negotiations = new ArrayList<ContractNegotiationDto>();
		negotiations.add(new ContractNegotiationDto());

		return negotiations;
	}

	@GET
	@Path("/{id}")
	public ContractNegotiationDto getNegotiationById(@PathParam("id") String id) {
		// var negotiation = contractNegotiationStore.find(id);
		//
		// if (negotiation == null) {
		// 	return Response.status(404).build();
		// }
		//
		// return ContractNegotiationDto.fromContractNegotiation(negotiation);
		return new ContractNegotiationDto();
	}

	@GET
	@Path("/{id}/state")
	public String getNegotiationStateById(@PathParam("id") String id) {
		// var negotiation = contractNegotiationStore.find(id);
		//
		// if (negotiation == null) {
		// 	throw new ObjectNotFoundException();
		// }
		//
		// return new ContractNegotiationDto(negotiation).getState();
		var dto = new ContractNegotiationDto();
		return dto.getState();
	}

	@POST
	@Path("/{id}/cancel")
	public void cancelNegotiation(@PathParam("id") String id) {
		// var negotiation = contractNegotiationStore.find(id);
		//
		// if (negotiation == null) {
		// 	return Response.status(404).build();
		// }

		// TODO move Negotiation to the CANCELLING/CANCELLED state
		// TODO Throw IllegalStateException if not possible
	}

	@POST
	@Path("/{id}/decline")
	public void declineNegotiation(@PathParam("id") String id) {
		// var negotiation = contractNegotiationStore.find(id);
		//
		// if (negotiation == null) {
		// 	return Response.status(404).build();
		// }

		// TODO move Negotiation to the DECLINING/DECLINED state
		// TODO Throw IllegalStateException if not possible
	}
}
