package com.skillmatch.userservice.mapper;

import com.skillmatch.userservice.dto.response.PortfolioItemResponse;
import com.skillmatch.userservice.model.PortfolioItem;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PortfolioItemMapper {

    PortfolioItemResponse toResponse(PortfolioItem item);
}
