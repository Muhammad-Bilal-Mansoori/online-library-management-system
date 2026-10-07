package com.library.management.concurrency;

import com.library.management.model.Book;
import com.library.management.model.Transaction;
import com.library.management.repository.BookRepository;
import com.library.management.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import jakarta.annotation.PreDestroy;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
* Demonstrates Java multithreading by calculating independent report values
* concurrently. Each task performs a read-only calculation, so no shared
* mutable state is modified.
*/
@Service
public class ConcurrentReportService {
    private final BookRepository books;
    private final TransactionRepository transactions;
    private final ExecutorService executor = Executors.newFixedThreadPool(3);
    public ConcurrentReportService(BookRepository books, TransactionRepository transactions) {
        this.books = books;
        this.transactions = transactions;
    }
    public ReportSnapshot buildSnapshot() {
        CompletableFuture<Integer> totalBooks = CompletableFuture.supplyAsync(
        () -> books.findAll().stream().mapToInt(Book::getTotalCopies).sum(), executor);
        CompletableFuture<Integer> availableBooks = CompletableFuture.supplyAsync(
        () -> books.findAll().stream().mapToInt(Book::getAvailableCopies).sum(), executor);
        CompletableFuture<List<Transaction>> allTransactions = CompletableFuture.supplyAsync(
        transactions::findAllByOrderByBorrowDateDesc, executor);
        return new ReportSnapshot(
        totalBooks.join(),
        availableBooks.join(),
        allTransactions.join()
        );
    }
    @PreDestroy
    public void shutdown() {
        executor.shutdown();
    }
    public synchronized long calculateOverdue(List<Transaction> transactions) {
        return transactions.stream().filter(Transaction::isOverdue).count();
    }
    public synchronized long calculateBorrowed(List<Transaction> transactions) {
        return transactions.stream()
        .filter(t -> t.isIssued() || t.isReturnRequested())
        .count();
    }
    public record ReportSnapshot(int totalBooks, int availableBooks, List<Transaction> transactions) {
    }
}
