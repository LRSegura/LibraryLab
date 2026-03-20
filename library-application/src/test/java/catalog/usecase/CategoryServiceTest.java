package catalog.usecase;

import catalog.dto.CategoryDTO;
import catalog.model.Book;
import catalog.model.Category;
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
 * Unit tests for CategoryService.
 *
 * CategoryService is the simplest service in the Catalog bounded context.
 * It manages a single repository and enforces two rules:
 *   1. Category names must be unique across the system.
 *   2. A category with associated books cannot be deleted (would orphan them).
 *
 * Tested methods:
 *   - create(): name duplicate check, save
 *   - update(): not-found check, name collision across entities
 *   - delete(): not-found check, books-exist guard
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CategoryService")
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private Validator validator;

    @InjectMocks
    private CategoryService categoryService;

    @BeforeEach
    void injectValidator() {
        TestServiceHelper.injectValidator(categoryService, validator);
    }

    // =========================================================================
    // create()
    // =========================================================================

    @Nested
    @DisplayName("create()")
    class CreateTests {

        @Test
        @DisplayName("Should throw DuplicateEntityException and never save when name already exists")
        void shouldThrowWhenNameAlreadyExists() {
            when(categoryRepository.existsByName("Technology")).thenReturn(true);

            CategoryDTO dto = CategoryDTO.builder()
                    .name("Technology")
                    .description("Tech books")
                    .build();

            assertThrows(DuplicateEntityException.class, () -> categoryService.create(dto));
            verify(categoryRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should save category and return DTO when name is unique")
        void shouldSaveCategoryWhenNameIsUnique() {
            when(categoryRepository.existsByName(any())).thenReturn(false);

            CategoryDTO dto = CategoryDTO.builder()
                    .name("Technology")
                    .description("Tech books")
                    .build();

            CategoryDTO result = categoryService.create(dto);

            assertAll("Created category DTO",
                    () -> assertNotNull(result),
                    () -> assertEquals("Technology", result.getName()),
                    () -> assertEquals("Tech books", result.getDescription())
            );
            verify(categoryRepository).save(any(Category.class));
        }
    }

    // =========================================================================
    // update()
    // =========================================================================

    @Nested
    @DisplayName("update()")
    class UpdateTests {

        private Category existingCategory;

        @BeforeEach
        void setUp() {
            existingCategory = new Category("Technology", "Tech books");
            TestServiceHelper.setEntityId(existingCategory, 1L);
        }

        @Test
        @DisplayName("Should throw EntityNotFoundException when category ID does not exist")
        void shouldThrowWhenCategoryNotFound() {
            when(categoryRepository.findById(1L)).thenReturn(Optional.empty());

            CategoryDTO dto = CategoryDTO.builder().id(1L).name("Tech").build();

            assertThrows(EntityNotFoundException.class, () -> categoryService.update(dto));
        }

        @Test
        @DisplayName("Should throw DuplicateEntityException when name is already held by a different category")
        void shouldThrowWhenNameTakenByDifferentCategory() {
            Category other = new Category("Technology", "Different description");
            TestServiceHelper.setEntityId(other, 2L); // different ID → conflict

            when(categoryRepository.findById(1L)).thenReturn(Optional.of(existingCategory));
            when(categoryRepository.findByName("Technology")).thenReturn(Optional.of(other));

            CategoryDTO dto = CategoryDTO.builder().id(1L).name("Technology").build();

            assertThrows(DuplicateEntityException.class, () -> categoryService.update(dto));
        }

        @Test
        @DisplayName("Should update successfully when the name belongs to the same category (no collision)")
        void shouldUpdateWhenNameBelongsToSameCategory() {
            // Name lookup returns the same entity — keeping the same name is allowed
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(existingCategory));
            when(categoryRepository.findByName("Technology")).thenReturn(Optional.of(existingCategory));

            CategoryDTO dto = CategoryDTO.builder()
                    .id(1L)
                    .name("Technology")
                    .description("Updated description")
                    .build();

            CategoryDTO result = categoryService.update(dto);

            assertAll("Updated category",
                    () -> assertNotNull(result),
                    () -> assertEquals("Technology", result.getName()),
                    () -> assertEquals("Updated description", result.getDescription())
            );
            verify(categoryRepository).update(existingCategory);
        }
    }

    // =========================================================================
    // delete()
    // =========================================================================

    @Nested
    @DisplayName("delete()")
    class DeleteTests {

        @Test
        @DisplayName("Should throw EntityNotFoundException when category ID does not exist")
        void shouldThrowWhenCategoryNotFound() {
            when(categoryRepository.findById(1L)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class, () -> categoryService.delete(1L));
        }

        @Test
        @DisplayName("Should throw BusinessRuleException and never delete when category has associated books")
        void shouldThrowWhenCategoryHasBooks() {
            Category category = new Category("Technology", "Tech books");
            // addBook() populates the books list and sets book.category = this
            category.addBook(new Book("978-0-13-468599-1", "Effective Java", "Joshua Bloch"));

            when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

            assertThrows(BusinessRuleException.class, () -> categoryService.delete(1L),
                    "Deleting a category with books would orphan those books");
            verify(categoryRepository, never()).delete(any());
        }

        @Test
        @DisplayName("Should delete category when no books are associated")
        void shouldDeleteCategoryWhenNoBooksAssociated() {
            // Category initialized with an empty books list by default
            Category category = new Category("Technology", "Tech books");

            when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

            assertDoesNotThrow(() -> categoryService.delete(1L));
            verify(categoryRepository).delete(category);
        }
    }
}
