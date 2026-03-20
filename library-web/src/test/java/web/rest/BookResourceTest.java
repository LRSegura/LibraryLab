package web.rest;

import catalog.dto.BookDTO;
import catalog.usecase.BookService;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import web.rest.dto.ApiResponse;
import web.rest.dto.BookCreateRequest;
import web.rest.dto.BookUpdateRequest;
import web.rest.mapper.BookMapper;

import java.net.URI;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for BookResource.
 *
 * These tests verify the HTTP contract: correct status codes, response bodies,
 * and service delegation. Services and mappers are mocked — no container needed.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BookResource")
class BookResourceTest {

    @Mock
    private BookService bookService;

    @Mock
    private BookMapper bookMapper;

    @Mock
    private UriInfo uriInfo;

    @Mock
    private UriBuilder uriBuilder;

    @InjectMocks
    private BookResource bookResource;

    // Shared fixture
    private static BookDTO aBook() {
        return BookDTO.builder()
                .id(1L)
                .isbn("978-0-13-468599-1")
                .title("Clean Code")
                .author("Robert C. Martin")
                .build();
    }

    @Nested
    @DisplayName("findAll")
    class FindAllTests {

        @Test
        @DisplayName("Should delegate to service and wrap result in ApiResponse")
        void shouldReturnApiResponseWithAllBooks() {
            List<BookDTO> books = List.of(aBook());
            when(bookService.findAll()).thenReturn(books);

            ApiResponse<List<BookDTO>> response = bookResource.findAll();

            assertSame(books, response.data());
            verify(bookService).findAll();
        }
    }

    @Nested
    @DisplayName("findById")
    class FindByIdTests {

        @Test
        @DisplayName("Should return 200 OK with book when found")
        void shouldReturn200WhenFound() {
            BookDTO book = aBook();
            when(bookService.findById(1L)).thenReturn(Optional.of(book));

            Response response = bookResource.findById(1L);

            assertAll(
                    () -> assertEquals(200, response.getStatus()),
                    () -> assertSame(book, response.getEntity())
            );
        }

        @Test
        @DisplayName("Should return 404 Not Found when book does not exist")
        void shouldReturn404WhenNotFound() {
            when(bookService.findById(99L)).thenReturn(Optional.empty());

            Response response = bookResource.findById(99L);

            assertEquals(404, response.getStatus());
        }
    }

    @Nested
    @DisplayName("findByIsbn")
    class FindByIsbnTests {

        @Test
        @DisplayName("Should return 200 OK with book when ISBN is found")
        void shouldReturn200WhenFound() {
            BookDTO book = aBook();
            when(bookService.findByIsbn("978-0-13-468599-1")).thenReturn(Optional.of(book));

            Response response = bookResource.findByIsbn("978-0-13-468599-1");

            assertAll(
                    () -> assertEquals(200, response.getStatus()),
                    () -> assertSame(book, response.getEntity())
            );
        }

        @Test
        @DisplayName("Should return 404 Not Found when ISBN is not found")
        void shouldReturn404WhenNotFound() {
            when(bookService.findByIsbn("000-0-00-000000-0")).thenReturn(Optional.empty());

            Response response = bookResource.findByIsbn("000-0-00-000000-0");

            assertEquals(404, response.getStatus());
        }
    }

    @Nested
    @DisplayName("create")
    class CreateTests {

        @Test
        @DisplayName("Should return 201 Created with the saved book in the body")
        void shouldReturn201WithCreatedBook() {
            BookCreateRequest request = new BookCreateRequest();
            BookDTO mappedDto = BookDTO.builder().isbn("978-0-13-468599-1").title("Clean Code").author("Martin").build();
            BookDTO createdDto = aBook(); // has id = 1

            when(bookMapper.toDto(request)).thenReturn(mappedDto);
            when(bookService.create(mappedDto)).thenReturn(createdDto);
            // Stub UriInfo chain: getAbsolutePathBuilder().path("1").build()
            when(uriInfo.getAbsolutePathBuilder()).thenReturn(uriBuilder);
            when(uriBuilder.path(anyString())).thenReturn(uriBuilder);
            when(uriBuilder.build()).thenReturn(URI.create("http://localhost/api/v1/books/1"));

            Response response = bookResource.create(request, uriInfo);

            assertAll(
                    () -> assertEquals(201, response.getStatus()),
                    () -> assertSame(createdDto, response.getEntity())
            );
            verify(bookService).create(mappedDto);
        }
    }

    @Nested
    @DisplayName("update")
    class UpdateTests {

        @Test
        @DisplayName("Should return 200 OK with the updated book in the body")
        void shouldReturn200WithUpdatedBook() {
            BookUpdateRequest request = new BookUpdateRequest();
            BookDTO mappedDto = BookDTO.builder().id(1L).isbn("978-0-13-468599-1").title("Clean Code").author("Martin").build();
            BookDTO updatedDto = aBook();

            when(bookMapper.toDto(1L, request)).thenReturn(mappedDto);
            when(bookService.update(mappedDto)).thenReturn(updatedDto);

            Response response = bookResource.update(1L, request);

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
            Response response = bookResource.delete(1L);

            assertEquals(204, response.getStatus());
            verify(bookService).delete(1L);
        }
    }

    @Nested
    @DisplayName("updateCopies")
    class UpdateCopiesTests {

        @Test
        @DisplayName("Should return 200 OK and delegate to service")
        void shouldReturn200() {
            Response response = bookResource.updateCopies(1L, 5);

            assertEquals(200, response.getStatus());
            verify(bookService).updateCopies(1L, 5);
        }
    }
}
