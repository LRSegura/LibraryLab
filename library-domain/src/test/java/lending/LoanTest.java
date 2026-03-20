package lending;

import catalog.model.Book;
import catalog.model.BookStatus;
import common.TestEntityHelper;
import common.exception.BusinessRuleException;
import lending.model.Loan;
import lending.model.LoanStatus;
import membership.model.Member;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for the Loan domain entity.
 *
 * Loan is the most behavior-rich entity in the Lending bounded context.
 * Its business rules coordinate state transitions (ACTIVE → RETURNED,
 * ACTIVE → OVERDUE, ACTIVE → LOST) and enforce constraints on renewals.
 *
 * Date-dependent methods (isOverdue, getDaysOverdue, canRenew) are tricky
 * to test because they depend on LocalDate.now(). We use reflection to
 * set loanDate and dueDate directly, giving us deterministic control
 * over the time dimension without requiring a Clock abstraction.
 *
 * Tested methods:
 *   - Constructors: default loan days and custom loan days
 *   - isOverdue(): date boundary conditions
 *   - getDaysOverdue(): calculation accuracy
 *   - getDaysUntilDue(): remaining time calculation
 *   - canRenew(): composite eligibility check
 *   - renew() / renew(int): renewal with guard conditions
 *   - returnBook(): status transition and guard
 *   - markAsLost(): status transition
 */
@DisplayName("Loan Entity")
class LoanTest {

    private Book book;
    private Member member;
    private Loan loan;

    @BeforeEach
    void setUp() {
        book = new Book("978-0-13-468599-1", "Effective Java", "Joshua Bloch");
        book.setTotalCopies(3);
        book.setAvailableCopies(3);
        book.setStatus(BookStatus.AVAILABLE);

        member = new Member("MEM-001", "Luis", "García", "luis@example.com");

        // Default loan: 14-day period starting today
        loan = new Loan(book, member);
    }

    /**
     * Helper to manipulate loan dates via reflection for deterministic testing.
     * In production, these fields are set by the constructor and JPA lifecycle.
     */
    private void setLoanDates(Loan loan, LocalDate loanDate, LocalDate dueDate) {
        try {
            Field loanDateField = Loan.class.getDeclaredField("loanDate");
            loanDateField.setAccessible(true);
            loanDateField.set(loan, loanDate);

            Field dueDateField = Loan.class.getDeclaredField("dueDate");
            dueDateField.setAccessible(true);
            dueDateField.set(loan, dueDate);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Failed to set loan dates for testing", e);
        }
    }

    // =========================================================================
    // Constructor and Initial State
    // =========================================================================

    @Nested
    @DisplayName("Constructor and Defaults")
    class ConstructorTests {

        @Test
        @DisplayName("Default constructor should set loan date to today and due date 14 days ahead")
        void defaultConstructorShouldSetDates() {
            Loan newLoan = new Loan(book, member);

            assertAll("Default loan dates",
                    () -> assertEquals(LocalDate.now(), newLoan.getLoanDate()),
                    () -> assertEquals(LocalDate.now().plusDays(14), newLoan.getDueDate())
            );
        }

        @Test
        @DisplayName("Custom days constructor should use specified loan period")
        void customDaysConstructorShouldUseSpecifiedPeriod() {
            Loan customLoan = new Loan(book, member, 30);

            assertEquals(LocalDate.now().plusDays(30), customLoan.getDueDate(),
                    "Due date should be 30 days from today, not the default 14");
        }

        @Test
        @DisplayName("Should initialize with ACTIVE status, 0 renewals, and null return date")
        void shouldHaveCorrectDefaults() {
            assertAll("Initial loan state",
                    () -> assertEquals(LoanStatus.ACTIVE, loan.getStatus()),
                    () -> assertEquals(0, loan.getRenewalCount()),
                    () -> assertNull(loan.getReturnDate()),
                    () -> assertNull(loan.getNotes())
            );
        }

        @Test
        @DisplayName("Should store references to book and member")
        void shouldStoreEntityReferences() {
            assertAll("Entity references",
                    () -> assertSame(book, loan.getBook()),
                    () -> assertSame(member, loan.getMember())
            );
        }
    }

    // =========================================================================
    // isOverdue()
    // =========================================================================

    @Nested
    @DisplayName("isOverdue()")
    class IsOverdueTests {

        @Test
        @DisplayName("Should return false when due date is in the future")
        void shouldNotBeOverdueWhenDueDateInFuture() {
            // Default loan: due in 14 days
            assertFalse(loan.isOverdue());
        }

        @Test
        @DisplayName("Should return false on the exact due date (not yet past)")
        void shouldNotBeOverdueOnDueDate() {
            // Set due date to today — isAfter(today) is false
            setLoanDates(loan, LocalDate.now().minusDays(14), LocalDate.now());

            assertFalse(loan.isOverdue(),
                    "Loan is still valid on the due date itself — overdue starts the day after");
        }

