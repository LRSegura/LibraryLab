package lending.usecase;

import catalog.model.Book;
import catalog.model.BookStatus;
import catalog.port.BookRepository;
import common.TestServiceHelper;
import common.exception.BusinessRuleException;
import common.exception.EntityNotFoundException;
import jakarta.validation.Validator;
import lending.dto.LoanDTO;
import lending.model.Loan;
import lending.model.LoanStatus;
import lending.port.LoanRepository;
import membership.model.Member;
import membership.model.MemberStatus;
import membership.port.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for LoanService — the most orchestration-heavy service.
 *
 * borrowBook() coordinates three repositories plus two domain entity state
 * transitions (Book.borrowCopy(), Member.incrementActiveLoans()), making it
 * the richest scenario. Each guard condition (book not found, member not found,
 * book unavailable, member ineligible, duplicate active loan) is verified
 * independently to ensure exactly one repository call triggers the failure.
 *
 * Date-sensitive assertions rely on the fact that new Loan() sets
 * dueDate = today + 14, so freshly created loans are always within their
 * period and canRenew() returns true by default.
 *
 * Tested methods:
 *   - borrowBook(): full guard chain and happy path (with and without notes)
 *   - returnBook(): not-found, already-returned guard, happy path
 *   - renewLoan(): not-found, renewal-limit guard, happy path
 *   - markAsLost(): not-found, already-returned guard, happy path
 *   - updateOverdueStatus(): batch transition for overdue active loans
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("LoanService")
class LoanServiceTest {

    @Mock
    private LoanRepository loanRepository;

    @Mock
    private BookRepository bookRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private Validator validator;

    @InjectMocks
    private LoanService loanService;

    // Shared fixtures — reset by @BeforeEach before every test
    private Book book;
    private Member member;

    @BeforeEach
    void setUp() {
        TestServiceHelper.injectValidator(loanService, validator);

        // An available book with multiple copies
        book = new Book("978-0-13-468599-1", "Effective Java", "Joshua Bloch");
        book.setTotalCopies(3);
        book.setAvailableCopies(3);
        book.setStatus(BookStatus.AVAILABLE);

        // An active member with room to borrow (defaults: ACTIVE, 0 active loans, max 5, +1yr expiry)
        member = new Member("MEM-001", "John", "Doe", "john@example.com");
    }

    // =========================================================================
    // borrowBook()
    // =========================================================================

    @Nested
    @DisplayName("borrowBook()")
    class BorrowBookTests {

