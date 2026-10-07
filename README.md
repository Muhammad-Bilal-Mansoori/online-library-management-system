# Online Library Management System

A Spring Boot + Thymeleaf + JPA library management system designed for a college Java project.

## Main Workflow

### Public Visitor

1. Open the website home page.
2. Browse and search the public catalog.
3. Apply for membership.
4. The application remains **Pending** until a librarian reviews it.
5. Only after approval is an active member account and membership ID created.

### Member

1. Login with an active account.
2. Search books and view availability.
3. Submit a **Borrow Request**.
4. The librarian verifies and approves the request and physically issues the book.
5. The transaction becomes **Issued** and a due date is created automatically.
6. The member can submit a **Return Request**.
7. The librarian verifies the physical book and confirms the return.
8. Only then does the transaction become **Returned** and the book becomes available again.

### Librarian

- Manage books and members.
- Review membership applications.
- Approve or decline borrow requests.
- Confirm physical returns.
- Directly issue a book when appropriate at the circulation desk.
- Send targeted or broadcast notifications.
- View inventory, circulation, overdue and fine reports.

## Business Rules

- Only active members can request or borrow books.
- A member can have at most 3 active loans.
- A book cannot be issued while unavailable.
- The borrowing period is 14 days.
- The overdue fine is ₹5 per late day.
- A member cannot directly mark a physical return as completed.
- Books and members with transaction history are protected from deletion.
- Librarian accounts cannot be publicly self-registered.
- Passwords are stored using BCrypt hashing.
- Due-date, overdue and new-arrival notifications respect member preferences.

## Demo Accounts

**Librarian**

```text
Email: librarian@library.com
Password: admin123
Role: Librarian
```

**Member**

```text
Email: member@library.com
Password: member123
Role: Member
```

## Run on Windows

Open PowerShell inside the `library-management` folder.

```powershell
.\mvnw.cmd spring-boot:run
```

Then open:

**http://localhost:8080**

The application uses port `8080` by default.

## Technology

- Java 17
- Spring Boot 3.5.6
- Spring MVC
- Thymeleaf
- Spring Data JPA / Hibernate
- H2 File Database
- JDBC
- BCrypt Password Hashing
- Maven

The H2 database is stored locally under `./data`, so no separate MySQL installation is required for the college demo.

## Review 1 Technical Demonstrations

### 1. JDBC

The project primarily uses Spring Data JPA for normal application operations.

A separate `com.library.management.jdbc` package demonstrates direct JDBC database access using:

- `DataSource`
- `Connection`
- `PreparedStatement`
- `ResultSet`

JDBC-based report values are also displayed on the librarian Reports page.

### 2. Multithreading and Synchronization

The `com.library.management.concurrency` package contains `ConcurrentReportService`.

Independent report calculations are executed concurrently using a fixed thread pool and `CompletableFuture`.

The service also demonstrates synchronized report calculations where shared service access could otherwise overlap.

### 3. Servlet and Web Integration

The project includes a direct Java Servlet at:

**http://localhost:8080/servlet/library-status**

`LibraryStatusServlet` handles the HTTP request, obtains the application's JDBC repository from the Spring application context, reads live library statistics, and returns an HTML response.

This provides a concrete Servlet and Web Integration implementation alongside the Spring MVC controllers.

## Project Structure

```text
library-management/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/library/management/
│   │   │       ├── controller/
│   │   │       ├── model/
│   │   │       ├── repository/
│   │   │       ├── service/
│   │   │       ├── jdbc/
│   │   │       ├── concurrency/
│   │   │       └── servlet/
│   │   └── resources/
│   │       ├── templates/
│   │       ├── static/
│   │       └── application.properties
│   └── test/
├── pom.xml
├── mvnw
├── mvnw.cmd
└── README.md
```

## Main Features

### Public Features

- Public home page
- Public book catalog
- Book search
- Membership application
- Membership approval workflow

### Member Features

- Secure login
- Book search
- Borrow requests
- Return requests
- Borrowing history
- Notifications
- Profile management
- Membership status

### Librarian Features

- Book management
- Member management
- Membership application approval
- Borrow request approval
- Return verification
- Direct book issue
- Notifications
- Inventory reports
- Circulation reports
- Overdue and fine reports

## Borrow and Return Workflow

```text
Member
   ↓
Borrow Request
   ↓
Librarian Verification
   ↓
Physical Book Issue
   ↓
Issued
   ↓
Member Return Request
   ↓
Physical Book Submitted
   ↓
Librarian Verification
   ↓
Returned
```

## Membership Workflow

```text
Public Visitor
      ↓
Membership Application
      ↓
Pending
      ↓
Librarian Review
      ↓
Approved / Rejected
      ↓
Active Member Account
```

## Database

The application uses an H2 file-based database.

Database location:

```text
./data/librarydb
```

The database is automatically created and managed by Spring Boot and Hibernate during application startup.

## Project Objective

The objective of this project is to provide a complete digital library management workflow where members can search and request books, while librarians manage books, members, transactions, notifications and reports through a centralized web application.

The system separates public access, member operations and librarian operations using role-based access control and follows realistic library circulation workflows.