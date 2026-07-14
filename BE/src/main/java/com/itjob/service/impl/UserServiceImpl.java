package com.itjob.service.impl;

import com.itjob.dto.request.UserUpdateRequest;
import com.itjob.dto.response.PageResponse;
import com.itjob.dto.response.UserResponse;
import com.itjob.entity.User;
import com.itjob.exception.AppException;
import com.itjob.exception.ErrorCode;
import com.itjob.mapper.UserMapper;
import com.itjob.repository.RoleRepository;
import com.itjob.repository.SkillRepository;
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
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class UserServiceImpl implements UserService {
    UserRepository userRepository;
    RoleRepository roleRepository;
    SkillRepository skillRepository;
    SpecificationHelper specificationHelper;
    UserMapper userMapper;
    PasswordEncoder passwordEncoder;

    @Override
    @PreAuthorize("hasRole('ADMIN')")
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
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse getUserById(String id) {
        log.info("Getting user by id: {}", id);
        User user = userRepository.findById(UUID.fromString(id))
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        return userMapper.toUserResponse(user);
    }

    @Override
    public UserResponse getMyInfo() {
        String email = getCurrentUserEmail();
        
        log.info("Getting user profile for: {}", email);
        
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        
        return userMapper.toUserResponse(user);
    }
    
    @Override
    @Transactional
    @PostAuthorize("returnObject.email == authentication.name or hasRole('ADMIN')")
    public UserResponse updateUser(String id, UserUpdateRequest request) {
        log.info("Updating user: {}", id);
        User user = userRepository.findById(UUID.fromString(id))
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        
        // Update basic info
        updateUserBasicInfo(user, request);
        
        // Update password if provided
        updatePasswordIfProvided(user, request.getPassword());
        
        // Update skills if provided
        updateUserSkills(user, request.getSkillIds());
        
        // Only ADMIN can update roles
        updateUserRolesIfAdmin(user, request.getRoles());
        
        user = userRepository.save(user);
        
        return userMapper.toUserResponse(user);
    }
    
    @Override
    @Transactional
    public UserResponse updateMyProfile(UserUpdateRequest request) {
        String email = getCurrentUserEmail();
        
        log.info("Updating profile for: {}", email);
        
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        
        // Update basic info
        updateUserBasicInfo(user, request);
        
        // Update password if provided
        updatePasswordIfProvided(user, request.getPassword());
        
        // Update skills if provided
        updateUserSkills(user, request.getSkillIds());
        
        // Users cannot update their own roles (only ADMIN can)
        // Ignore request.getRoles() for security
        
        user = userRepository.save(user);
        
        return userMapper.toUserResponse(user);
    }
    
    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteUser(String id) {
        log.info("Deleting user: {}", id);
        User user = userRepository.findById(UUID.fromString(id))
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        
        userRepository.delete(user);
        
        log.info("User {} deleted by admin", id);
    }
    
    /**
     * Get current authenticated user's email from SecurityContext
     */
    private String getCurrentUserEmail() {
        var context = SecurityContextHolder.getContext();
        return Objects.requireNonNull(context.getAuthentication()).getName();
    }
    
    /**
     * Update user's basic information using mapper
     */
    private void updateUserBasicInfo(User user, UserUpdateRequest request) {
        userMapper.updateUser(user, request);
    }
    
    /**
     * Update user's password if provided (with encoding)
     */
    private void updatePasswordIfProvided(User user, String password) {
        if (password != null && !password.isEmpty()) {
            user.setPassword(passwordEncoder.encode(password));
        }
    }
    
    /**
     * Update user's skills if skill IDs provided
     */
    private void updateUserSkills(User user, java.util.Set<UUID> skillIds) {
        if (skillIds != null) {
            var skills = skillRepository.findAllById(skillIds);
            user.setSkills(new HashSet<>(skills));
        }
    }
    
    /**
     * Update user's roles if caller is ADMIN
     * Regular users cannot update roles (security)
     */
    private void updateUserRolesIfAdmin(User user, java.util.Set<String> roleNames) {
        if (roleNames != null && isCurrentUserAdmin()) {
            var roles = roleRepository.findAllById(roleNames);
            user.setRoles(new HashSet<>(roles));
        }
    }
    
    /**
     * Check if current authenticated user has ADMIN role
     */
    private boolean isCurrentUserAdmin() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        assert authentication != null;
        return authentication.getAuthorities().stream()
                .anyMatch(auth -> Objects.equals(auth.getAuthority(), "ROLE_ADMIN"));
    }
}
