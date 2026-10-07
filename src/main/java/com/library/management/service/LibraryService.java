package com.library.management.service;

import com.library.management.model.*;
import com.library.management.repository.*;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class LibraryService {
    public static final int MAX_ACTIVE_LOANS = 3;
    public static final int LOAN_DAYS = 14;
    public static final int DAILY_FINE = 5;
    private final BookRepository books;
    private final UserRepository users;
    private final TransactionRepository tx;
    private final NotificationRepository notes;
    private final SearchHistoryRepository searches;
    public LibraryService(BookRepository b, UserRepository u, TransactionRepository t, NotificationRepository n, SearchHistoryRepository s) {
        books=b;
        users=u;
        tx=t;
        notes=n;
        searches=s;
    }
    public List<Book> search(String q) {
        if(q==null || q.isBlank()) return books.findAll();
        return books.findByTitleContainingIgnoreCaseOrAuthorContainingIgnoreCaseOrGenreContainingIgnoreCase(q,q,q);
    }
    public void recordSearch(User member, String q) {
        if(member != null && member.getRole()==User.Role.MEMBER && q!=null && !q.isBlank()) searches.save(new SearchHistory(member,q.trim()));
    }
    public Transaction requestBorrow(Long bookId, User member) {
        if (member.getRole()!=User.Role.MEMBER || !member.isActive()) throw new IllegalStateException("Only active members can request books.");
        if (tx.findByMemberOrderByBorrowDateDesc(member).stream().filter(t -> !t.isReturned() && !t.isBorrowRequested()).count() >= MAX_ACTIVE_LOANS)
        throw new IllegalStateException("Loan limit reached. You can have up to 3 active books.");
        Book b=books.findById(bookId).orElseThrow(() -> new IllegalStateException("Book not found."));
        if(!b.isAvailable()) throw new IllegalStateException("Book is currently unavailable.");
        boolean alreadyPending=tx.findByMemberOrderByBorrowDateDesc(member).stream().anyMatch(t -> t.getBook().getId().equals(bookId) && t.isBorrowRequested());
        if(alreadyPending) throw new IllegalStateException("A borrow request for this book is already pending.");
        Transaction t=new Transaction(b,member);
        t.setStatus(Transaction.Status.BORROW_REQUESTED);
        t.setBorrowDate(null);
        t.setDueDate(null);
        Transaction saved=tx.save(t);
        notes.save(new Notification(member,"Borrow request submitted for: " + b.getTitle() + ". Please collect the book from the librarian after approval."));
        users.findAll().stream()
                .filter(u -> u.getRole() == User.Role.LIBRARIAN && u.isActive())
                .forEach(l -> notes.save(new Notification(
                        l,
                        "New borrow request: " + member.getName()
                                + " requested “" + b.getTitle() + "”")));
        return saved;
    }
    public Transaction issue(Long bookId, User member) {
        if (member.getRole()!=User.Role.MEMBER || !member.isActive()) throw new IllegalStateException("Member account is not active.");
        if (tx.findByMemberOrderByBorrowDateDesc(member).stream().filter(t -> !t.isReturned() && !t.isBorrowRequested()).count() >= MAX_ACTIVE_LOANS)
        throw new IllegalStateException("Loan limit reached. You can have up to 3 active books.");
        Book b=books.findById(bookId).orElseThrow(() -> new IllegalStateException("Book not found."));
        if(!b.isAvailable()) throw new IllegalStateException("Book is currently unavailable.");
        b.setAvailableCopies(b.getAvailableCopies()-1);
        books.save(b);
        Transaction t=new Transaction(b,member);
        t.setStatus(Transaction.Status.ISSUED);
        t.setBorrowDate(LocalDate.now());
        t.setDueDate(LocalDate.now().plusDays(LOAN_DAYS));
        Transaction saved=tx.save(t);
        notes.save(new Notification(member,"Book issued: " + b.getTitle() + ". Due on " + saved.getDueDate() + "."));
        return saved;
    }
    public Transaction approveBorrow(Long txId) {
        Transaction t=tx.findById(txId).orElseThrow(() -> new IllegalStateException("Transaction not found."));
        if(!t.isBorrowRequested()) throw new IllegalStateException("This borrow request is not pending.");
        if(!t.getBook().isAvailable()) throw new IllegalStateException("Book is no longer available.");
        if (tx.findByMemberOrderByBorrowDateDesc(t.getMember()).stream()
                .filter(x -> !x.isReturned() && !x.isBorrowRequested())
                .count() >= MAX_ACTIVE_LOANS) {
            throw new IllegalStateException("Member has reached the active loan limit.");
        }
        t.getBook().setAvailableCopies(t.getBook().getAvailableCopies()-1);
        books.save(t.getBook());
        t.setStatus(Transaction.Status.ISSUED);
        t.setBorrowDate(LocalDate.now());
        t.setDueDate(LocalDate.now().plusDays(LOAN_DAYS));
        Transaction saved=tx.save(t);
        notes.save(new Notification(t.getMember(),"Borrow request approved. " + t.getBook().getTitle() + " has been issued to your account. Due on " + saved.getDueDate() + "."));
        return saved;
    }
    public void rejectBorrow(Long txId) {
        Transaction t=tx.findById(txId).orElseThrow(() -> new IllegalStateException("Transaction not found."));
        if(!t.isBorrowRequested()) throw new IllegalStateException("This request is not pending.");
        t.setStatus(Transaction.Status.CANCELLED);
        tx.save(t);
        notes.save(new Notification(t.getMember(),"Borrow request declined for: " + t.getBook().getTitle() + ". The book remains available in the catalog."));
    }
    public void requestReturn(Long txId, User member) {
        Transaction t=tx.findById(txId).orElseThrow(() -> new IllegalStateException("Transaction not found."));
        if(!t.getMember().getId().equals(member.getId())) throw new IllegalStateException("You can only return your own borrowed books.");
        if(!t.isIssued()) throw new IllegalStateException("This book is not currently eligible for return.");
        t.setStatus(Transaction.Status.RETURN_REQUESTED);
        tx.save(t);
        notes.save(new Notification(member,"Return request submitted for: " + t.getBook().getTitle() + ". Please hand the physical book to the librarian for verification."));
    }
    public void completeReturn(Transaction t) {
        if(t.isReturned()) throw new IllegalStateException("Book is already returned.");
        if(!t.isIssued() && !t.isReturnRequested()) throw new IllegalStateException("The book must be issued before it can be returned.");
        t.setReturned(true);
        t.setReturnDate(LocalDate.now());
        t.setStatus(Transaction.Status.RETURNED);
        t.getBook().setAvailableCopies(Math.min(t.getBook().getTotalCopies(), t.getBook().getAvailableCopies()+1));
        books.save(t.getBook());
        tx.save(t);
    }
    public long calculateFine(Transaction t) {
        if(t.getDueDate()==null) return 0;
        LocalDate end = t.isReturned() && t.getReturnDate()!=null ? t.getReturnDate() : LocalDate.now();
        long lateDays = Math.max(0, ChronoUnit.DAYS.between(t.getDueDate(), end));
        return lateDays * DAILY_FINE;
    }
    public void notifyAllMembers(String message, boolean newBookOnly) {
        users.findAll().stream().filter(u -> u.getRole()==User.Role.MEMBER && u.isActive())
        .filter(u -> !newBookOnly || u.isNewBookAlerts())
        .forEach(u -> notes.save(new Notification(u,message)));
    }
    public void notifyMember(User member, String message) {
        notes.save(new Notification(member,message));
    }
    public void sendDueAlerts() {
        LocalDate today=LocalDate.now();
        tx.findAllByOrderByBorrowDateDesc().stream().filter(t -> !t.isReturned() && t.getDueDate()!=null && (t.isIssued() || t.isReturnRequested())).forEach(t -> {
            User m=t.getMember();
            if(!m.isActive() || !m.isDueAlerts()) return;
            long days=ChronoUnit.DAYS.between(today,t.getDueDate());
            String marker = days < 0 ? "OVERDUE:" : "DUE:";
            String message = days < 0
            ? "Overdue alert: " + t.getBook().getTitle() + " is " + Math.abs(days) + " day(s) overdue. Current fine: ₹" + calculateFine(t) + "."
            : days == 0 ? "Due today: " + t.getBook().getTitle() + ". Please return it today."
            : days <= 2 ? "Due soon: " + t.getBook().getTitle() + " is due in " + days + " day(s)." : null;
            if (message != null
                    && notes.findByRecipientOrderByCreatedAtDesc(m).stream().noneMatch(n ->
                    n.getMessage().startsWith(marker)
                            && n.getMessage().contains(t.getBook().getTitle())
                            && n.getCreatedAt().toLocalDate().equals(today)))
            notes.save(new Notification(m, marker + " " + message));
        }
        );
    }
}
