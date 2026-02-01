package com.itjob.itjob.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Table(name = "skills")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
public class Skill {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String id;
    
    String name;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    
    @ManyToMany(mappedBy = "skills")
    Set<User> users;
    
    @ManyToMany(mappedBy = "skills")
    Set<Job> jobs;
    
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
