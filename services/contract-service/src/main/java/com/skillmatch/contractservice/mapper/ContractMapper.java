package com.skillmatch.contractservice.mapper;

import com.skillmatch.contractservice.dto.response.ContractResponse;
import com.skillmatch.contractservice.model.Contract;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ContractMapper {

    ContractResponse toResponse(Contract contract);
}
