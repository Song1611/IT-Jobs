package com.itjob.service.impl;

import com.itjob.dto.response.PageResponse;
import com.itjob.dto.response.UserResponse;
import com.itjob.entity.User;
import com.itjob.mapper.UserMapper;
import com.itjob.repository.UserRepository;
import com.itjob.service.UserService;
import com.itjob.specification.helper.SpecificationHelper;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserServiceImpl implements UserService {
    UserRepository userRepository;
    SpecificationHelper specificationHelper;
    UserMapper userMapper;

    @Override
    public PageResponse<UserResponse> getUsers(String[] filters, Pageable pageable) {

        Specification<User> spec = specificationHelper.buildSpecification(filters);

        Page<User> usersPage = userRepository.findAll(spec,pageable);

        return PageResponse.<UserResponse>builder()
                .items(usersPage.map(userMapper::toUserResponse).getContent())
                .page(usersPage.getNumber())
                .size(usersPage.getSize())
                .totalElements(usersPage.getTotalElements())
                .totalPages(usersPage.getTotalPages())
                .build();
    }

    @Override
    public UserResponse getUserById(String id) {
        return null;
    }
}
