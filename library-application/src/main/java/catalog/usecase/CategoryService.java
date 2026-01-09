package catalog.usecase;

import catalog.dto.CategoryDTO;
import catalog.model.Category;
import catalog.port.CategoryRepository;
import common.BaseService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class CategoryService extends BaseService<Category> {

    private CategoryRepository categoryRepository;

    public CategoryService() {
        //Required by proxy
    }

    @Inject
    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

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
        validateFieldsConstraint(category);
        categoryRepository.save(category);
        return CategoryDTO.fromEntity(category);
    }

    @Transactional
    public CategoryDTO update(CategoryDTO dto) {
        Category category = categoryRepository.findById(dto.getId())
                .orElseThrow(() -> new IllegalArgumentException("Category not found with id: " + dto.getId()));

        Optional<Category> existingByName = categoryRepository.findByName(dto.getName());
        if (existingByName.isPresent() && !existingByName.get().getId().equals(dto.getId())) {
            throw new IllegalArgumentException("Category with name '" + dto.getName() + "' already exists");
        }

        dto.updateEntity(category);
        validateFieldsConstraint(category);
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
