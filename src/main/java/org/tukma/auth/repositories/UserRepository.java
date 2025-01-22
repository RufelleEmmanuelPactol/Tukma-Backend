package org.tukma.auth.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.tukma.auth.models.UserEntity;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity, Long> {

    Optional<UserEntity> findByUsername(String username);
}
