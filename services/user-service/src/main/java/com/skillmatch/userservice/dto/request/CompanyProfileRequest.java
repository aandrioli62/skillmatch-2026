package com.skillmatch.userservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CompanyProfileRequest {

    @NotBlank(message = "Company name is required")
    @Size(max = 255, message = "Company name must not exceed 255 characters")
    private String companyName;

    @Size(max = 50, message = "VAT number must not exceed 50 characters")
    private String vatNumber;

    @Size(max = 1000, message = "Address must not exceed 1000 characters")
    private String address;

    @Size(max = 200, message = "Contact person must not exceed 200 characters")
    private String contactPerson;
}
