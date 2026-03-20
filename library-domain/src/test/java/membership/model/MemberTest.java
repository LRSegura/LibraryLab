package membership.model;

import common.TestEntityHelper;
import common.exception.BusinessRuleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the Member domain entity.
 *
 * Member is one of the richest entities in terms of business rules:
 * - Borrowing eligibility depends on status, active loans, AND expiration
 * - Loan counting must be consistent (increment/decrement guards)
 * - Membership renewal has different base-date behavior depending on expiration
 *
 * Each of these rules can produce subtle bugs if modified carelessly,
 * which is why comprehensive edge-case testing is essential here.
 */
@DisplayName("Member Entity")
class MemberTest {

    private Member member;

    @BeforeEach
    void setUp() {
        // Standard active member with capacity to borrow
        member = new Member("MEM-001", "Luis", "García", "luis@example.com");
    }

    // =========================================================================
    // Constructor and Initial State
    // =========================================================================

    @Nested
    @DisplayName("Constructor and Defaults")
    class ConstructorTests {

        @Test
        @DisplayName("Should set membership number, name fields, and email from constructor")
        void shouldSetFieldsFromConstructor() {
            assertAll("Constructor fields",
                    () -> assertEquals("MEM-001", member.getMembershipNumber()),
                    () -> assertEquals("Luis", member.getFirstName()),
                    () -> assertEquals("García", member.getLastName()),
                    () -> assertEquals("luis@example.com", member.getEmail())
            );
        }

        @Test
        @DisplayName("Should default to ACTIVE status, 0 active loans, and 5 max loans")
        void shouldHaveCorrectDefaults() {
            assertAll("Default values",
                    () -> assertEquals(MemberStatus.ACTIVE, member.getStatus()),
                    () -> assertEquals(0, member.getActiveLoans()),
                    () -> assertEquals(5, member.getMaxLoans())
            );
        }

        @Test
        @DisplayName("Should set registration date to today")
        void shouldSetRegistrationDateToToday() {
            assertEquals(LocalDate.now(), member.getRegistrationDate());
        }

        @Test
        @DisplayName("Should set expiration date to one year from today")
        void shouldSetExpirationToOneYearFromNow() {
            assertEquals(LocalDate.now().plusYears(1), member.getExpirationDate(),
                    "New members get a 1-year membership by default");
        }
    }

    // =========================================================================
    // getFullName()
    // =========================================================================

    @Test
    @DisplayName("getFullName() should concatenate first and last name with a space")
    void fullNameShouldConcatenateNames() {
        assertEquals("Luis García", member.getFullName());
    }

    // =========================================================================
    // isMembershipExpired()
    // =========================================================================

    @Nested
    @DisplayName("isMembershipExpired()")
    class MembershipExpiredTests {

        @Test
        @DisplayName("Should return false when expiration date is in the future")
        void shouldNotBeExpiredWhenFuture() {
            // Default constructor sets expiration 1 year ahead
            assertFalse(member.isMembershipExpired());
        }

        @Test
        @DisplayName("Should return true when expiration date is in the past")
        void shouldBeExpiredWhenPast() {
            member.setExpirationDate(LocalDate.now().minusDays(1));

            assertTrue(member.isMembershipExpired());
        }

        @Test
        @DisplayName("Should return false when expiration date is today (not yet past)")
        void shouldNotBeExpiredOnExactDay() {
            // isAfter is strict: today is NOT after today
            member.setExpirationDate(LocalDate.now());

            assertFalse(member.isMembershipExpired(),
                    "Membership valid until end of expiration day");
        }

        @Test
        @DisplayName("Should return false when expiration date is null")
        void shouldNotBeExpiredWhenNull() {
            member.setExpirationDate(null);

            assertFalse(member.isMembershipExpired(),
                    "Null expiration should be treated as non-expired (e.g., lifetime membership)");
        }
    }

    // =========================================================================
    // canBorrow()
    // =========================================================================

    @Nested
    @DisplayName("canBorrow()")
    class CanBorrowTests {

        @Test
        @DisplayName("Should return true for active member with capacity and valid membership")
        void shouldAllowBorrowForActiveMemberWithCapacity() {
            // member is ACTIVE, 0 active loans, 5 max, not expired
            assertTrue(member.canBorrow());
        }

