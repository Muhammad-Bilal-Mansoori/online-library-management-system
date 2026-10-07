package com.library.management.repository;

import com.library.management.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByMembershipId(String id);
    long countByRole(User.Role role);
    long countByRoleAndActiveTrue(User.Role role);
}
