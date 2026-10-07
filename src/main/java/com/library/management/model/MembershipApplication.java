package com.library.management.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name="membership_applications")
public class MembershipApplication {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false) private String name;
    @Column(nullable=false, unique=true) private String email;
    @Column(nullable=false) private String password;
    @Column(nullable=false) private LocalDateTime appliedAt = LocalDateTime.now();
    @Enumerated(EnumType.STRING) @Column(nullable=false) private Status status = Status.PENDING;
    public enum Status {
        PENDING,  APPROVED,  REJECTED
    }
    public MembershipApplication() {
    }
    public Long getId() {
        return id;
    }
    public String getName() {
        return name;
    }
    public void setName(String v) {
        name=v;
    }
    public String getEmail() {
        return email;
    }
    public void setEmail(String v) {
        email=v;
    }
    public String getPassword() {
        return password;
    }
    public void setPassword(String v) {
        password=v;
    }
    public LocalDateTime getAppliedAt() {
        return appliedAt;
    }
    public Status getStatus() {
        return status;
    }
    public void setStatus(Status v) {
        status=v;
    }
}
