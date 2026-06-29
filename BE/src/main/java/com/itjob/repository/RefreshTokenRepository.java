package com.itjob.repository;

import com.itjob.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
    Optional<RefreshToken> findByToken(UUID token);
    
    List<RefreshToken> findAllByUsernameAndRevokedFalse(String username);
    
    void deleteByUsername(String username);
}
