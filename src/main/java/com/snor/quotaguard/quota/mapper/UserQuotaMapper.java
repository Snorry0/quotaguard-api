package com.snor.quotaguard.quota.mapper;

import com.snor.quotaguard.domain.UserQuota;
import com.snor.quotaguard.quota.dto.response.QuotaResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserQuotaMapper {
    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "remainingToday", expression = "java(quota.remainingToday())")
    QuotaResponse toResponse(UserQuota quota);
}
