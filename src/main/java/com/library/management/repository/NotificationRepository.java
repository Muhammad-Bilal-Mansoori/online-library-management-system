package com.library.management.repository;

import com.library.management.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByRecipientOrderByCreatedAtDesc(User u);
    long countByRecipientAndReadFlagFalse(User u);
}
