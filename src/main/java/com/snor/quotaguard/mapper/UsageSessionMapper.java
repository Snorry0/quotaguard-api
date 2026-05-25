package com.snor.quotaguard.mapper;

import com.snor.quotaguard.domain.UsageSession;
import com.snor.quotaguard.dto.response.UsageSessionResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UsageSessionMapper {

    @Mapping(target = "userId", source = "user.id")
    UsageSessionResponse toResponse(UsageSession session);
}