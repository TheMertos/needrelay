package com.yagci.needrelay.repository;

import com.yagci.needrelay.domain.Organization;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * Persistence access for organizations.
 */
public interface OrganizationRepository extends JpaRepository<Organization, UUID> {
}
