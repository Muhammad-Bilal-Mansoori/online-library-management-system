package com.library.management.repository;

import com.library.management.model.SearchHistory;
import com.library.management.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SearchHistoryRepository extends JpaRepository<SearchHistory,  Long> {
    List<SearchHistory> findTop8ByMemberOrderBySearchedAtDesc(User member);
}
