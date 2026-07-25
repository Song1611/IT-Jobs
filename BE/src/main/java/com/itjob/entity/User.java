package com.itjob.entity;

import com.itjob.enums.Gender;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;
    
    @Column(name = "full_name", nullable = false, length = 100)
    String fullName;
    
    @Column(nullable = false, unique = true, length = 100)
    String email;
    
    @Column(nullable = false)
    String password;
    
    @Column(length = 15)
    String phone;
    
    @Column(length = 10)
    @Enumerated(EnumType.STRING)
    Gender gender;
    
    @Column(name = "date_of_birth")
    LocalDate dateOfBirth;
    
    String avatar;
    
    @Column(name = "cover_image")
    String coverImage;
    
    @Column(name = "cv_url")
    String cvUrl;
    
    @Column(columnDefinition = "TEXT")
    String address;
    
    @Column(name = "created_at", nullable = false)
    LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    LocalDateTime updatedAt;

    @Builder.Default
    @Column(nullable = false)
    boolean enabled = true;
    
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "user_roles",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "role_name")
    )
    Set<Role> roles;
    
    @ManyToMany
    @JoinTable(
        name = "skill_users",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "skill_id")
    )
    Set<Skill> skills;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