        @Test
        @DisplayName("Should return false when member is SUSPENDED")
        void shouldDenyBorrowWhenSuspended() {
            member.setStatus(MemberStatus.SUSPENDED);

            assertFalse(member.canBorrow(),
                    "Suspended members must not borrow books");
        }

        @Test
        @DisplayName("Should return false when member is EXPIRED")
        void shouldDenyBorrowWhenExpiredStatus() {
            member.setStatus(MemberStatus.EXPIRED);

            assertFalse(member.canBorrow());
        }

        @Test
        @DisplayName("Should return false when member is INACTIVE")
        void shouldDenyBorrowWhenInactive() {
            member.setStatus(MemberStatus.INACTIVE);

            assertFalse(member.canBorrow());
        }

        @Test
        @DisplayName("Should return false when active loans equal max loans")
        void shouldDenyBorrowWhenAtMaxLoans() {
            member.setActiveLoans(5); // equals maxLoans
            // activeLoans < maxLoans fails → canBorrow returns false

            assertFalse(member.canBorrow(),
                    "Member at loan limit should not be allowed to borrow more");
        }

        @Test
        @DisplayName("Should return true when active loans are one below max")
        void shouldAllowBorrowWhenOneBelowMax() {
            member.setActiveLoans(4); // one below default maxLoans of 5

            assertTrue(member.canBorrow());
        }

        @Test
        @DisplayName("Should return false when membership is expired (date-based)")
        void shouldDenyBorrowWhenMembershipExpired() {
            member.setExpirationDate(LocalDate.now().minusDays(1));

            assertFalse(member.canBorrow(),
                    "Expired membership should block borrowing even if status is ACTIVE");
        }

        @Test
        @DisplayName("All three conditions must be true simultaneously")
        void shouldRequireAllThreeConditions() {
            // This test documents the AND relationship:
            // canBorrow = ACTIVE status AND activeLoans < maxLoans AND !expired

            // Scenario: ACTIVE + capacity + expired → should fail
            member.setExpirationDate(LocalDate.now().minusDays(1));
            assertFalse(member.canBorrow(), "Failed on: expired membership");

            // Reset expiration, now exceed loan limit
            member.setExpirationDate(LocalDate.now().plusYears(1));
            member.setActiveLoans(5);
            assertFalse(member.canBorrow(), "Failed on: loan limit reached");

            // Reset loans, now suspend
            member.setActiveLoans(0);
            member.setStatus(MemberStatus.SUSPENDED);
            assertFalse(member.canBorrow(), "Failed on: suspended status");
        }
    }

    // =========================================================================
    // incrementActiveLoans()
    // =========================================================================

    @Nested
    @DisplayName("incrementActiveLoans()")
    class IncrementActiveLoansTests {

        @Test
        @DisplayName("Should increase active loans by one")
        void shouldIncrementByOne() {
            int loansBefore = member.getActiveLoans(); // 0

            member.incrementActiveLoans();

            assertEquals(loansBefore + 1, member.getActiveLoans());
        }

        @Test
        @DisplayName("Should throw BusinessRuleException when member cannot borrow")
        void shouldThrowWhenCannotBorrow() {
            // Fill up to max loans first
            member.setActiveLoans(5);

            assertThrows(BusinessRuleException.class,
                    () -> member.incrementActiveLoans(),
                    "Incrementing beyond max loans should be rejected");
        }

        @Test
        @DisplayName("Should throw when suspended even if under loan limit")
        void shouldThrowWhenSuspendedEvenWithCapacity() {
            member.setStatus(MemberStatus.SUSPENDED);

            assertThrows(BusinessRuleException.class,
                    () -> member.incrementActiveLoans(),
                    "canBorrow() check in increment should catch suspended status");
        }
    }

    // =========================================================================
    // decrementActiveLoans()
    // =========================================================================

    @Nested
    @DisplayName("decrementActiveLoans()")
    class DecrementActiveLoansTests {

        @Test
        @DisplayName("Should decrease active loans by one")
        void shouldDecrementByOne() {
            member.setActiveLoans(2);

            member.decrementActiveLoans();

            assertEquals(1, member.getActiveLoans());
        }

