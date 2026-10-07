package com.library.management.repository;

import com.library.management.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByMemberOrderByBorrowDateDesc(User m);
    List<Transaction> findAllByOrderByBorrowDateDesc();
    long countByReturnedFalse();
}
