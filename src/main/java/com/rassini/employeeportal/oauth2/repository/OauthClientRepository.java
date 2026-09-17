package com.rassini.employeeportal.oauth2.repository;

import com.rassini.employeeportal.oauth2.entity.OauthClientEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OauthClientRepository extends JpaRepository<OauthClientEntity, Long> {
    Optional<OauthClientEntity> findByClientId(String clientId);
    Optional<OauthClientEntity> findByApplicationId(Long applicationId);
    boolean existsByClientId(String clientId);
}
