package web.bean;

import catalog.dto.CategoryDTO;
import catalog.usecase.CategoryService;
import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
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
public class CategoryBean implements Serializable {


    private CategoryService categoryService;

    private List<CategoryDTO> categories;
    private CategoryDTO selectedCategory;
    private CategoryDTO newCategory;
    private boolean editMode;

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
        newCategory = new CategoryDTO();
        editMode = false;
    }

    public void prepareEdit(CategoryDTO category) {
        this.selectedCategory = CategoryDTO.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .bookCount(category.getBookCount())
                .build();
        editMode = true;
    }

    public void save() {
        try {
            if (editMode && selectedCategory != null) {
                categoryService.update(selectedCategory.getId(), selectedCategory);
                addMessage(FacesMessage.SEVERITY_INFO, "Success", "Category updated successfully");
            } else {
                categoryService.create(newCategory);
                addMessage(FacesMessage.SEVERITY_INFO, "Success", "Category created successfully");
                initNewCategory();
            }
            loadCategories();
        } catch (Exception e) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Error", e.getMessage());
        }
    }

    public void delete(CategoryDTO category) {
        try {
            categoryService.delete(category.getId());
            addMessage(FacesMessage.SEVERITY_INFO, "Success", "Category deleted successfully");
            loadCategories();
        } catch (Exception e) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Error", e.getMessage());
        }
    }

    private void addMessage(FacesMessage.Severity severity, String summary, String detail) {
        FacesContext.getCurrentInstance().addMessage(null, 
            new FacesMessage(severity, summary, detail));
    }
}
