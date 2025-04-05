package com.biletal.biletalbackend.repository;

import com.biletal.biletalbackend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    
    // Find active (non-deleted) user by email
    Optional<User> findByEmailAndIsDeletedFalse(String email);
    
    // Find active (non-deleted) user by id
    Optional<User> findByIdAndIsDeletedFalse(Long id);
}
