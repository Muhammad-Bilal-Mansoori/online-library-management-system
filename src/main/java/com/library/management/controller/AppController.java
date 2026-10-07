package com.library.management.controller;

import com.library.management.model.*;
import com.library.management.jdbc.JdbcReportRepository;
import com.library.management.jdbc.JdbcConnectionDemo;
import com.library.management.concurrency.ConcurrentReportService;
import com.library.management.repository.*;
import com.library.management.service.LibraryService;
import com.library.management.service.PasswordService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Controller
public class AppController {
    private final UserRepository users;
    private final BookRepository books;
    private final TransactionRepository tx;
    private final NotificationRepository notes;
    private final SearchHistoryRepository searches;
    private final MembershipApplicationRepository applications;
    private final LibraryService service;
    private final PasswordService passwords;
    private final JdbcReportRepository jdbcReports;
    private final JdbcConnectionDemo jdbcConnectionDemo;
    private final ConcurrentReportService concurrentReports;
    public AppController(UserRepository u, BookRepository b, TransactionRepository t, NotificationRepository n,
    SearchHistoryRepository sh, MembershipApplicationRepository a, LibraryService s, PasswordService p,
    JdbcReportRepository jr, JdbcConnectionDemo jcd, ConcurrentReportService cr) {
        users=u;
        books=b;
        tx=t;
        notes=n;
        searches=sh;
        applications=a;
        service=s;
        passwords=p;
        jdbcReports=jr;
        jdbcConnectionDemo=jcd;
        concurrentReports=cr;
    }
    @GetMapping("/")
    public String home(Model m) {
        m.addAttribute("books",books.findAll().stream().limit(6).toList());
        m.addAttribute("bookCount",books.findAll().stream().mapToInt(Book::getTotalCopies).sum());
        return "home";
    }
    @GetMapping("/login")
    public String login() {
        return "login";
    }
    @GetMapping("/register")
    public String register(Model m) {
        m.addAttribute("application",new MembershipApplication());
        return "register";
    }
    @PostMapping("/register")
    public String submitRegistration(@ModelAttribute MembershipApplication a) {
        String email=a.getEmail()==null?"":a.getEmail().trim().toLowerCase();
        if (users.findByEmail(email).isPresent()
                || applications.findByEmail(email).isPresent()) {
            return "redirect:/register?error=An+account+or+pending+application+already+exists+for+this+email";
        }
        a.setEmail(email);
        a.setPassword(passwords.encode(a.getPassword()));
        a.setStatus(MembershipApplication.Status.PENDING);
        applications.save(a);
        return "redirect:/login?registered=true";
    }
    @PostMapping("/login")
    public String doLogin(@RequestParam String email,@RequestParam String password,@RequestParam String role,HttpSession session,Model model) {
        return users.findByEmail(email.trim().toLowerCase()).filter(User::isActive)
        .filter(u->u.getRole().name().equals(role))
        .filter(u->passwords.matches(password,u.getPassword())).map(u-> {
            session.setAttribute("userId",u.getId()); session.setAttribute("role",u.getRole().name()); return "redirect:/dashboard";
        }
        ).orElseGet(()-> {
            model.addAttribute("error","Invalid credentials or selected role does not match the account.");return "login";
        }
        );
    }
    @GetMapping("/logout")
    public String logout(HttpSession s) {
        s.invalidate();
        return "redirect:/?logout=true";
    }
    private User current(HttpSession s) {
        Object id=s.getAttribute("userId");
        return id==null?null:users.findById((Long)id).orElse(null);
    }
    private String guard(HttpSession s,String role) {
        User u=current(s);
        if(u==null)return "redirect:/login";
        if(!u.isActive()||!u.getRole().name().equals(role))return "redirect:/dashboard";
        return null;
    }
    @GetMapping("/dashboard")
    public String dashboard(HttpSession s,Model m) {
        User u=current(s);
        if(u==null)return "redirect:/login";
        m.addAttribute("user",u);
        service.sendDueAlerts();
        if(u.getRole()==User.Role.LIBRARIAN) {
            m.addAttribute("bookCount",books.findAll().stream().mapToInt(Book::getTotalCopies).sum());
            m.addAttribute("memberCount",users.countByRoleAndActiveTrue(User.Role.MEMBER));
            m.addAttribute("availableCount",books.findAll().stream().mapToInt(Book::getAvailableCopies).sum());
            m.addAttribute("onLoanCount",tx.findAllByOrderByBorrowDateDesc().stream().filter(t->t.isIssued()||t.isReturnRequested()).count());
            m.addAttribute("activeLoans",m.getAttribute("onLoanCount"));
            m.addAttribute("overdueLoans",tx.findAllByOrderByBorrowDateDesc().stream().filter(Transaction::isOverdue).count());
            m.addAttribute("pendingApplications",applications.findAllByOrderByAppliedAtDesc().stream().filter(a->a.getStatus()==MembershipApplication.Status.PENDING).count());
            m.addAttribute("transactions",tx.findAllByOrderByBorrowDateDesc().stream().limit(8).toList());
            return "librarian-dashboard";
        }
        m.addAttribute("books",books.findAll().stream().limit(6).toList());
        m.addAttribute("loans",tx.findByMemberOrderByBorrowDateDesc(u).stream().limit(6).toList());
        m.addAttribute("notifications",notes.findByRecipientOrderByCreatedAtDesc(u).stream().limit(5).toList());
        m.addAttribute("unread",notes.countByRecipientAndReadFlagFalse(u));
        m.addAttribute("searchHistory",searches.findTop8ByMemberOrderBySearchedAtDesc(u));
        return "member-dashboard";
    }
    @GetMapping("/catalog")
    public String catalog(@RequestParam(required=false) String q,Model m) {
        m.addAttribute("books",service.search(q));
        m.addAttribute("q",q);
        return "catalog";
    }
    @GetMapping("/books")
    public String books(@RequestParam(required=false) String q,HttpSession s,Model m) {
        User u=current(s);
        if(u==null)return "redirect:/login";
        if(q!=null&&!q.isBlank())service.recordSearch(u,q);
        m.addAttribute("user",u);
        m.addAttribute("books",service.search(q));
        m.addAttribute("q",q);
        return "books";
    }
    @GetMapping("/books/add")
    public String addBook(HttpSession s,Model m) {
        String g=guard(s,"LIBRARIAN");
        if(g!=null)return g;
        m.addAttribute("book",new Book());
        return "book-form";
    }
    @GetMapping("/books/edit/{id}")
    public String editBook(@PathVariable Long id,HttpSession s,Model m) {
        String g=guard(s,"LIBRARIAN");
        if(g!=null)return g;
        m.addAttribute("book",books.findById(id).orElseThrow());
        return "book-form";
    }
    @PostMapping("/books/save")
    public String saveBook(@ModelAttribute Book b,HttpSession s) {
        String g=guard(s,"LIBRARIAN");
        if(g!=null)return g;
        boolean isNew=b.getId()==null;
        if(isNew) {
            b.setAvailableCopies(b.getTotalCopies());
        }
        else {
            Book existing=books.findById(b.getId()).orElseThrow();
            long onLoan=tx.findAllByOrderByBorrowDateDesc().stream().filter(t->t.getBook().getId().equals(b.getId()) && (t.isIssued()||t.isReturnRequested())).count();
            if(b.getTotalCopies()<onLoan) return "redirect:/books?error=Total+copies+cannot+be+less+than+copies+currently+on+loan";
            b.setAvailableCopies((int)b.getTotalCopies()-(int)onLoan);
        }
        books.save(b);
        if(isNew)service.notifyAllMembers("New arrival: “"+b.getTitle()+"” by "+b.getAuthor()+" is now available in the library catalog.",true);
        return "redirect:/books?success=Book+saved+successfully";
    }
    @PostMapping("/books/delete/{id}")
    public String deleteBook(@PathVariable Long id,HttpSession s) {
        String g=guard(s,"LIBRARIAN");
        if(g!=null)return g;
        if (tx.findAllByOrderByBorrowDateDesc().stream().anyMatch(t -> t.getBook().getId().equals(id))) {
            return "redirect:/books?error=This+book+has+transaction+history+and+cannot+be+deleted";
        }
        books.deleteById(id);
        return "redirect:/books?success=Book+deleted";
    }
    @GetMapping("/members")
    public String members(HttpSession s,Model m) {
        String g=guard(s,"LIBRARIAN");
        if(g!=null)return g;
        var allMembers=users.findAll().stream().filter(u->u.getRole()==User.Role.MEMBER).toList();
        m.addAttribute("activeMembers",allMembers.stream().filter(User::isActive).toList());
        m.addAttribute("inactiveMembers",allMembers.stream().filter(u->!u.isActive()).toList());
        m.addAttribute("memberLoanCounts", allMembers.stream().collect(
                java.util.stream.Collectors.toMap(
                        User::getId,
                        u -> tx.findByMemberOrderByBorrowDateDesc(u).stream()
                                .filter(t -> t.isIssued() || t.isReturnRequested())
                                .count())));
        m.addAttribute("memberFines", allMembers.stream().collect(
                java.util.stream.Collectors.toMap(
                        User::getId,
                        u -> tx.findByMemberOrderByBorrowDateDesc(u).stream()
                                .mapToLong(service::calculateFine)
                                .sum())));
        return "members";
    }
    @GetMapping("/members/view/{id}")
    public String memberView(@PathVariable Long id,HttpSession s,Model m) {
        String g=guard(s,"LIBRARIAN");
        if(g!=null)return g;
        User member=users.findById(id).orElseThrow();
        var history=tx.findByMemberOrderByBorrowDateDesc(member);
        m.addAttribute("member",member);
        m.addAttribute("loans",history);
        m.addAttribute("currentBooks",history.stream().filter(t->t.isIssued()||t.isReturnRequested()).count());
        m.addAttribute("overdueBooks",history.stream().filter(Transaction::isOverdue).count());
        m.addAttribute("fineTotal",history.stream().mapToLong(service::calculateFine).sum());
        return "member-detail";
    }
    @GetMapping("/members/add")
    public String addMember(HttpSession s,Model m) {
        String g=guard(s,"LIBRARIAN");
        if(g!=null)return g;
        m.addAttribute("member",new User());
        return "member-form";
    }
    @GetMapping("/members/edit/{id}")
    public String editMember(@PathVariable Long id,HttpSession s,Model m) {
        String g=guard(s,"LIBRARIAN");
        if(g!=null)return g;
        m.addAttribute("member",users.findById(id).orElseThrow());
        return "member-form";
    }
    @PostMapping("/members/save")
    public String saveMember(@ModelAttribute User u,HttpSession s) {
        String g=guard(s,"LIBRARIAN");
        if(g!=null)return g;
        User existing=u.getId()==null?null:users.findById(u.getId()).orElse(null);
        u.setRole(User.Role.MEMBER);
        if(u.getPassword()==null||u.getPassword().isBlank())u.setPassword(existing!=null?existing.getPassword():passwords.encode("member123"));
        else if(existing==null||!u.getPassword().equals(existing.getPassword()))u.setPassword(passwords.encode(u.getPassword()));
        if(u.getMembershipId()==null||u.getMembershipId().isBlank())u.setMembershipId("MEM"+UUID.randomUUID().toString().substring(0,6).toUpperCase());
        users.save(u);
        return "redirect:/members?success=Member+saved";
    }
    @PostMapping("/members/delete/{id}")
    public String deleteMember(@PathVariable Long id,HttpSession s) {
        String g=guard(s,"LIBRARIAN");
        if(g!=null)return g;
        return "redirect:/members?error=Member+records+are+preserved.+Use+Deactivate+or+Reactivate+instead.";
    }
    @PostMapping("/members/toggle/{id}")
    public String toggleMember(@PathVariable Long id,HttpSession s) {
        String g=guard(s,"LIBRARIAN");
        if(g!=null)return g;
        User member=users.findById(id).orElse(null);
        if(member==null || member.getRole()!=User.Role.MEMBER) return "redirect:/members?error=Member+not+found";
        if(member.isActive()) {
            boolean hasIssued=tx.findByMemberOrderByBorrowDateDesc(member).stream().anyMatch(t->t.isIssued()||t.isReturnRequested());
            if(hasIssued) return "redirect:/members?error=Member+has+an+active+book.+Complete+the+return+before+deactivating+the+account.";
            member.setActive(false);
        }
        else {
            member.setActive(true);
        }
        users.save(member);
        return "redirect:/members?success=Member+status+updated";
    }
    @GetMapping("/membership-applications")
    public String applications(HttpSession s,Model m) {
        String g=guard(s,"LIBRARIAN");
        if(g!=null)return g;
        m.addAttribute("applications",applications.findAllByOrderByAppliedAtDesc());
        return "membership-applications";
    }
    @PostMapping("/membership-applications/{id}/approve")
    public String approveApplication(@PathVariable Long id,HttpSession s) {
        String g=guard(s,"LIBRARIAN");
        if(g!=null)return g;
        MembershipApplication a=applications.findById(id).orElseThrow();
        if(a.getStatus()!=MembershipApplication.Status.PENDING)return "redirect:/membership-applications";
        if(users.findByEmail(a.getEmail()).isPresent()) {
            a.setStatus(MembershipApplication.Status.REJECTED);
            applications.save(a);
            return "redirect:/membership-applications?error=An+account+already+exists";
        }
        String membership="MEM"+String.format("%04d",users.countByRole(User.Role.MEMBER)+1);
        User u=new User(membership,a.getName(),a.getEmail(),a.getPassword(),User.Role.MEMBER);
        u.setActive(true);
        users.save(u);
        a.setStatus(MembershipApplication.Status.APPROVED);
        applications.save(a);
        return "redirect:/membership-applications?success=Membership+approved+and+account+created";
    }
    @PostMapping("/membership-applications/{id}/reject")
    public String rejectApplication(@PathVariable Long id,HttpSession s) {
        String g=guard(s,"LIBRARIAN");
        if(g!=null)return g;
        MembershipApplication a=applications.findById(id).orElseThrow();
        a.setStatus(MembershipApplication.Status.REJECTED);
        applications.save(a);
        return "redirect:/membership-applications?success=Application+rejected";
    }
    @GetMapping("/transactions")
    public String transactions(HttpSession s,Model m) {
        String g=guard(s,"LIBRARIAN");
        if(g!=null)return g;
        m.addAttribute("transactions",tx.findAllByOrderByBorrowDateDesc());
        m.addAttribute("books",books.findAll());
        m.addAttribute("members",users.findAll().stream().filter(u->u.getRole()==User.Role.MEMBER&&u.isActive()).toList());
        return "transactions";
    }
    @PostMapping("/transactions/issue")
    public String issue(@RequestParam Long bookId,@RequestParam Long memberId,HttpSession s) {
        String g=guard(s,"LIBRARIAN");
        if(g!=null)return g;
        try {
            service.issue(bookId,users.findById(memberId).orElseThrow());
            return "redirect:/transactions?success=Book+issued+successfully";
        }
        catch(Exception e) {
            return "redirect:/transactions?error="+URLEncoder.encode(e.getMessage(),StandardCharsets.UTF_8);
        }
    }
    @PostMapping("/transactions/approve/{id}")
    public String approveBorrow(@PathVariable Long id,HttpSession s) {
        String g=guard(s,"LIBRARIAN");
        if(g!=null)return g;
        try {
            service.approveBorrow(id);
            return "redirect:/transactions?success=Borrow+request+approved+and+book+issued";
        }
        catch(Exception e) {
            return "redirect:/transactions?error="+URLEncoder.encode(e.getMessage(),StandardCharsets.UTF_8);
        }
    }
    @PostMapping("/transactions/reject/{id}")
    public String rejectBorrow(@PathVariable Long id,HttpSession s) {
        String g=guard(s,"LIBRARIAN");
        if(g!=null)return g;
        try {
            service.rejectBorrow(id);
            return "redirect:/transactions?success=Borrow+request+declined";
        }
        catch(Exception e) {
            return "redirect:/transactions?error="+URLEncoder.encode(e.getMessage(),StandardCharsets.UTF_8);
        }
    }
    @PostMapping("/transactions/return/{id}")
    public String librarianReturn(@PathVariable Long id,HttpSession s) {
        String g=guard(s,"LIBRARIAN");
        if(g!=null)return g;
        Transaction t=tx.findById(id).orElse(null);
        if(t!=null&&!t.isReturned()) {
            long fine=service.calculateFine(t);
            service.completeReturn(t);
            notes.save(new Notification(t.getMember(),"Book returned and verified: "+t.getBook().getTitle()+". Final fine: ₹"+fine+"."));
        }
        return "redirect:/transactions";
    }
    @GetMapping("/my-books")
    public String myBooks(HttpSession s,Model m) {
        User u=current(s);
        if(u==null)return "redirect:/login";
        m.addAttribute("user",u);
        m.addAttribute("loans",tx.findByMemberOrderByBorrowDateDesc(u));
        return "my-books";
    }
    @PostMapping("/borrow/{id}")
    public String borrow(@PathVariable Long id,HttpSession s) {
        User u=current(s);
        if(u==null)return "redirect:/login";
        try {
            service.requestBorrow(id,u);
            return "redirect:/books?success=Borrow+request+submitted.+Wait+for+librarian+confirmation.";
        }
        catch(Exception e) {
            return "redirect:/books?error="+URLEncoder.encode(e.getMessage(),StandardCharsets.UTF_8);
        }
    }
    @PostMapping("/borrow/cancel/{id}")
    public String cancelBorrow(@PathVariable Long id,HttpSession s) {
        User u=current(s);
        if(u==null)return "redirect:/login";
        try {
            Transaction t=tx.findById(id).orElseThrow();
            if(!t.getMember().getId().equals(u.getId())) throw new IllegalStateException("You cannot cancel this request.");
            if(!t.isBorrowRequested()) throw new IllegalStateException("Only pending borrow requests can be cancelled.");
            t.setStatus(Transaction.Status.CANCELLED);
            tx.save(t);
            return "redirect:/my-books?success=Borrow+request+cancelled+successfully";
        }
        catch(Exception e) {
            return "redirect:/my-books?error="+URLEncoder.encode(e.getMessage(),StandardCharsets.UTF_8);
        }
    }
    @PostMapping("/return/{id}")
    public String returnBook(@PathVariable Long id,HttpSession s) {
        User u=current(s);
        if(u==null)return "redirect:/login";
        try {
            service.requestReturn(id,u);
            return "redirect:/my-books?success=Return+request+submitted.+Give+the+book+to+the+librarian+for+confirmation.";
        }
        catch(Exception e) {
            return "redirect:/my-books?error="+URLEncoder.encode(e.getMessage(),StandardCharsets.UTF_8);
        }
    }
    @GetMapping("/notifications")
    public String notifications(HttpSession s,Model m) {
        User u=current(s);
        if(u==null)return "redirect:/login";
        m.addAttribute("user",u);
        m.addAttribute("notifications",notes.findByRecipientOrderByCreatedAtDesc(u));
        m.addAttribute("unread",notes.countByRecipientAndReadFlagFalse(u));
        return "notifications";
    }
    @PostMapping("/notifications/read")
    public String markRead(HttpSession s) {
        User u=current(s);
        if(u==null)return "redirect:/login";
        var list=notes.findByRecipientOrderByCreatedAtDesc(u);
        list.forEach(n->n.setReadFlag(true));
        notes.saveAll(list);
        return "redirect:/notifications";
    }
    @GetMapping("/profile")
    public String profile(HttpSession s,Model m) {
        User u=current(s);
        if(u==null)return "redirect:/login";
        m.addAttribute("user",u);
        return "profile";
    }
    @PostMapping("/profile/save")
    public String saveProfile(@RequestParam String name,@RequestParam String password,HttpSession s) {
        User u=current(s);
        if(u==null)return "redirect:/login";
        u.setName(name);
        if(password!=null&&!password.isBlank())u.setPassword(passwords.encode(password));
        users.save(u);
        return "redirect:/profile?success=Profile+updated";
    }
    @PostMapping("/profile/preferences")
    public String savePreferences(@RequestParam(defaultValue="false") boolean dueAlerts,@RequestParam(defaultValue="false") boolean newBookAlerts,HttpSession s) {
        User u=current(s);
        if(u==null)return "redirect:/login";
        u.setDueAlerts(dueAlerts);
        u.setNewBookAlerts(newBookAlerts);
        users.save(u);
        return "redirect:/profile?success=Notification+preferences+updated";
    }
    @GetMapping("/reports")
    public String reports(HttpSession s,Model m) {
        String g=guard(s,"LIBRARIAN");
        if(g!=null)return g;
        var snapshot=concurrentReports.buildSnapshot();
        var all=snapshot.transactions();
        m.addAttribute("bookCount",snapshot.totalBooks());
        m.addAttribute("available",snapshot.availableBooks());
        m.addAttribute("borrowed",concurrentReports.calculateBorrowed(all));
        m.addAttribute("members",users.countByRoleAndActiveTrue(User.Role.MEMBER));
        m.addAttribute("overdue",concurrentReports.calculateOverdue(all));
        m.addAttribute("fineTotal",all.stream().filter(t->t.isIssued()||t.isReturnRequested()).mapToLong(service::calculateFine).sum());
        // Review 1: direct JDBC report values, independent from JPA values above.
        m.addAttribute("jdbcTotalBooks",jdbcReports.getTotalBooks());
        m.addAttribute("jdbcAvailableBooks",jdbcReports.getAvailableBooks());
        m.addAttribute("jdbcActiveMembers",jdbcReports.getActiveMembers());
        m.addAttribute("jdbcConnectionStatus",jdbcConnectionDemo.checkConnection());
        return "reports";
    }
    @GetMapping("/notifications/manage")
    public String manageNotifications(HttpSession s,Model m) {
        String g=guard(s,"LIBRARIAN");
        if(g!=null)return g;
        m.addAttribute("members",users.findAll().stream().filter(u->u.getRole()==User.Role.MEMBER&&u.isActive()).toList());
        return "notification-form";
    }
    @PostMapping("/notifications/send")
    public String sendNotification(@RequestParam String message,@RequestParam(defaultValue="ALL") String recipient,HttpSession s) {
        String g=guard(s,"LIBRARIAN");
        if(g!=null)return g;
        if("ALL".equals(recipient))service.notifyAllMembers(message,false);
        else users.findById(Long.valueOf(recipient)).ifPresent(u->service.notifyMember(u,message));
        return "redirect:/notifications/manage?success=Notification+sent";
    }
}
