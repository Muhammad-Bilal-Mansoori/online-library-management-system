package com.library.management.service;

import com.library.management.model.*;
import com.library.management.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {
    private final UserRepository users;
    private final BookRepository books;
    private final TransactionRepository tx;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    public DataInitializer(UserRepository u, BookRepository b, TransactionRepository t) {
        users=u;
        books=b;
        tx=t;
    }
    public void run(String... args) {
        if(users.count()==0) {
            users.save(new User("LIB001","Aarav Sharma","librarian@library.com",encoder.encode("admin123"),User.Role.LIBRARIAN));
            users.save(new User("MEM001","Muhammad Bilal","member@library.com",encoder.encode("member123"),User.Role.MEMBER));
            users.save(new User("MEM002","Aisha Khan","aisha@library.com",encoder.encode("member123"),User.Role.MEMBER));
        }
        if(books.count()==0) {
            books.save(copy(new Book("Clean Code","Robert C. Martin","9780132350884","Programming"),3));
            books.save(copy(new Book("Java: The Complete Reference","Herbert Schildt","9781260440232","Programming"),2));
            books.save(copy(new Book("Database System Concepts","Silberschatz","9780078022159","Database"),2));
            books.save(copy(new Book("The Alchemist","Paulo Coelho","9780062315007","Fiction"),1));
            books.save(copy(new Book("Atomic Habits","James Clear","9780735211292","Self Help"),1));
            books.save(copy(new Book("Computer Networks","Andrew S. Tanenbaum","9780132126953","Networking"),1));
        }
        // Keep legacy databases consistent after adding copy-count fields.
        books.findAll().forEach(b -> {
            if (b.getTotalCopies() < 1) b.setTotalCopies(1);
            long onLoan = tx.findAllByOrderByBorrowDateDesc().stream()
            .filter(t -> t.getBook().getId().equals(b.getId()) && (t.isIssued() || t.isReturnRequested()))
            .count();
            b.setAvailableCopies(Math.max(0, b.getTotalCopies() - (int) onLoan));
            books.save(b);
        }
        );
    }
    private Book copy(Book b, int count) {
        b.setTotalCopies(count);
        b.setAvailableCopies(count);
        return b;
    }
}