        @Test
        @DisplayName("Should return true when due date is in the past")
        void shouldBeOverdueWhenDueDatePassed() {
            setLoanDates(loan, LocalDate.now().minusDays(20), LocalDate.now().minusDays(6));

            assertTrue(loan.isOverdue());
        }

        @Test
        @DisplayName("Should return true when one day past due")
        void shouldBeOverdueOneDayLate() {
            setLoanDates(loan, LocalDate.now().minusDays(15), LocalDate.now().minusDays(1));

            assertTrue(loan.isOverdue(),
                    "Even one day past due should count as overdue");
        }

        @Test
        @DisplayName("Should return false when book has been returned (even if past due)")
        void shouldNotBeOverdueWhenReturned() {
            setLoanDates(loan, LocalDate.now().minusDays(20), LocalDate.now().minusDays(6));
            loan.returnBook(); // sets returnDate to today

            // isOverdue checks returnDate == null first
            assertFalse(loan.isOverdue(),
                    "Returned books are never considered overdue, regardless of dates");
        }
    }

    // =========================================================================
    // getDaysOverdue()
    // =========================================================================

    @Nested
    @DisplayName("getDaysOverdue()")
    class DaysOverdueTests {

        @Test
        @DisplayName("Should return 0 when loan is not overdue")
        void shouldReturnZeroWhenNotOverdue() {
            assertEquals(0, loan.getDaysOverdue());
        }

        @Test
        @DisplayName("Should calculate correct number of overdue days")
        void shouldCalculateCorrectOverdueDays() {
            // Due 5 days ago → 5 days overdue
            setLoanDates(loan, LocalDate.now().minusDays(19), LocalDate.now().minusDays(5));

            assertEquals(5, loan.getDaysOverdue());
        }

        @Test
        @DisplayName("Should return 1 when one day past due")
        void shouldReturnOneWhenOneDayLate() {
            setLoanDates(loan, LocalDate.now().minusDays(15), LocalDate.now().minusDays(1));

            assertEquals(1, loan.getDaysOverdue());
        }
    }

    // =========================================================================
    // getDaysUntilDue()
    // =========================================================================

    @Nested
    @DisplayName("getDaysUntilDue()")
    class DaysUntilDueTests {

        @Test
        @DisplayName("Should return correct remaining days for active loan")
        void shouldReturnRemainingDays() {
            // Default loan: due in 14 days
            assertEquals(14, loan.getDaysUntilDue());
        }

        @Test
        @DisplayName("Should return 0 when book has been returned")
        void shouldReturnZeroWhenReturned() {
            loan.returnBook();

            assertEquals(0, loan.getDaysUntilDue(),
                    "Returned loans should show 0 days until due");
        }

        @Test
        @DisplayName("Should return negative value when overdue")
        void shouldReturnNegativeWhenOverdue() {
            setLoanDates(loan, LocalDate.now().minusDays(20), LocalDate.now().minusDays(5));

            // ChronoUnit.DAYS.between(today, pastDate) gives negative
            assertTrue(loan.getDaysUntilDue() < 0,
                    "Overdue loans should have negative days until due");
        }
    }

    // =========================================================================
    // canRenew()
    // =========================================================================

    @Nested
    @DisplayName("canRenew()")
    class CanRenewTests {

        @Test
        @DisplayName("Should return true for active, non-overdue loan with renewals remaining")
        void shouldAllowRenewalWhenEligible() {
            assertTrue(loan.canRenew());
        }

        @Test
        @DisplayName("Should return false when renewal count reaches MAX_RENEWALS (2)")
        void shouldDenyRenewalWhenMaxReached() {
            loan.renew();  // renewal 1
            loan.renew();  // renewal 2

            assertFalse(loan.canRenew(),
                    "Maximum of 2 renewals should be enforced");
        }

        @Test
        @DisplayName("Should return false when loan is overdue")
        void shouldDenyRenewalWhenOverdue() {
            setLoanDates(loan, LocalDate.now().minusDays(20), LocalDate.now().minusDays(5));

            assertFalse(loan.canRenew(),
                    "Overdue loans cannot be renewed — member must return first");
        }

        @Test
        @DisplayName("Should return false when loan status is RETURNED")
        void shouldDenyRenewalWhenReturned() {
            loan.returnBook();

            assertFalse(loan.canRenew(),
                    "Returned loans cannot be renewed");
        }

        @Test
        @DisplayName("Should return false when loan status is LOST")
        void shouldDenyRenewalWhenLost() {
            loan.markAsLost();

            assertFalse(loan.canRenew());
        }

        @Test
        @DisplayName("Should allow exactly one more renewal after first renewal")
        void shouldAllowOneMoreAfterFirstRenewal() {
            loan.renew(); // renewal 1

            assertTrue(loan.canRenew(),
                    "Should still have one renewal remaining after first use");
        }
    }

    // =========================================================================
    // renew()
    // =========================================================================

