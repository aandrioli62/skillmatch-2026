package com.skillmatch.contractservice.service;

import com.skillmatch.contractservice.dto.response.ContractResponse;
import com.skillmatch.contractservice.event.CandidatureAcceptedEvent;

import java.util.List;
import java.util.UUID;

public interface ContractService {

    /**
     * Creates a micro-contract in DRAFT status from an incoming candidature.accepted event.
     * Idempotent: if a contract already exists for the project (e.g. duplicate message
     * redelivery), the call is a no-op.
     */
    void createFromCandidatureAccepted(CandidatureAcceptedEvent.Data data);

    /**
     * Returns a single contract.
     *
     * @throws com.skillmatch.contractservice.exception.ContractNotFoundException if contract does not exist
     */
    ContractResponse getContract(UUID contractId);

    /**
     * Returns all contracts where the given user is the company party.
     */
    List<ContractResponse> listContractsByCompany(UUID companyId);

    /**
     * Returns all contracts where the given user is the professional party.
     */
    List<ContractResponse> listContractsByProfessional(UUID professionalId);

    /**
     * Signs a contract on behalf of the caller. The company party must sign first
     * (DRAFT -> PENDING_SIGNATURES), then the professional party (PENDING_SIGNATURES -> ACTIVE).
     *
     * @throws com.skillmatch.contractservice.exception.ContractNotFoundException          if contract does not exist
     * @throws com.skillmatch.contractservice.exception.InvalidContractOperationException  if the caller is not the
     *         expected party for the current stage, or the contract is not awaiting a signature
     */
    ContractResponse signContract(UUID callerId, UUID contractId);

    /**
     * Marks an ACTIVE contract as COMPLETED. Only the owning company may complete it.
     *
     * @throws com.skillmatch.contractservice.exception.ContractNotFoundException          if contract does not exist
     * @throws com.skillmatch.contractservice.exception.InvalidContractOperationException  if the caller is not the
     *         owning company, or the contract is not ACTIVE
     */
    ContractResponse completeContract(UUID companyId, UUID contractId);
}
