package com.snor.quotaguard.user.mapper;

import com.snor.quotaguard.domain.User;
import com.snor.quotaguard.user.dto.response.UserResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserResponse toResponse(User user);
}
