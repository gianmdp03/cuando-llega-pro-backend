package com.gianmdp03.cuando_llega_pro.domain.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.presets WHERE u.id = :id")
    Optional<User> findByIdWithPresets(@Param("id") Long id);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.presets WHERE u.email = :email")
    Optional<User> findByEmailWithPresets(@Param("email") String email);
}
