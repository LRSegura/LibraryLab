package catalog.model;

import common.TestEntityHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the Category domain entity.
 *
 * Category has less complex business logic than Book or Loan,
 * but the bidirectional relationship management (addBook/removeBook)
 * is critical to verify since it maintains referential consistency
 * that JPA relies on for correct persistence.
 *
 * Tested methods:
 *   - Constructor: initial state and defaults
 *   - addBook(): bidirectional relationship setup
 *   - removeBook(): bidirectional relationship teardown
 */
@DisplayName("Category Entity")
class CategoryTest {

    private Category category;

    @BeforeEach
    void setUp() {
        category = new Category("Programming", "Books about software development");
    }

    // =========================================================================
    // Constructor and Initial State
    // =========================================================================

    @Nested
    @DisplayName("Constructor and Defaults")
    class ConstructorTests {

        @Test
        @DisplayName("Should set name and description from constructor arguments")
        void shouldSetFieldsFromConstructor() {
            assertAll("Constructor fields",
                    () -> assertEquals("Programming", category.getName()),
                    () -> assertEquals("Books about software development", category.getDescription())
            );
        }

        @Test
        @DisplayName("Should initialize books list as empty (not null)")
        void shouldInitializeBooksListAsEmpty() {
            // This is important: a null list would cause NPE when checking category.getBooks().isEmpty()
            // in CategoryService.delete() — the guard clause depends on this being initialized
            assertNotNull(category.getBooks(), "Books list should never be null");
            assertTrue(category.getBooks().isEmpty(), "Books list should start empty");
        }
    }

    // =========================================================================
    // addBook()
    // =========================================================================

    @Nested
    @DisplayName("addBook()")
    class AddBookTests {

        @Test
        @DisplayName("Should add book to the category's book list")
        void shouldAddBookToList() {
            Book book = new Book("1234567890", "Clean Code", "Robert C. Martin");

            category.addBook(book);

            assertTrue(category.getBooks().contains(book),
                    "Category should contain the added book");
            assertEquals(1, category.getBooks().size());
        }

        @Test
        @DisplayName("Should set the book's category to this category (bidirectional sync)")
        void shouldSetBookCategoryBidirectionally() {
            Book book = new Book("1234567890", "Clean Code", "Robert C. Martin");

            category.addBook(book);

            // The owning side (Book.category) must also be set
            // for JPA to persist the relationship correctly
            assertSame(category, book.getCategory(),
                    "Book's category reference should point back to this category");
        }

        @Test
        @DisplayName("Should support adding multiple books")
        void shouldSupportMultipleBooks() {
            Book book1 = new Book("1234567890", "Clean Code", "Robert C. Martin");
            Book book2 = new Book("0987654321", "Refactoring", "Martin Fowler");

            category.addBook(book1);
            category.addBook(book2);

            assertEquals(2, category.getBooks().size());
        }
    }

    // =========================================================================
    // removeBook()
    // =========================================================================

    @Nested
    @DisplayName("removeBook()")
    class RemoveBookTests {

        @Test
        @DisplayName("Should remove book from the category's book list")
        void shouldRemoveBookFromList() {
            Book book = new Book("1234567890", "Clean Code", "Robert C. Martin");
            category.addBook(book);

            category.removeBook(book);

            assertFalse(category.getBooks().contains(book));
            assertTrue(category.getBooks().isEmpty());
        }

        @Test
        @DisplayName("Should null out the book's category reference (bidirectional sync)")
        void shouldNullBookCategoryBidirectionally() {
            Book book = new Book("1234567890", "Clean Code", "Robert C. Martin");
            category.addBook(book);

            category.removeBook(book);

            assertNull(book.getCategory(),
                    "Removed book should no longer reference the category");
        }

        @Test
        @DisplayName("Should only remove the specified book, leaving others intact")
        void shouldOnlyRemoveSpecifiedBook() {
            Book book1 = new Book("1234567890", "Clean Code", "Robert C. Martin");
            Book book2 = new Book("0987654321", "Refactoring", "Martin Fowler");
            category.addBook(book1);
            category.addBook(book2);

            category.removeBook(book1);

            assertEquals(1, category.getBooks().size());
            assertTrue(category.getBooks().contains(book2),
                    "Remaining book should still be in the list");
        }
    }

    // =========================================================================
    // equals() and hashCode()
    // =========================================================================

    @Nested
    @DisplayName("Equals and HashCode")
    class EqualsHashCodeTests {

        @Test
        @DisplayName("Two categories with same name and description should be equal when IDs match")
        void shouldBeEqualWithSameIdAndFields() {
            Category cat1 = new Category("Programming", "Software books");
            Category cat2 = new Category("Programming", "Software books");
            TestEntityHelper.setId(cat1, 1L);
            TestEntityHelper.setId(cat2, 1L);

            assertEquals(cat1, cat2);
            assertEquals(cat1.hashCode(), cat2.hashCode());
        }

        @Test
        @DisplayName("Two categories with different names should not be equal")
        void shouldNotBeEqualWithDifferentNames() {
            Category cat1 = new Category("Programming", "Software books");
            Category cat2 = new Category("Science", "Software books");
            TestEntityHelper.setId(cat1, 1L);
            TestEntityHelper.setId(cat2, 1L);

            assertNotEquals(cat1, cat2);
        }
    }
}
