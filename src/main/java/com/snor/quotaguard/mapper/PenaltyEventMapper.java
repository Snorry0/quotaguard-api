package com.snor.quotaguard.mapper;

import com.snor.quotaguard.domain.PenaltyEvent;
import com.snor.quotaguard.dto.response.PenaltyEventResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PenaltyEventMapper {
    @Mapping(target = "userId", source = "user.id")
    PenaltyEventResponse toResponse(PenaltyEvent penaltyEvent);
}
