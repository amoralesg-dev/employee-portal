package com.rassini.employeeportal.oauth2.repository;

import com.rassini.employeeportal.oauth2.entity.OauthClientRedirectUriEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OauthClientRedirectUriRepository extends JpaRepository<OauthClientRedirectUriEntity, Long> {
    List<OauthClientRedirectUriEntity> findByOauthClientId(Long oauthClientId);
    void deleteByOauthClientId(Long oauthClientId);
}
