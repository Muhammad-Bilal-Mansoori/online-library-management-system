package com.library.management.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "search_history")
public class SearchHistory {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false) private User member;
    @Column(nullable = false, length = 120) private String query;
    private LocalDateTime searchedAt = LocalDateTime.now();
    public SearchHistory() {
    }
    public SearchHistory(User member, String query) {
        this.member = member;
        this.query = query;
    }
    public Long getId() {
        return id;
    }
    public User getMember() {
        return member;
    }
    public String getQuery() {
        return query;
    }
    public LocalDateTime getSearchedAt() {
        return searchedAt;
    }
}
