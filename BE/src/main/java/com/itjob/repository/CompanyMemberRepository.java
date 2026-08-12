package com.itjob.repository;

import com.itjob.entity.CompanyMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface CompanyMemberRepository extends JpaRepository<CompanyMember, CompanyMember.CompanyMemberId> {

    boolean existsByCompanyIdAndUserIdAndStatus(UUID companyId, UUID userId, String status);
}
