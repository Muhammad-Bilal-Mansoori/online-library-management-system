package com.library.management.repository;

import com.library.management.model.MembershipApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface MembershipApplicationRepository extends JpaRepository<MembershipApplication, Long> {
    Optional<MembershipApplication> findByEmail(String email);
    List<MembershipApplication> findAllByOrderByAppliedAtDesc();
}
