package com.snor.quotaguard.usage.mapper;

import com.snor.quotaguard.domain.UsageRecord;
import com.snor.quotaguard.usage.dto.response.UsageRecordResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UsageRecordMapper {
    @Mapping(target = "userId", source = "user.id")
    UsageRecordResponse toResponse(UsageRecord usageRecord);
}
