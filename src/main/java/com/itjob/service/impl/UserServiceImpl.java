package com.itjob.service.impl;

import com.itjob.dto.response.PageResponse;
import com.itjob.dto.response.UserResponse;
import com.itjob.entity.User;
import com.itjob.exception.AppException;
import com.itjob.exception.ErrorCode;
import com.itjob.mapper.UserMapper;
import com.itjob.repository.UserRepository;
import com.itjob.service.UserService;
import com.itjob.specification.helper.SpecificationHelper;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.Arrays;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class UserServiceImpl implements UserService {
    UserRepository userRepository;
    SpecificationHelper specificationHelper;
    UserMapper userMapper;

    @Override
    public PageResponse<UserResponse> getUsers(String[] filters, Pageable pageable) {

        log.info("=== SERVICE LAYER DEBUG ===");
        log.info("Filters received: {}", filters != null ? Arrays.toString(filters) : "null");
        
        Specification<User> spec = specificationHelper.buildSpecification(filters);
        
        log.info("Specification built: {}", spec != null ? "NOT NULL" : "NULL");
        log.info("=== SERVICE LAYER DEBUG END ===");

        Page<User> usersPage = userRepository.findAll(spec,pageable);

        if(usersPage.isEmpty() || usersPage.getTotalElements() == 0){
            throw new AppException(ErrorCode.USER_NOT_FOUND);
        }

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
