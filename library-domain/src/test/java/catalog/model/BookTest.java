package catalog.model;

import common.TestEntityHelper;
import common.exception.BusinessRuleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the Book domain entity.
 *
 * These tests verify the business rules enforced by Book,
 * independent of JPA, CDI, or any framework infrastructure.
 *
 * Tested methods:
 *   - Constructor: initial state and defaults
 *   - isAvailable(): availability logic based on copies and status
 *   - borrowCopy(): decrement logic and guard conditions
 *   - returnCopy(): increment logic and guard conditions
 */
@DisplayName("Book Entity")
class BookTest {

    private Book book;

    @BeforeEach
    void setUp() {
        // Arrange: a standard book with known initial state
        book = new Book("978-0-13-468599-1", "Effective Java", "Joshua Bloch");
        book.setTotalCopies(3);
        book.setAvailableCopies(3);
        book.setStatus(BookStatus.AVAILABLE);
    }

    // =========================================================================
    // Constructor and Initial State
    // =========================================================================

    @Nested
    @DisplayName("Constructor and Defaults")
    class ConstructorTests {

        @Test
        @DisplayName("Should set isbn, title, and author from constructor arguments")
        void shouldSetFieldsFromConstructor() {
            Book newBook = new Book("1234567890", "Clean Code", "Robert C. Martin");

            assertAll("Constructor fields",
                    () -> assertEquals("1234567890", newBook.getIsbn()),
                    () -> assertEquals("Clean Code", newBook.getTitle()),
                    () -> assertEquals("Robert C. Martin", newBook.getAuthor())
            );
        }

        @Test
        @DisplayName("Should default to 1 total copy, 1 available copy, and AVAILABLE status")
        void shouldHaveCorrectDefaults() {
            Book newBook = new Book("1234567890", "Clean Code", "Robert C. Martin");

            assertAll("Default values",
                    () -> assertEquals(1, newBook.getTotalCopies()),
                    () -> assertEquals(1, newBook.getAvailableCopies()),
                    () -> assertEquals(BookStatus.AVAILABLE, newBook.getStatus())
            );
        }

        @Test
        @DisplayName("Should have null optional fields by default")
        void shouldHaveNullOptionalFields() {
            Book newBook = new Book("1234567890", "Clean Code", "Robert C. Martin");

            assertAll("Optional fields",
                    () -> assertNull(newBook.getPublisher()),
                    () -> assertNull(newBook.getPublicationDate()),
                    () -> assertNull(newBook.getCategory())
            );
        }
    }

    // =========================================================================
    // isAvailable()
    // =========================================================================

    @Nested
    @DisplayName("isAvailable()")
    class IsAvailableTests {

        @Test
        @DisplayName("Should return true when copies are available and status is AVAILABLE")
        void shouldBeAvailableWhenCopiesExistAndStatusAvailable() {
            // Book set up in @BeforeEach: 3 available copies, AVAILABLE status
            assertTrue(book.isAvailable());
        }

        @Test
        @DisplayName("Should return false when no copies are available")
        void shouldNotBeAvailableWhenNoCopies() {
            book.setAvailableCopies(0);

            assertFalse(book.isAvailable());
        }

        @Test
        @DisplayName("Should return false when status is UNAVAILABLE even if copies exist")
        void shouldNotBeAvailableWhenStatusUnavailable() {
            book.setStatus(BookStatus.UNAVAILABLE);

            // Copies exist, but the status overrides availability
            assertFalse(book.isAvailable());
        }

        @Test
        @DisplayName("Should return false when status is DISCONTINUED")
        void shouldNotBeAvailableWhenDiscontinued() {
            book.setStatus(BookStatus.DISCONTINUED);

            assertFalse(book.isAvailable());
        }

        @Test
        @DisplayName("Should return false when status is RESERVED")
        void shouldNotBeAvailableWhenReserved() {
            book.setStatus(BookStatus.RESERVED);

            assertFalse(book.isAvailable());
        }
    }

    // =========================================================================
    // borrowCopy()
    // =========================================================================

    @Nested
    @DisplayName("borrowCopy()")
    class BorrowCopyTests {

        @Test
        @DisplayName("Should decrement available copies by one on successful borrow")
        void shouldDecrementAvailableCopies() {
            int copiesBefore = book.getAvailableCopies(); // 3

            book.borrowCopy();

            assertEquals(copiesBefore - 1, book.getAvailableCopies(),
                    "Available copies should decrease by exactly 1");
        }

