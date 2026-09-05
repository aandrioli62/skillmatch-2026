package com.skillmatch.userservice.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class PortfolioItemResponse {

    private UUID id;
    private String title;
    private String description;
    private String url;
}
