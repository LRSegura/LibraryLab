package catalog.usecase;

import catalog.dto.BookDTO;
import catalog.model.Book;
import catalog.model.Category;
import catalog.port.BookRepository;
import catalog.port.CategoryRepository;
import common.TestServiceHelper;
import common.exception.BusinessRuleException;
import common.exception.DuplicateEntityException;
import common.exception.EntityNotFoundException;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for BookService.
 *
 * These tests verify the orchestration logic of BookService — how it coordinates
 * BookRepository and CategoryRepository to enforce business rules, without
 * touching JPA, CDI, or the database.
 *
 * All repositories and the Bean Validation Validator are mocked. BaseService.validator
 * is injected manually via TestServiceHelper because Mockito's constructor injection
 * does not reach inherited superclass fields.
 *
 * Tested methods:
 *   - create(): ISBN duplicate check, category lookup, save
 *   - update(): not-found check, ISBN collision across entities, category assignment
 *   - delete(): not-found check, active-loan guard
 *   - updateCopies(): not-found check, loaned-count floor, copy adjustment
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BookService")
class BookServiceTest {

    @Mock
    private BookRepository bookRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private Validator validator;

    @InjectMocks
    private BookService bookService;

    @BeforeEach
    void injectValidator() {
        // Mockito builds BookService via its @Inject constructor (bookRepository, categoryRepository)
        // and does not subsequently inject into the inherited BaseService.validator field.
        TestServiceHelper.injectValidator(bookService, validator);
    }

    // =========================================================================
    // create()
    // =========================================================================

    @Nested
    @DisplayName("create()")
    class CreateTests {