    @Nested
    @DisplayName("renew()")
    class RenewTests {

        @Test
        @DisplayName("Default renew should extend due date by 14 days from today")
        void defaultRenewShouldExtendByDefaultDays() {
            loan.renew();

            assertEquals(LocalDate.now().plusDays(14), loan.getDueDate(),
                    "Renewal resets due date to 14 days from today (not from old due date)");
        }

        @Test
        @DisplayName("Custom renew should extend due date by specified days from today")
        void customRenewShouldExtendBySpecifiedDays() {
            loan.renew(21);

            assertEquals(LocalDate.now().plusDays(21), loan.getDueDate());
        }

        @Test
        @DisplayName("Should increment renewal count")
        void shouldIncrementRenewalCount() {
            assertEquals(0, loan.getRenewalCount());

            loan.renew();
            assertEquals(1, loan.getRenewalCount());

            loan.renew();
            assertEquals(2, loan.getRenewalCount());
        }

        @Test
        @DisplayName("Should throw BusinessRuleException when canRenew() is false")
        void shouldThrowWhenCannotRenew() {
            loan.renew(); // 1st
            loan.renew(); // 2nd — max reached

            assertThrows(BusinessRuleException.class, () -> loan.renew(),
                    "Third renewal attempt should be rejected");
        }

        @Test
        @DisplayName("Should throw when loan is overdue")
        void shouldThrowWhenOverdue() {
            setLoanDates(loan, LocalDate.now().minusDays(20), LocalDate.now().minusDays(5));

            assertThrows(BusinessRuleException.class, () -> loan.renew());
        }

        @Test
        @DisplayName("Should not change loan status on renewal")
        void shouldNotChangeStatus() {
            loan.renew();

            assertEquals(LoanStatus.ACTIVE, loan.getStatus(),
                    "Renewal should keep the loan in ACTIVE status");
        }
    }

    // =========================================================================
    // returnBook()
    // =========================================================================

    @Nested
    @DisplayName("returnBook()")
    class ReturnBookTests {

        @Test
        @DisplayName("Should set return date to today")
        void shouldSetReturnDateToToday() {
            loan.returnBook();

            assertEquals(LocalDate.now(), loan.getReturnDate());
        }

        @Test
        @DisplayName("Should transition status to RETURNED")
        void shouldSetStatusToReturned() {
            loan.returnBook();

            assertEquals(LoanStatus.RETURNED, loan.getStatus());
        }

        @Test
        @DisplayName("Should throw BusinessRuleException when already returned")
        void shouldThrowWhenAlreadyReturned() {
            loan.returnBook(); // first return succeeds

            assertThrows(BusinessRuleException.class, () -> loan.returnBook(),
                    "Double return should be rejected — book is already back");
        }

        @Test
        @DisplayName("Should allow return of overdue loan")
        void shouldAllowReturnOfOverdueLoan() {
            setLoanDates(loan, LocalDate.now().minusDays(20), LocalDate.now().minusDays(5));

            // Should not throw — overdue loans can still be returned
            assertDoesNotThrow(() -> loan.returnBook());
            assertEquals(LoanStatus.RETURNED, loan.getStatus());
        }
    }

    // =========================================================================
    // markAsLost()
    // =========================================================================

    @Nested
    @DisplayName("markAsLost()")
    class MarkAsLostTests {

        @Test
        @DisplayName("Should transition status to LOST")
        void shouldSetStatusToLost() {
            loan.markAsLost();

            assertEquals(LoanStatus.LOST, loan.getStatus());
        }

        @Test
        @DisplayName("Should not set a return date")
        void shouldNotSetReturnDate() {
            loan.markAsLost();

            assertNull(loan.getReturnDate(),
                    "Lost books are not returned — return date stays null");
        }
    }

    // =========================================================================
    // equals() and hashCode()
    // =========================================================================

    @Nested
    @DisplayName("Equals and HashCode")
    class EqualsHashCodeTests {

        @Test
        @DisplayName("Two loans with same book, member, and dates should be equal when IDs match")
        void shouldBeEqualWithSameFieldsAndId() {
            Loan loan1 = new Loan(book, member);
            Loan loan2 = new Loan(book, member);
            TestEntityHelper.setId(loan1, 1L);
            TestEntityHelper.setId(loan2, 1L);

            // Both created at the same moment → same loanDate and dueDate
            assertEquals(loan1, loan2);
            assertEquals(loan1.hashCode(), loan2.hashCode());
        }

        @Test
        @DisplayName("Two loans with different books should not be equal")
        void shouldNotBeEqualWithDifferentBooks() {
            Book otherBook = new Book("0987654321", "Clean Code", "Robert C. Martin");
            Loan loan1 = new Loan(book, member);
            Loan loan2 = new Loan(otherBook, member);
            TestEntityHelper.setId(loan1, 1L);
            TestEntityHelper.setId(loan2, 1L);

            assertNotEquals(loan1, loan2);
        }
    }
}
