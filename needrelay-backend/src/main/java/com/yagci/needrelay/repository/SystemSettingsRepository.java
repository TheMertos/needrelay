package com.yagci.needrelay.repository;

import com.yagci.needrelay.domain.SystemSettings;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Persistence for the singleton {@link SystemSettings} row.
 */
public interface SystemSettingsRepository extends JpaRepository<SystemSettings, Integer> {
}
