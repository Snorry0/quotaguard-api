package com.snor.quotaguard.mapper;

import com.snor.quotaguard.domain.User;
import com.snor.quotaguard.dto.response.UserResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserResponse toResponse(User user);
}