        @Test
        @DisplayName("Should throw EntityNotFoundException and never save when book ID does not exist")
        void shouldThrowWhenBookNotFound() {
            when(bookRepository.findById(1L)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class, () -> loanService.borrowBook(1L, 1L));
            verify(loanRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw EntityNotFoundException and never save when member ID does not exist")
        void shouldThrowWhenMemberNotFound() {
            when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
            when(memberRepository.findById(1L)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class, () -> loanService.borrowBook(1L, 1L));
            verify(loanRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw BusinessRuleException when book has no available copies")
        void shouldThrowWhenBookNotAvailable() {
            book.setAvailableCopies(0); // all copies are currently on loan

            when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
            when(memberRepository.findById(1L)).thenReturn(Optional.of(member));

            assertThrows(BusinessRuleException.class, () -> loanService.borrowBook(1L, 1L));
            verify(loanRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw BusinessRuleException when member is not eligible to borrow")
        void shouldThrowWhenMemberCannotBorrow() {
            member.setStatus(MemberStatus.SUSPENDED); // suspended members cannot borrow

            when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
            when(memberRepository.findById(1L)).thenReturn(Optional.of(member));

            assertThrows(BusinessRuleException.class, () -> loanService.borrowBook(1L, 1L));
            verify(loanRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw BusinessRuleException when member already has an active loan for this book")
        void shouldThrowWhenActiveLoanAlreadyExists() {
            Loan existingLoan = new Loan(book, member);

            when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
            when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
            when(loanRepository.findActiveByBookAndMember(book, member)).thenReturn(Optional.of(existingLoan));

            assertThrows(BusinessRuleException.class, () -> loanService.borrowBook(1L, 1L));
            verify(loanRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should create loan, decrement book copies, and increment member active loans on success")
        void shouldBorrowBookSuccessfully() {
            when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
            when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
            when(loanRepository.findActiveByBookAndMember(book, member)).thenReturn(Optional.empty());

            LoanDTO result = loanService.borrowBook(1L, 1L);

            assertAll("Successful borrow",
                    () -> assertNotNull(result),
                    () -> assertEquals(2, book.getAvailableCopies(),
                            "One copy should be decremented from available stock"),
                    () -> assertEquals(1, member.getActiveLoans(),
                            "Member's active loan count should increase by 1")
            );
            verify(loanRepository).save(any(Loan.class));
            verify(bookRepository).update(book);
        }

        @Test
        @DisplayName("Should attach notes to the loan when non-blank notes are provided")
        void shouldSetNotesWhenProvided() {
            when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
            when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
            when(loanRepository.findActiveByBookAndMember(book, member)).thenReturn(Optional.empty());

            LoanDTO result = loanService.borrowBook(1L, 1L, "Reserved for study group");

            assertEquals("Reserved for study group", result.getNotes());
        }
    }

    // =========================================================================
    // returnBook()
    // =========================================================================

    @Nested
    @DisplayName("returnBook()")
    class ReturnBookTests {

        @Test
        @DisplayName("Should throw EntityNotFoundException when loan ID does not exist")
        void shouldThrowWhenLoanNotFound() {
            when(loanRepository.findById(1L)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class, () -> loanService.returnBook(1L));
        }

        @Test
        @DisplayName("Should throw BusinessRuleException when loan is already in RETURNED status")
        void shouldThrowWhenAlreadyReturned() {
            Loan loan = new Loan(book, member);
            loan.setStatus(LoanStatus.RETURNED);

            when(loanRepository.findById(1L)).thenReturn(Optional.of(loan));

            assertThrows(BusinessRuleException.class, () -> loanService.returnBook(1L));
        }

        @Test
        @DisplayName("Should mark loan RETURNED, restore book copy, and decrement member active loans")
        void shouldReturnBookSuccessfully() {
            book.setAvailableCopies(2); // 1 copy currently on loan
            member.setActiveLoans(1);
            Loan loan = new Loan(book, member); // ACTIVE by default

            when(loanRepository.findById(1L)).thenReturn(Optional.of(loan));

            LoanDTO result = loanService.returnBook(1L);

            assertAll("Successful return",
                    () -> assertNotNull(result),
                    () -> assertEquals(LoanStatus.RETURNED, loan.getStatus()),
                    () -> assertEquals(3, book.getAvailableCopies(),
                            "Returned copy should be restored to available stock"),
                    () -> assertEquals(0, member.getActiveLoans(),
                            "Member's active loan count should decrease by 1")
            );
            verify(loanRepository).update(loan);
            verify(bookRepository).update(book);
        }
    }

    // =========================================================================
    // renewLoan()
    // =========================================================================

    @Nested
    @DisplayName("renewLoan()")
    class RenewLoanTests {

        @Test
        @DisplayName("Should throw EntityNotFoundException when loan ID does not exist")
        void shouldThrowWhenLoanNotFound() {
            when(loanRepository.findById(1L)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class, () -> loanService.renewLoan(1L));
        }

        @Test
        @DisplayName("Should throw BusinessRuleException when maximum renewals have been reached")
        void shouldThrowWhenMaxRenewalsReached() {
            Loan loan = new Loan(book, member);
            loan.setRenewalCount(2); // MAX_RENEWALS = 2 → canRenew() returns false

            when(loanRepository.findById(1L)).thenReturn(Optional.of(loan));

            assertThrows(BusinessRuleException.class, () -> loanService.renewLoan(1L));
        }

        @Test
        @DisplayName("Should extend due date by 14 days and increment renewal count on success")
        void shouldRenewLoanSuccessfully() {
            // Freshly created loan: ACTIVE, not overdue, renewalCount = 0
            Loan loan = new Loan(book, member);

            when(loanRepository.findById(1L)).thenReturn(Optional.of(loan));

            LoanDTO result = loanService.renewLoan(1L);

            assertAll("Successful renewal",
                    () -> assertNotNull(result),
                    () -> assertEquals(1, loan.getRenewalCount()),
                    () -> assertEquals(LocalDate.now().plusDays(14), loan.getDueDate(),
                            "Due date resets to 14 days from today on renewal")
            );
            verify(loanRepository).update(loan);
        }
    }

    // =========================================================================
    // markAsLost()
    // =========================================================================

    @Nested
    @DisplayName("markAsLost()")
    class MarkAsLostTests {

        @Test
        @DisplayName("Should throw EntityNotFoundException when loan ID does not exist")
        void shouldThrowWhenLoanNotFound() {
            when(loanRepository.findById(1L)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class, () -> loanService.markAsLost(1L));
        }

        @Test
        @DisplayName("Should throw BusinessRuleException when loan is already in RETURNED status")
        void shouldThrowWhenLoanAlreadyReturned() {
            Loan loan = new Loan(book, member);
            loan.setStatus(LoanStatus.RETURNED);

            when(loanRepository.findById(1L)).thenReturn(Optional.of(loan));

            assertThrows(BusinessRuleException.class, () -> loanService.markAsLost(1L));
        }

        @Test
        @DisplayName("Should mark loan LOST, decrement member loans, and reduce book total copies")
        void shouldMarkLoanAsLostSuccessfully() {
            book.setAvailableCopies(2); // 1 copy currently on loan
            member.setActiveLoans(1);
            Loan loan = new Loan(book, member); // ACTIVE

            when(loanRepository.findById(1L)).thenReturn(Optional.of(loan));

            LoanDTO result = loanService.markAsLost(1L);

            assertAll("Loan marked as lost",
                    () -> assertNotNull(result),
                    () -> assertEquals(LoanStatus.LOST, loan.getStatus()),
                    () -> assertEquals(0, member.getActiveLoans(),
                            "The lost book closes the active loan — member count decreases"),
                    () -> assertEquals(2, book.getTotalCopies(),
                            "Lost book is permanently removed from the inventory")
            );
            verify(loanRepository).update(loan);
            verify(bookRepository).update(book);
        }
    }

    // =========================================================================
    // updateOverdueStatus()
    // =========================================================================

    @Nested
    @DisplayName("updateOverdueStatus()")
    class UpdateOverdueStatusTests {

        @Test
        @DisplayName("Should transition overdue active loans to OVERDUE and leave current loans unchanged")
        void shouldUpdateOnlyOverdueActiveLoans() {
            Loan overdueLoan = new Loan(book, member);
            overdueLoan.setDueDate(LocalDate.now().minusDays(3)); // 3 days past due → isOverdue() = true

            Loan currentLoan = new Loan(book, member);
            // dueDate defaults to today + 14 → isOverdue() = false

            when(loanRepository.findByStatus(LoanStatus.ACTIVE))
                    .thenReturn(List.of(overdueLoan, currentLoan));

            loanService.updateOverdueStatus();

            assertAll("Overdue status update",
                    () -> assertEquals(LoanStatus.OVERDUE, overdueLoan.getStatus(),
                            "Loans past their due date must transition to OVERDUE"),
                    () -> assertEquals(LoanStatus.ACTIVE, currentLoan.getStatus(),
                            "Loans still within their lending period must not be touched")
            );
            // Only the overdue loan should be written back to the repository
            verify(loanRepository).update(overdueLoan);
            verify(loanRepository, never()).update(currentLoan);
        }

        @Test
        @DisplayName("Should perform no updates when no active loans are overdue")
        void shouldDoNothingWhenNoLoansAreOverdue() {
            Loan currentLoan = new Loan(book, member); // due in 14 days

            when(loanRepository.findByStatus(LoanStatus.ACTIVE)).thenReturn(List.of(currentLoan));

            loanService.updateOverdueStatus();

            verify(loanRepository, never()).update(any());
        }
    }
}
