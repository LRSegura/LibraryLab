package web.rest;

import catalog.dto.BookDTO;
import catalog.usecase.BookService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import web.rest.dto.BookCreateRequest;
import web.rest.dto.BookUpdateRequest;
import web.rest.mapper.BookMapper;

import java.util.List;

@Path("/books")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class BookResource {

    private BookService bookService;
    private BookMapper bookMapper;

    public BookResource() {
    }

    @Inject
    public BookResource(BookService bookService, BookMapper bookMapper) {
        this.bookService = bookService;
        this.bookMapper = bookMapper;
    }

    @GET
    public List<BookDTO> findAll() {
        return bookService.findAll();
    }

    @GET
    @Path("/{id}")
    public Response findById(@PathParam("id") Long id) {
        return bookService.findById(id)
                .map(Response::ok)
                .orElse(Response.status(Response.Status.NOT_FOUND))
                .build();
    }

    @GET
    @Path("/isbn/{isbn}")
    public Response findByIsbn(@PathParam("isbn") String isbn) {
        return bookService.findByIsbn(isbn)
                .map(Response::ok)
                .orElse(Response.status(Response.Status.NOT_FOUND))
                .build();
    }

    @GET
    @Path("/search/title")
    public List<BookDTO> findByTitle(@QueryParam("q") String title) {
        return bookService.findByTitle(title);
    }

    @GET
    @Path("/search/author")
    public List<BookDTO> findByAuthor(@QueryParam("q") String author) {
        return bookService.findByAuthor(author);
    }

    @GET
    @Path("/available")
    public List<BookDTO> findAvailable() {
        return bookService.findAvailable();
    }

    @GET
    @Path("/category/{categoryId}")
    public List<BookDTO> findByCategory(@PathParam("categoryId") Long categoryId) {
        return bookService.findByCategory(categoryId);
    }

    @POST
    public Response create(@Valid BookCreateRequest request) {
        BookDTO dto = bookMapper.toDto(request);
        BookDTO created = bookService.create(dto);
        return Response.status(Response.Status.CREATED).entity(created).build();
    }

    @PUT
    @Path("/{id}")
    public Response update(@PathParam("id") Long id, @Valid BookUpdateRequest request) {
        BookDTO dto = bookMapper.toDto(id, request);
        BookDTO updated = bookService.update(dto);
        return Response.ok(updated).build();
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") Long id) {
        bookService.delete(id);
        return Response.noContent().build();
    }

    @PATCH
    @Path("/{id}/copies")
    public Response updateCopies(@PathParam("id") Long id, @QueryParam("total") int totalCopies) {
        bookService.updateCopies(id, totalCopies);
        return Response.ok().build();
    }
}
