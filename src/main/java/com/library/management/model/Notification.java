package com.library.management.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity @Table(name="notifications")
public class Notification {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional=false) private User recipient;
    @Column(nullable=false,length=500) private String message;
    private LocalDateTime createdAt=LocalDateTime.now();
    private boolean readFlag;
    public Notification() {
    }
    public Notification(User u,String m) {
        recipient=u;
        message=m;
    }
    public Long getId() {
        return id;
    }
    public User getRecipient() {
        return recipient;
    }
    public String getMessage() {
        return message;
    }
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    public boolean isReadFlag() {
        return readFlag;
    }
    public void setReadFlag(boolean v) {
        readFlag=v;
    }
}
