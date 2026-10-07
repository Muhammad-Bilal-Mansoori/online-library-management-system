package com.library.management.model;

import jakarta.persistence.*;

@Entity @Table(name="users")
public class User {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(unique=true, nullable=false) private String membershipId;
    @Column(nullable=false) private String name;
    @Column(unique=true, nullable=false) private String email;
    @Column(nullable=false) private String password;
    @Enumerated(EnumType.STRING) @Column(nullable=false) private Role role;
    private boolean active=true;
    private boolean dueAlerts=true;
    private boolean newBookAlerts=true;
    public enum Role {
        LIBRARIAN,  MEMBER
    }
    public User() {
    }
    public User(String membershipId,String name,String email,String password,Role role) {
        this.membershipId=membershipId;
        this.name=name;
        this.email=email;
        this.password=password;
        this.role=role;
    }
    public Long getId() {
        return id;
    }
    public String getMembershipId() {
        return membershipId;
    }
    public void setMembershipId(String v) {
        membershipId=v;
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
    public Role getRole() {
        return role;
    }
    public void setRole(Role v) {
        role=v;
    }
    public boolean isActive() {
        return active;
    }
    public void setActive(boolean v) {
        active=v;
    }
    public boolean isDueAlerts() {
        return dueAlerts;
    }
    public void setDueAlerts(boolean v) {
        dueAlerts=v;
    }
    public boolean isNewBookAlerts() {
        return newBookAlerts;
    }
    public void setNewBookAlerts(boolean v) {
        newBookAlerts=v;
    }
}
