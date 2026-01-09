package web.bean;

import catalog.dto.CategoryDTO;
import catalog.usecase.CategoryService;
import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

@Named
@ViewScoped
@Getter
@Setter
public class CategoryBean extends BasicBean implements Serializable {

    private CategoryService categoryService;
    private List<CategoryDTO> categories;
    private CategoryDTO currentCategory;

    @Inject
    public CategoryBean(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    public CategoryBean(){
        //Required by proxy
    }

    @PostConstruct
    public void init() {
        loadCategories();
        initNewCategory();
    }

    public void loadCategories() {
        categories = categoryService.findAll();
    }

    public void initNewCategory() {
        currentCategory = new CategoryDTO();
    }

    public void save() {
        Runnable operation = () -> {
            categoryService.create(currentCategory);
            addInfoMessage(SummaryValues.SUCCESS.getDescription(), "Category created successfully");
            initNewCategory();
            loadCategories();
        };
        executeOperation(operation, "Saving category");
    }

    public void update(){
        Runnable operation = () -> {
            categoryService.update(currentCategory);
            addInfoMessage(SummaryValues.SUCCESS.getDescription(), "Category updated successfully");
            loadCategories();
        };
        executeOperation(operation, "Updating category");
    }

    public void delete(CategoryDTO category) {
        Runnable operation = () -> {
            categoryService.delete(category.getId());
            addInfoMessage(SummaryValues.SUCCESS.getDescription(), "Category deleted successfully");
            loadCategories();
        };
        executeOperation(operation, "Deleting category");
    }
}
