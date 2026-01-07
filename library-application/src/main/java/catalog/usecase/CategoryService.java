package catalog.usecase;

import catalog.dto.CategoryDTO;
import catalog.model.Category;
import catalog.port.CategoryRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class CategoryService {

    @Inject
    private CategoryRepository categoryRepository;

    public List<CategoryDTO> findAll() {
        return categoryRepository.findAll().stream()
                .map(CategoryDTO::fromEntity)
                .toList();
    }

    public Optional<CategoryDTO> findById(Long id) {
        return categoryRepository.findById(id)
                .map(CategoryDTO::fromEntity);
    }

    public Optional<CategoryDTO> findByName(String name) {
        return categoryRepository.findByName(name)
                .map(CategoryDTO::fromEntity);
    }

    @Transactional
    public CategoryDTO create(CategoryDTO dto) {
        if (categoryRepository.existsByName(dto.getName())) {
            throw new IllegalArgumentException("Category with name '" + dto.getName() + "' already exists");
        }
        Category category = dto.toEntity();
        categoryRepository.save(category);
        return CategoryDTO.fromEntity(category);
    }

    @Transactional
    public CategoryDTO update(Long id, CategoryDTO dto) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Category not found with id: " + id));

        Optional<Category> existingByName = categoryRepository.findByName(dto.getName());
        if (existingByName.isPresent() && !existingByName.get().getId().equals(id)) {
            throw new IllegalArgumentException("Category with name '" + dto.getName() + "' already exists");
        }

        dto.updateEntity(category);
        categoryRepository.update(category);
        return CategoryDTO.fromEntity(category);
    }

    @Transactional
    public void delete(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Category not found with id: " + id));

        if (category.getBooks() != null && !category.getBooks().isEmpty()) {
            throw new IllegalStateException("Cannot delete category with associated books");
        }

        categoryRepository.delete(category);
    }
}
