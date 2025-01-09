package org.demo.huyminh.Mapper;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.demo.huyminh.DTO.Reponse.AdopterResponse;
import org.demo.huyminh.DTO.Reponse.UserResponse;
import org.demo.huyminh.DTO.Request.UserCreationRequest;
import org.demo.huyminh.DTO.Request.UserUpdateRequest;
import org.demo.huyminh.Entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

/**
 * @author Minh
 * Date: 9/24/2024
 * Time: 10:27 AM
 */

@Mapper(componentModel = "Spring")
public interface UserMapper {
    User toUser(UserCreationRequest request);
    UserResponse toUserResponse(User user);

    @Mapping(target = "roles", ignore = true)
    UserResponse toUserResponseForTask(User user);

    @Mapping(target = "address", ignore = true)
    @Mapping(target = "phone", ignore = true)
    @Mapping(target = "gender", ignore = true)
    AdopterResponse toAdopterResponseForTask(User user);

    @Mapping(target = "roles", ignore = true)
    void updateUser(@MappingTarget User user, UserUpdateRequest request);
}
