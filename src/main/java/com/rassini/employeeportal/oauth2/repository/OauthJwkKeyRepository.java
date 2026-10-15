package com.rassini.employeeportal.oauth2.repository;

import com.rassini.employeeportal.oauth2.entity.OauthJwkKeyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OauthJwkKeyRepository extends JpaRepository<OauthJwkKeyEntity, Long> {
    Optional<OauthJwkKeyEntity> findFirstByStatusOrderByCreatedAtDesc(OauthJwkKeyEntity.KeyStatus status);
    List<OauthJwkKeyEntity> findByStatusIn(List<OauthJwkKeyEntity.KeyStatus> statuses);
    Optional<OauthJwkKeyEntity> findByKeyId(String keyId);
}
