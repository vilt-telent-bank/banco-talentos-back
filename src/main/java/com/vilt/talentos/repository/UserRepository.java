package com.vilt.talentos.repository;

import com.vilt.talentos.entity.DomainStatus;
import com.vilt.talentos.entity.User;
import com.vilt.talentos.entity.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    @EntityGraph(attributePaths = {"group"})
    Optional<User> findByEmailIgnoreCase(String email);

    @EntityGraph(attributePaths = {"group"})
    Optional<User> findByResetToken(String resetToken);

    @EntityGraph(attributePaths = {"group"})
    Page<User> findAllByRoleAndStatus(UserRole role, DomainStatus status, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"group"})
    Page<User> findAll(Pageable pageable);

    Optional<User> findByRefreshToken(String refreshToken);
}
