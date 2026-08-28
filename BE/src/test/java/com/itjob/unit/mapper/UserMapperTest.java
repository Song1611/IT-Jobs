package com.itjob.unit.mapper;

import com.itjob.dto.request.UserUpdateRequest;
import com.itjob.dto.response.UserResponse;
import com.itjob.entity.User;
import com.itjob.mapper.UserMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Unit - UserMapper")
class UserMapperTest {

    private final UserMapper mapper = Mappers.getMapper(UserMapper.class);

    @Test
    @DisplayName("toUserResponse -> maps user fields")
    void toUserResponseMapsFields() {
        User user = User.builder().id(UUID.randomUUID()).email("u@example.com").fullName("John").build();

        UserResponse response = mapper.toUserResponse(user);

        assertThat(response.getEmail()).isEqualTo("u@example.com");
        assertThat(response.getFullName()).isEqualTo("John");
    }

    @Test
    @DisplayName("updateUser -> updates name but ignores email and password")
    void updateUserIgnoresEmailAndPassword() {
        User user = User.builder().email("old@example.com").fullName("Old").password("old-pw").build();
        UserUpdateRequest request = new UserUpdateRequest();
        request.setFullName("New Name");

        mapper.updateUser(user, request);

        assertThat(user.getFullName()).isEqualTo("New Name");
        assertThat(user.getEmail()).isEqualTo("old@example.com");
        assertThat(user.getPassword()).isEqualTo("old-pw");
    }
}