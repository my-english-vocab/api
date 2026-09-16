package com.myenglishvocab.server.user.repository;

import com.myenglishvocab.server.user.entity.User;
import com.myenglishvocab.server.user.entity.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM User u WHERE u.id = :id")
    Optional<User> findByIdForOnboarding(Long id);

    Optional<User> findByUsername(String username);
    Optional<User> findByIdAndStatus(Long id, UserStatus status);
    long countByStatus(UserStatus status);
    long countByCreatedAtIsNullAndStatus(UserStatus status);
    List<User> findAllByOrderByLastActiveAtDesc(Pageable pageable);
}
