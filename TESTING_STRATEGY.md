# LibraryLab — Testing Strategy

## Overview

This document describes the unit testing strategy for the LibraryLab monolith,
organized by architectural layer and phased by implementation priority.

## Phase 1: Domain Layer 

**Goal:** Verify every business rule enforced by domain entities.

**Why prioritize this?** Domain entities contain the core invariants of the system.
A bug in `Book.borrowCopy()` or `Member.canBorrow()` can corrupt data in ways that
cascade through the entire application. These tests are also the easiest to write —
pure Java, no framework dependencies, no mocking required.

### Test Classes

| Entity     | Test Class            | Key Methods Tested                                                     |
|------------|-----------------------|------------------------------------------------------------------------|
| `Book`     | `BookTest`            | `isAvailable()`, `borrowCopy()`, `returnCopy()`                       |
| `Category` | `CategoryTest`        | `addBook()`, `removeBook()` (bidirectional sync)                       |
| `Member`   | `MemberTest`          | `canBorrow()`, `isMembershipExpired()`, `incrementActiveLoans()`,      |
|            |                       | `decrementActiveLoans()`, `renewMembership()`                          |
| `Loan`     | `LoanTest`            | `isOverdue()`, `getDaysOverdue()`, `getDaysUntilDue()`, `canRenew()`, |
|            |                       | `renew()`, `returnBook()`, `markAsLost()`                              |

### Conventions Used

- **`@Nested` classes** group tests by method, making the test report read like documentation
- **`@DisplayName`** provides human-readable descriptions in test reports
- **`@BeforeEach`** sets up a "standard" entity state — tests then modify only what's relevant
- **`assertAll()`** groups related assertions so all failures are reported at once
- **`common.TestEntityHelper`** uses reflection to set `BaseEntity.id` (which has no public setter)
- **Comments explain the "why"**, not just the "what" — especially on boundary conditions

### Running the Tests

```bash
# Run all domain tests
mvn test -pl library-domain

# Run a specific test class
mvn test -pl library-domain -Dtest=BookTest

# Run a specific nested test group
mvn test -pl library-domain -Dtest="BookTest\$BorrowCopyTests"
```

## Phase 2: Application Layer

**Goal:** Verify service orchestration logic using mocked repositories.

Each service test class will use `@ExtendWith(MockitoExtension.class)` with
`@Mock` for repository interfaces and `@InjectMocks` for the service under test.

### Key scenarios per service:

**`BookServiceTest`** — create with duplicate ISBN (DuplicateEntityException),
update with category assignment, delete with active loans (BusinessRuleException),
updateCopies below loaned count.

**`MemberServiceTest`** — create with duplicate email, update with email collision,
delete with active loans, suspend/activate lifecycle, renewMembership.

**`LoanServiceTest`** — borrowBook full flow (book not found, member not found,
book unavailable, member can't borrow, duplicate active loan, happy path),
returnBook, renewLoan, markAsLost, updateOverdueStatus batch.

**`CategoryServiceTest`** — create with duplicate name, delete with associated books.

### Example Pattern

```java
@ExtendWith(MockitoExtension.class)
class BookServiceTest {

    @Mock
    private BookRepository bookRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private BookService bookService;

    @Test
    void createShouldThrowWhenIsbnAlreadyExists() {
        when(bookRepository.existsByIsbn("123")).thenReturn(true);
        BookDTO dto = BookDTO.builder().isbn("123").build();

        assertThrows(DuplicateEntityException.class,
                () -> bookService.create(dto));
        verify(bookRepository, never()).save(any());
    }
}
```

## Phase 3: REST Layer

**Goal:** Verify HTTP contract — status codes, response mapping, exception handling.

Tests will mock the service layer and verify that resources return correct
HTTP statuses (201 Created, 404 Not Found, 409 Conflict) and that
ExceptionMapper classes correctly translate domain exceptions to ErrorResponse.

## Test Infrastructure

### common.TestEntityHelper

`BaseEntity.id` is managed by JPA and has no public setter. For unit tests
that need entities with assigned IDs (equality checks, service layer mocking),
`common.TestEntityHelper.setId(entity, id)` uses reflection to set the field.

This is intentionally limited to test scope — production code should never
need to set IDs manually.

### Date Manipulation in Loan Tests

`Loan.isOverdue()` and related methods depend on `LocalDate.now()`. Rather than
introducing a `Clock` abstraction into the domain (which would add framework
coupling), loan tests use reflection to set `loanDate` and `dueDate` directly.

If the project later adopts a `Clock` pattern (common in DDD), these tests
can be simplified to inject a fixed clock instead.
