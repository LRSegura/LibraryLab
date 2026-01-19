package web.rest;

import catalog.dto.CategoryDTO;
import catalog.usecase.CategoryService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import web.rest.dto.CategoryCreateRequest;
import web.rest.dto.CategoryUpdateRequest;
import web.rest.mapper.CategoryMapper;

import java.util.List;

@Path("/categories")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CategoryResource {

    private CategoryService categoryService;
    private CategoryMapper categoryMapper;

    public CategoryResource() {
    }

    @Inject
    public CategoryResource(CategoryService categoryService, CategoryMapper categoryMapper) {
        this.categoryService = categoryService;
        this.categoryMapper = categoryMapper;
    }

    @GET
    public List<CategoryDTO> findAll() {
        return categoryService.findAll();
    }

    @GET
    @Path("/{id}")
    public Response findById(@PathParam("id") Long id) {
        return categoryService.findById(id)
                .map(Response::ok)
                .orElse(Response.status(Response.Status.NOT_FOUND))
                .build();
    }

    @GET
    @Path("/name/{name}")
    public Response findByName(@PathParam("name") String name) {
        return categoryService.findByName(name)
                .map(Response::ok)
                .orElse(Response.status(Response.Status.NOT_FOUND))
                .build();
    }

    @POST
    public Response create(@Valid CategoryCreateRequest request) {
        CategoryDTO dto = categoryMapper.toDto(request);
        CategoryDTO created = categoryService.create(dto);
        return Response.status(Response.Status.CREATED).entity(created).build();
    }

    @PUT
    @Path("/{id}")
    public Response update(@PathParam("id") Long id, @Valid CategoryUpdateRequest request) {
        CategoryDTO dto = categoryMapper.toDto(id, request);
        CategoryDTO updated = categoryService.update(dto);
        return Response.ok(updated).build();
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") Long id) {
        categoryService.delete(id);
        return Response.noContent().build();
    }
}
