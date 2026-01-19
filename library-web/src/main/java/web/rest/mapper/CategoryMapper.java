package web.rest.mapper;

import catalog.dto.CategoryDTO;
import jakarta.enterprise.context.ApplicationScoped;
import web.rest.dto.CategoryCreateRequest;
import web.rest.dto.CategoryUpdateRequest;

@ApplicationScoped
public class CategoryMapper {

    public CategoryDTO toDto(CategoryCreateRequest request) {
        return CategoryDTO.builder()
                .name(request.getName())
                .description(request.getDescription())
                .build();
    }

    public CategoryDTO toDto(Long id, CategoryUpdateRequest request) {
        return CategoryDTO.builder()
                .id(id)
                .name(request.getName())
                .description(request.getDescription())
                .build();
    }
}
