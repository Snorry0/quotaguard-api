package com.snor.quotaguard.mapper;

import com.snor.quotaguard.domain.UsageRecord;
import com.snor.quotaguard.dto.response.UsageRecordResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UsageRecordMapper {
    @Mapping(target = "userId", source = "user.id")
    UsageRecordResponse toResponse(UsageRecord usageRecord);
}
