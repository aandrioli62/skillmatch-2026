package com.skillmatch.contractservice.service;

import com.skillmatch.contractservice.dto.response.ContractResponse;
import com.skillmatch.contractservice.event.CandidatureAcceptedEvent;
import com.skillmatch.contractservice.exception.ContractNotFoundException;
import com.skillmatch.contractservice.exception.InvalidContractOperationException;
import com.skillmatch.contractservice.mapper.ContractMapper;
import com.skillmatch.contractservice.model.Contract;
import com.skillmatch.contractservice.model.enums.ContractStatus;
import com.skillmatch.contractservice.repository.ContractRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ContractServiceImpl implements ContractService {

    // Platform default commission rate (CLAUDE.md business rule #2). The admin-configurable
    // rate actually applied to a payment lives in Payment Service's own commission_config;
    // this is the rate recorded on the contract at drafting time.
    private static final BigDecimal DEFAULT_COMMISSION_RATE = new BigDecimal("8.00");

    private final ContractRepository contractRepository;
    private final ContractMapper contractMapper;

    // =========================================================================
    // Creation (event-driven)
    // =========================================================================

    @Override
    public void createFromCandidatureAccepted(CandidatureAcceptedEvent.Data data) {
        if (contractRepository.existsByProjectId(data.getProjectId())) {
            log.warn("Contract already exists for projectId={}, skipping duplicate candidature.accepted event",
                    data.getProjectId());
            return;
        }
        if (data.getAmount() == null) {
            log.error("candidature.accepted event for projectId={} carries no amount; skipping contract creation",
                    data.getProjectId());
            return;
        }

        Contract contract = new Contract();
        contract.setProjectId(data.getProjectId());
        contract.setProfessionalId(data.getProfessionalId());
        contract.setCompanyId(data.getCompanyId());
        contract.setAmount(data.getAmount());
        contract.setCommissionRate(DEFAULT_COMMISSION_RATE);
        contract.setStatus(ContractStatus.DRAFT);
        contractRepository.save(contract);

        log.info("Contract created in DRAFT: projectId={}, companyId={}, professionalId={}, amount={}",
                data.getProjectId(), data.getCompanyId(), data.getProfessionalId(), data.getAmount());
    }

    // =========================================================================
    // Retrieval
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public ContractResponse getContract(UUID contractId) {
        return contractMapper.toResponse(findContractById(contractId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContractResponse> listContractsByCompany(UUID companyId) {
        return contractRepository.findByCompanyId(companyId).stream()
                .map(contractMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContractResponse> listContractsByProfessional(UUID professionalId) {
        return contractRepository.findByProfessionalId(professionalId).stream()
                .map(contractMapper::toResponse)
                .collect(Collectors.toList());
    }

    // =========================================================================
    // Signature & completion
    // =========================================================================

    @Override
    public ContractResponse signContract(UUID callerId, UUID contractId) {
        Contract contract = findContractById(contractId);

        switch (contract.getStatus()) {
            case DRAFT -> {
                if (!contract.getCompanyId().equals(callerId)) {
                    throw new InvalidContractOperationException(
                            "Contract with id=" + contractId + " is awaiting the company's signature");
                }
                contract.setStatus(ContractStatus.PENDING_SIGNATURES);
            }
            case PENDING_SIGNATURES -> {
                if (!contract.getProfessionalId().equals(callerId)) {
                    throw new InvalidContractOperationException(
                            "Contract with id=" + contractId + " is awaiting the professional's signature");
                }
                contract.setStatus(ContractStatus.ACTIVE);
                contract.setSignedAt(LocalDateTime.now());
            }
            default -> throw new InvalidContractOperationException(
                    "Contract with id=" + contractId + " cannot be signed from status=" + contract.getStatus());
        }

        contract = contractRepository.save(contract);
        log.info("Contract signed: contractId={}, callerId={}, newStatus={}", contractId, callerId, contract.getStatus());
        return contractMapper.toResponse(contract);
    }

    @Override
    public ContractResponse completeContract(UUID companyId, UUID contractId) {
        Contract contract = findContractById(contractId);

        if (!contract.getCompanyId().equals(companyId)) {
            throw new InvalidContractOperationException(
                    "Company with id=" + companyId + " is not the owner of contract id=" + contractId);
        }
        if (contract.getStatus() != ContractStatus.ACTIVE) {
            throw new InvalidContractOperationException(
                    "Contract with id=" + contractId + " cannot be completed from status=" + contract.getStatus());
        }

        contract.setStatus(ContractStatus.COMPLETED);
        contract = contractRepository.save(contract);

        log.info("Contract completed: contractId={}", contractId);
        return contractMapper.toResponse(contract);
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    private Contract findContractById(UUID contractId) {
        return contractRepository.findById(contractId)
                .orElseThrow(() -> new ContractNotFoundException(contractId));
    }
}