        @Test
        @DisplayName("Should throw DuplicateEntityException and never save when ISBN already exists")
        void shouldThrowWhenIsbnAlreadyExists() {
            when(bookRepository.existsByIsbn("978-0-13-468599-1")).thenReturn(true);

            BookDTO dto = BookDTO.builder()
                    .isbn("978-0-13-468599-1")
                    .title("Effective Java")
                    .author("Joshua Bloch")
                    .totalCopies(1)
                    .build();

            assertThrows(DuplicateEntityException.class, () -> bookService.create(dto));

            // A duplicate ISBN must never reach the database
            verify(bookRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw EntityNotFoundException and never save when category ID is not found")
        void shouldThrowWhenCategoryNotFound() {
            when(bookRepository.existsByIsbn(any())).thenReturn(false);
            when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

            BookDTO dto = BookDTO.builder()
                    .isbn("978-0-13-468599-1")
                    .title("Effective Java")
                    .author("Joshua Bloch")
                    .totalCopies(1)
                    .categoryId(99L)
                    .build();

            assertThrows(EntityNotFoundException.class, () -> bookService.create(dto));
            verify(bookRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should save book and return DTO when ISBN is unique and no category is given")
        void shouldSaveBookWithoutCategory() {
            when(bookRepository.existsByIsbn(any())).thenReturn(false);

            BookDTO dto = BookDTO.builder()
                    .isbn("978-0-13-468599-1")
                    .title("Effective Java")
                    .author("Joshua Bloch")
                    .totalCopies(1)
                    .build();

            BookDTO result = bookService.create(dto);

            assertAll("Created book DTO",
                    () -> assertNotNull(result),
                    () -> assertEquals("978-0-13-468599-1", result.getIsbn()),
                    () -> assertEquals("Effective Java", result.getTitle()),
                    () -> assertNull(result.getCategoryId(), "No category should be assigned")
            );
            verify(bookRepository).save(any(Book.class));
        }

        @Test
        @DisplayName("Should assign category and return full DTO when a valid category ID is provided")
        void shouldSaveBookWithCategory() {
            Category category = new Category("Technology", "Tech books");
            TestServiceHelper.setEntityId(category, 1L);

            when(bookRepository.existsByIsbn(any())).thenReturn(false);
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

            BookDTO dto = BookDTO.builder()
                    .isbn("978-0-13-468599-1")
                    .title("Effective Java")
                    .author("Joshua Bloch")
                    .totalCopies(1)
                    .categoryId(1L)
                    .build();

            BookDTO result = bookService.create(dto);

            assertAll("Created book with category",
                    () -> assertNotNull(result),
                    () -> assertEquals(1L, result.getCategoryId()),
                    () -> assertEquals("Technology", result.getCategoryName())
            );
            verify(bookRepository).save(any(Book.class));
        }
    }

    // =========================================================================
    // update()
    // =========================================================================

    @Nested
    @DisplayName("update()")
    class UpdateTests {

        private Book existingBook;

        @BeforeEach
        void setUp() {
            existingBook = new Book("978-0-13-468599-1", "Effective Java", "Joshua Bloch");
            TestServiceHelper.setEntityId(existingBook, 1L);
        }

        @Test
        @DisplayName("Should throw EntityNotFoundException when book ID does not exist")
        void shouldThrowWhenBookNotFound() {
            when(bookRepository.findById(1L)).thenReturn(Optional.empty());

            BookDTO dto = BookDTO.builder().id(1L).isbn("978-0-13-468599-1")
                    .title("EJ").author("Bloch").totalCopies(1).build();

            assertThrows(EntityNotFoundException.class, () -> bookService.update(dto));
        }

        @Test
        @DisplayName("Should throw DuplicateEntityException when ISBN is already held by a different book")
        void shouldThrowWhenIsbnTakenByDifferentBook() {
            // A second book already registered with the same ISBN
            Book bookWithSameIsbn = new Book("978-0-13-468599-1", "Another Book", "Author");
            TestServiceHelper.setEntityId(bookWithSameIsbn, 2L); // different ID → conflict

            when(bookRepository.findById(1L)).thenReturn(Optional.of(existingBook));
            when(bookRepository.findByIsbn("978-0-13-468599-1")).thenReturn(Optional.of(bookWithSameIsbn));

            BookDTO dto = BookDTO.builder().id(1L).isbn("978-0-13-468599-1")
                    .title("EJ Updated").author("Bloch").totalCopies(1).build();

            assertThrows(DuplicateEntityException.class, () -> bookService.update(dto));
        }

        @Test
        @DisplayName("Should update successfully when the ISBN belongs to the same book (no collision)")
        void shouldUpdateWhenIsbnBelongsToSameBook() {
            // ISBN lookup returns the exact entity being updated — not a conflict
            when(bookRepository.findById(1L)).thenReturn(Optional.of(existingBook));
            when(bookRepository.findByIsbn("978-0-13-468599-1")).thenReturn(Optional.of(existingBook));

            BookDTO dto = BookDTO.builder()
                    .id(1L)
                    .isbn("978-0-13-468599-1")
                    .title("Effective Java 3rd Ed")
                    .author("Joshua Bloch")
                    .totalCopies(2)
                    .build();

            BookDTO result = bookService.update(dto);

            assertAll("Updated book",
                    () -> assertNotNull(result),
                    () -> assertEquals("Effective Java 3rd Ed", result.getTitle()),
                    () -> assertEquals(2, result.getTotalCopies())
            );
            verify(bookRepository).update(existingBook);
        }

        @Test
        @DisplayName("Should throw EntityNotFoundException when the new category ID does not exist")
        void shouldThrowWhenCategoryNotFound() {
            when(bookRepository.findById(1L)).thenReturn(Optional.of(existingBook));
            when(bookRepository.findByIsbn(any())).thenReturn(Optional.empty());
            when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

            BookDTO dto = BookDTO.builder().id(1L).isbn("978-0-13-468599-1")
                    .title("EJ").author("Bloch").totalCopies(1).categoryId(99L).build();

            assertThrows(EntityNotFoundException.class, () -> bookService.update(dto));
        }
    }

    // =========================================================================
    // delete()
    // =========================================================================

    @Nested
    @DisplayName("delete()")
    class DeleteTests {

        @Test
        @DisplayName("Should throw EntityNotFoundException when book ID does not exist")
        void shouldThrowWhenBookNotFound() {
            when(bookRepository.findById(1L)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class, () -> bookService.delete(1L));
        }

        @Test
        @DisplayName("Should throw BusinessRuleException and never delete when book has active loans")
        void shouldThrowWhenBookHasActiveLoans() {
            // availableCopies < totalCopies means at least one copy is currently on loan
            Book book = new Book("978-0-13-468599-1", "Effective Java", "Joshua Bloch");
            book.setTotalCopies(3);
            book.setAvailableCopies(2); // 1 copy out on loan

            when(bookRepository.findById(1L)).thenReturn(Optional.of(book));

            assertThrows(BusinessRuleException.class, () -> bookService.delete(1L));
            verify(bookRepository, never()).delete(any());
        }

        @Test
        @DisplayName("Should delete book when all copies are available (no active loans)")
        void shouldDeleteBookWhenNoActiveLoans() {
            Book book = new Book("978-0-13-468599-1", "Effective Java", "Joshua Bloch");
            book.setTotalCopies(3);
            book.setAvailableCopies(3); // all copies in stock

            when(bookRepository.findById(1L)).thenReturn(Optional.of(book));

            assertDoesNotThrow(() -> bookService.delete(1L));
            verify(bookRepository).delete(book);
        }
    }

    // =========================================================================
    // updateCopies()
    // =========================================================================

    @Nested
    @DisplayName("updateCopies()")
    class UpdateCopiesTests {

        @Test
        @DisplayName("Should throw EntityNotFoundException when book ID does not exist")
        void shouldThrowWhenBookNotFound() {
            when(bookRepository.findById(1L)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class, () -> bookService.updateCopies(1L, 5));
        }

        @Test
        @DisplayName("Should throw BusinessRuleException when new total is below the loaned-out count")
        void shouldThrowWhenTotalBelowLoanedCount() {
            // 3 total, 1 available → 2 on loan; cannot reduce total to 1 (below the 2 loaned)
            Book book = new Book("978-0-13-468599-1", "Effective Java", "Joshua Bloch");
            book.setTotalCopies(3);
            book.setAvailableCopies(1);

            when(bookRepository.findById(1L)).thenReturn(Optional.of(book));

            assertThrows(BusinessRuleException.class, () -> bookService.updateCopies(1L, 1));
        }

        @Test
        @DisplayName("Should increase available copies by the same delta as the total increase")
        void shouldAdjustAvailableCopiesCorrectly() {
            // 3 total, 2 available → 1 on loan; adding 2 total copies → 4 available
            Book book = new Book("978-0-13-468599-1", "Effective Java", "Joshua Bloch");
            book.setTotalCopies(3);
            book.setAvailableCopies(2);

            when(bookRepository.findById(1L)).thenReturn(Optional.of(book));

            bookService.updateCopies(1L, 5);

            assertAll("Updated copy counts",
                    () -> assertEquals(5, book.getTotalCopies()),
                    () -> assertEquals(4, book.getAvailableCopies(),
                            "Available copies must increase by the same delta to preserve the loaned-out count")
            );
            verify(bookRepository).update(book);
        }
    }
}
