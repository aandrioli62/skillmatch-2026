package com.skillmatch.userservice.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class CompanyProfileResponse {

    private UUID id;
    private UUID userId;
    private String companyName;
    private String vatNumber;
    private String address;
    private String contactPerson;
}