        @Test
        @DisplayName("Should allow decrement down to zero")
        void shouldDecrementToZero() {
            member.setActiveLoans(1);

            member.decrementActiveLoans();

            assertEquals(0, member.getActiveLoans());
        }

        @Test
        @DisplayName("Should throw BusinessRuleException when active loans are already zero")
        void shouldThrowWhenAlreadyZero() {
            // Default is 0 active loans
            BusinessRuleException exception = assertThrows(BusinessRuleException.class,
                    () -> member.decrementActiveLoans());

            assertTrue(exception.getMessage().contains(member.getFullName()),
                    "Error message should identify the member");
        }
    }

    // =========================================================================
    // renewMembership()
    // =========================================================================

    @Nested
    @DisplayName("renewMembership()")
    class RenewMembershipTests {

        @Test
        @DisplayName("Should extend expiration from current expiration date when not expired")
        void shouldExtendFromCurrentExpirationWhenNotExpired() {
            // Member is not expired — expiration is 1 year ahead
            LocalDate currentExpiration = member.getExpirationDate(); // today + 1 year

            member.renewMembership(1); // renew for 1 more year

            assertEquals(currentExpiration.plusYears(1), member.getExpirationDate(),
                    "When not expired, renewal should stack on top of existing expiration");
        }

        @Test
        @DisplayName("Should extend expiration from today when membership is already expired")
        void shouldExtendFromTodayWhenExpired() {
            member.setExpirationDate(LocalDate.now().minusDays(30)); // expired 30 days ago

            member.renewMembership(1);

            assertEquals(LocalDate.now().plusYears(1), member.getExpirationDate(),
                    "When expired, renewal should start fresh from today, not stack on old date");
        }

        @Test
        @DisplayName("Should support multi-year renewals")
        void shouldSupportMultiYearRenewals() {
            LocalDate currentExpiration = member.getExpirationDate();

            member.renewMembership(3);

            assertEquals(currentExpiration.plusYears(3), member.getExpirationDate());
        }

        @Test
        @DisplayName("Should reactivate EXPIRED status to ACTIVE on renewal")
        void shouldReactivateExpiredStatus() {
            member.setStatus(MemberStatus.EXPIRED);
            member.setExpirationDate(LocalDate.now().minusDays(1));

            member.renewMembership(1);

            assertEquals(MemberStatus.ACTIVE, member.getStatus(),
                    "Renewal should automatically reactivate an EXPIRED member");
        }

        @Test
        @DisplayName("Should not change status if member is SUSPENDED (not EXPIRED)")
        void shouldNotChangeStatusIfSuspended() {
            member.setStatus(MemberStatus.SUSPENDED);

            member.renewMembership(1);

            assertEquals(MemberStatus.SUSPENDED, member.getStatus(),
                    "Renewal should only reactivate EXPIRED members — " +
                    "suspended members remain suspended (admin must explicitly activate)");
        }
    }

    // =========================================================================
    // equals() and hashCode()
    // =========================================================================

    @Nested
    @DisplayName("Equals and HashCode")
    class EqualsHashCodeTests {

        @Test
        @DisplayName("Two members with same key fields should be equal when IDs match")
        void shouldBeEqualWithSameIdAndFields() {
            Member m1 = new Member("MEM-001", "Luis", "García", "luis@example.com");
            Member m2 = new Member("MEM-001", "Luis", "García", "luis@example.com");
            TestEntityHelper.setId(m1, 1L);
            TestEntityHelper.setId(m2, 1L);

            assertEquals(m1, m2);
            assertEquals(m1.hashCode(), m2.hashCode());
        }

        @Test
        @DisplayName("Two members with different emails should not be equal")
        void shouldNotBeEqualWithDifferentEmail() {
            Member m1 = new Member("MEM-001", "Luis", "García", "luis@example.com");
            Member m2 = new Member("MEM-001", "Luis", "García", "other@example.com");
            TestEntityHelper.setId(m1, 1L);
            TestEntityHelper.setId(m2, 1L);

            assertNotEquals(m1, m2);
        }
    }
}
