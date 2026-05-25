package com.snor.quotaguard.mapper;

import com.snor.quotaguard.domain.UserQuota;
import com.snor.quotaguard.dto.response.QuotaResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserQuotaMapper {
    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "remainingToday", expression = "java(Math.max(0, quota.getDailyLimit() - quota.getUsedToday()))")
    QuotaResponse toResponse(UserQuota quota);
}
