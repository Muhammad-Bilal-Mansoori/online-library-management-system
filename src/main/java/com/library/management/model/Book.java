package com.library.management.model;

import jakarta.persistence.*;

@Entity
@Table(name="books")
public class Book {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false) private String title;
    @Column(nullable=false) private String author;
    @Column(unique=true,nullable=false) private String isbn;
    private String genre;
    @Column(nullable=false,columnDefinition="integer default 1") private int totalCopies=1;
    @Column(nullable=false,columnDefinition="integer default 1") private int availableCopies=1;
    public Book() {
    }
    public Book(String t,String a,String i,String g) {
        title=t;
        author=a;
        isbn=i;
        genre=g;
        totalCopies=1;
        availableCopies=1;
    }
    public Long getId() {
        return id;
    }
    public String getTitle() {
        return title;
    }
    public void setTitle(String v) {
        title=v;
    }
    public String getAuthor() {
        return author;
    }
    public void setAuthor(String v) {
        author=v;
    }
    public String getIsbn() {
        return isbn;
    }
    public void setIsbn(String v) {
        isbn=v;
    }
    public String getGenre() {
        return genre;
    }
    public void setGenre(String v) {
        genre=v;
    }
    public int getTotalCopies() {
        return totalCopies;
    }
    public void setTotalCopies(int v) {
        totalCopies=Math.max(1,v);
        if(availableCopies>totalCopies) availableCopies=totalCopies;
    }
    public int getAvailableCopies() {
        return availableCopies;
    }
    public void setAvailableCopies(int v) {
        availableCopies=Math.max(0,Math.min(v,totalCopies));
    }
    public int getOnLoanCopies() {
        return Math.max(0,totalCopies-availableCopies);
    }
    public boolean isAvailable() {
        return availableCopies>0;
    }
    public void setAvailable(boolean v) {
        availableCopies=v?totalCopies:0;
    }
}