        @Test
        @DisplayName("Should allow borrowing until zero copies remain")
        void shouldAllowBorrowingDownToZero() {
            // Borrow all 3 copies one by one
            book.borrowCopy();
            book.borrowCopy();
            book.borrowCopy();

            assertEquals(0, book.getAvailableCopies());
        }

        @Test
        @DisplayName("Should not modify total copies when borrowing")
        void shouldNotChangeTotalCopies() {
            int totalBefore = book.getTotalCopies();

            book.borrowCopy();

            assertEquals(totalBefore, book.getTotalCopies(),
                    "Total copies must remain unchanged — only available copies decrease");
        }

        @Test
        @DisplayName("Should throw BusinessRuleException when no copies available")
        void shouldThrowWhenNoCopiesAvailable() {
            book.setAvailableCopies(0);

            BusinessRuleException exception = assertThrows(BusinessRuleException.class,
                    () -> book.borrowCopy());

            // Verify the exception message references the book title
            assertTrue(exception.getMessage().contains(book.getTitle()),
                    "Exception message should include the book title for debugging");
        }

        @Test
        @DisplayName("Should throw BusinessRuleException when status is not AVAILABLE")
        void shouldThrowWhenStatusNotAvailable() {
            book.setStatus(BookStatus.UNAVAILABLE);

            assertThrows(BusinessRuleException.class, () -> book.borrowCopy(),
                    "Borrowing should fail when book status prevents availability");
        }
    }

    // =========================================================================
    // returnCopy()
    // =========================================================================

    @Nested
    @DisplayName("returnCopy()")
    class ReturnCopyTests {

        @Test
        @DisplayName("Should increment available copies by one on return")
        void shouldIncrementAvailableCopies() {
            // First borrow one copy so we can return it
            book.borrowCopy(); // 3 → 2
            int copiesBefore = book.getAvailableCopies(); // 2

            book.returnCopy();

            assertEquals(copiesBefore + 1, book.getAvailableCopies(),
                    "Available copies should increase by exactly 1");
        }

        @Test
        @DisplayName("Should throw BusinessRuleException when all copies already available")
        void shouldThrowWhenAllCopiesAlreadyAvailable() {
            // All 3 copies are available — nothing to return
            BusinessRuleException exception = assertThrows(BusinessRuleException.class,
                    () -> book.returnCopy());

            assertTrue(exception.getMessage().contains(book.getTitle()),
                    "Exception message should include the book title");
        }

        @Test
        @DisplayName("Should allow return after borrow round-trip")
        void shouldRestoreAvailabilityAfterBorrowAndReturn() {
            int originalCopies = book.getAvailableCopies(); // 3

            book.borrowCopy(); // 3 → 2
            book.returnCopy(); // 2 → 3

            assertEquals(originalCopies, book.getAvailableCopies(),
                    "Borrow + return should be a no-op on available copies count");
        }
    }

    // =========================================================================
    // equals() and hashCode()
    // =========================================================================

    @Nested
    @DisplayName("Equals and HashCode")
    class EqualsHashCodeTests {

        @Test
        @DisplayName("Two books with same isbn, title, and author should be equal when IDs match")
        void shouldBeEqualWithSameIdAndFields() {
            Book book1 = new Book("978-0-13-468599-1", "Effective Java", "Joshua Bloch");
            Book book2 = new Book("978-0-13-468599-1", "Effective Java", "Joshua Bloch");
            TestEntityHelper.setId(book1, 1L);
            TestEntityHelper.setId(book2, 1L);

            assertEquals(book1, book2);
            assertEquals(book1.hashCode(), book2.hashCode());
        }

        @Test
        @DisplayName("Two books with different ISBNs should not be equal")
        void shouldNotBeEqualWithDifferentIsbn() {
            Book book1 = new Book("978-0-13-468599-1", "Effective Java", "Joshua Bloch");
            Book book2 = new Book("978-0-13-235088-4", "Effective Java", "Joshua Bloch");
            TestEntityHelper.setId(book1, 1L);
            TestEntityHelper.setId(book2, 1L);

            assertNotEquals(book1, book2);
        }
    }

    // =========================================================================
    // toString()
    // =========================================================================

    @Test
    @DisplayName("toString should include isbn, title, and author")
    void toStringShouldContainKeyFields() {
        String result = book.toString();

        assertAll("toString content",
                () -> assertTrue(result.contains(book.getIsbn())),
                () -> assertTrue(result.contains(book.getTitle())),
                () -> assertTrue(result.contains(book.getAuthor()))
        );
    }
}
