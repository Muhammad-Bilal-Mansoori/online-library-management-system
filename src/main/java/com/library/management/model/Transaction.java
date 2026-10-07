package com.library.management.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity @Table(name="transactions")
public class Transaction {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional=false) private Book book;
    @ManyToOne(optional=false) private User member;
    private LocalDate requestDate;
    private LocalDate borrowDate;
    private LocalDate returnDate;
    private LocalDate dueDate;
    private boolean returned;
    @Enumerated(EnumType.STRING) private Status status;
    public enum Status {
        BORROW_REQUESTED,  ISSUED,  RETURN_REQUESTED,  RETURNED,  CANCELLED
    }
    public Transaction() {
    }
    public Transaction(Book b,User m) {
        book=b;
        member=m;
        requestDate=LocalDate.now();
        returned=false;
        status=Status.ISSUED;
        borrowDate=LocalDate.now();
        dueDate=borrowDate.plusDays(14);
    }
    public Long getId() {
        return id;
    }
    public Book getBook() {
        return book;
    }
    public void setBook(Book v) {
        book=v;
    }
    public User getMember() {
        return member;
    }
    public void setMember(User v) {
        member=v;
    }
    public LocalDate getRequestDate() {
        return requestDate;
    }
    public void setRequestDate(LocalDate v) {
        requestDate=v;
    }
    public LocalDate getBorrowDate() {
        return borrowDate;
    }
    public void setBorrowDate(LocalDate v) {
        borrowDate=v;
    }
    public LocalDate getReturnDate() {
        return returnDate;
    }
    public void setReturnDate(LocalDate v) {
        returnDate=v;
    }
    public LocalDate getDueDate() {
        return dueDate;
    }
    public void setDueDate(LocalDate v) {
        dueDate=v;
    }
    public boolean isReturned() {
        return returned || status==Status.RETURNED;
    }
    public void setReturned(boolean v) {
        returned=v;
        if(v) status=Status.RETURNED;
    }
    public Status getStatus() {
        return status==null?(returned?Status.RETURNED:Status.ISSUED):status;
    }
    public void setStatus(Status v) {
        status=v;
        returned=v==Status.RETURNED;
    }
    public boolean isBorrowRequested() {
        return getStatus()==Status.BORROW_REQUESTED;
    }
    public boolean isIssued() {
        return getStatus()==Status.ISSUED;
    }
    public boolean isReturnRequested() {
        return getStatus()==Status.RETURN_REQUESTED;
    }
    public boolean isCancelled() {
        return getStatus()==Status.CANCELLED;
    }
    public long getFineAmount() {
        if(dueDate==null)return 0;
        LocalDate end=returned&&returnDate!=null?returnDate:LocalDate.now();
        long late=Math.max(0,java.time.temporal.ChronoUnit.DAYS.between(dueDate,end));
        return late*5;
    }
    public boolean isOverdue() {
        return !returned && dueDate!=null && dueDate.isBefore(LocalDate.now());
    }
}
