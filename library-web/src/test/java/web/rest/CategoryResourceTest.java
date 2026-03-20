package web.rest;

import catalog.dto.CategoryDTO;
import catalog.usecase.CategoryService;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import web.rest.dto.CategoryCreateRequest;
import web.rest.dto.CategoryUpdateRequest;
import web.rest.mapper.CategoryMapper;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CategoryResource.
 *
 * Verifies HTTP status codes, response bodies, and correct delegation to the
 * service layer. No container or database needed.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CategoryResource")
class CategoryResourceTest {

    @Mock
    private CategoryService categoryService;

    @Mock
    private CategoryMapper categoryMapper;

    @InjectMocks
    private CategoryResource categoryResource;

    private static CategoryDTO aCategory() {
        return CategoryDTO.builder()
                .id(1L)
                .name("Science Fiction")
                .description("Sci-fi books")
                .build();
    }

    @Nested
    @DisplayName("findAll")
    class FindAllTests {

        @Test
        @DisplayName("Should delegate to service and return all categories")
        void shouldReturnAllCategories() {
            List<CategoryDTO> categories = List.of(aCategory());
            when(categoryService.findAll()).thenReturn(categories);

            List<CategoryDTO> result = categoryResource.findAll();

            assertSame(categories, result);
            verify(categoryService).findAll();
        }
    }

    @Nested
    @DisplayName("findById")
    class FindByIdTests {

        @Test
        @DisplayName("Should return 200 OK with category when found")
        void shouldReturn200WhenFound() {
            CategoryDTO category = aCategory();
            when(categoryService.findById(1L)).thenReturn(Optional.of(category));

            Response response = categoryResource.findById(1L);

            assertAll(
                    () -> assertEquals(200, response.getStatus()),
                    () -> assertSame(category, response.getEntity())
            );
        }

        @Test
        @DisplayName("Should return 404 Not Found when category does not exist")
        void shouldReturn404WhenNotFound() {
            when(categoryService.findById(99L)).thenReturn(Optional.empty());

            Response response = categoryResource.findById(99L);

            assertEquals(404, response.getStatus());
        }
    }

    @Nested
    @DisplayName("findByName")
    class FindByNameTests {

        @Test
        @DisplayName("Should return 200 OK with category when name is found")
        void shouldReturn200WhenFound() {
            CategoryDTO category = aCategory();
            when(categoryService.findByName("Science Fiction")).thenReturn(Optional.of(category));

            Response response = categoryResource.findByName("Science Fiction");

            assertAll(
                    () -> assertEquals(200, response.getStatus()),
                    () -> assertSame(category, response.getEntity())
            );
        }

        @Test
        @DisplayName("Should return 404 Not Found when name is not found")
        void shouldReturn404WhenNotFound() {
            when(categoryService.findByName("Unknown")).thenReturn(Optional.empty());

            Response response = categoryResource.findByName("Unknown");

            assertEquals(404, response.getStatus());
        }
    }

    @Nested
    @DisplayName("create")
    class CreateTests {

        @Test
        @DisplayName("Should return 201 Created with the saved category in the body")
        void shouldReturn201WithCreatedCategory() {
            CategoryCreateRequest request = new CategoryCreateRequest();
            CategoryDTO mappedDto = CategoryDTO.builder().name("Science Fiction").build();
            CategoryDTO createdDto = aCategory();

            when(categoryMapper.toDto(request)).thenReturn(mappedDto);
            when(categoryService.create(mappedDto)).thenReturn(createdDto);

            Response response = categoryResource.create(request);

            assertAll(
                    () -> assertEquals(201, response.getStatus()),
                    () -> assertSame(createdDto, response.getEntity())
            );
            verify(categoryService).create(mappedDto);
        }
    }

    @Nested
    @DisplayName("update")
    class UpdateTests {

        @Test
        @DisplayName("Should return 200 OK with the updated category in the body")
        void shouldReturn200WithUpdatedCategory() {
            CategoryUpdateRequest request = new CategoryUpdateRequest();
            CategoryDTO mappedDto = CategoryDTO.builder().id(1L).name("Science Fiction").build();
            CategoryDTO updatedDto = aCategory();

            when(categoryMapper.toDto(1L, request)).thenReturn(mappedDto);
            when(categoryService.update(mappedDto)).thenReturn(updatedDto);

            Response response = categoryResource.update(1L, request);

            assertAll(
                    () -> assertEquals(200, response.getStatus()),
                    () -> assertSame(updatedDto, response.getEntity())
            );
        }
    }

    @Nested
    @DisplayName("delete")
    class DeleteTests {

        @Test
        @DisplayName("Should return 204 No Content and delegate to service")
        void shouldReturn204() {
            Response response = categoryResource.delete(1L);

            assertEquals(204, response.getStatus());
            verify(categoryService).delete(1L);
        }
    }
}
