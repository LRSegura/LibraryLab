package web.bean;

import catalog.dto.BookDTO;
import catalog.dto.CategoryDTO;
import catalog.model.BookStatus;
import catalog.usecase.BookService;
import catalog.usecase.CategoryService;
import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.validation.ConstraintViolationException;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

@Named
@ViewScoped
@Getter
@Setter
public class BookBean extends BasicBean implements Serializable {

    private BookService bookService;
    private CategoryService categoryService;

    @Inject
    public BookBean(BookService bookService, CategoryService categoryService) {
        this.bookService = bookService;
        this.categoryService = categoryService;
    }

    public BookBean() {
        //Required by proxy
    }

    private List<BookDTO> books;
    private List<BookDTO> filteredBooks;
    private List<CategoryDTO> categories;
    private BookDTO selectedBook;
    private BookDTO newBook;
    private boolean editMode;

    @PostConstruct
    public void init() {
        loadBooks();
        loadCategories();
        initNewBook();
    }

    public void loadBooks() {
        books = bookService.findAll();
    }

    public void loadCategories() {
        categories = categoryService.findAll();
    }

    public void initNewBook() {
        newBook = BookDTO.builder()
                .totalCopies(1)
                .status(BookStatus.AVAILABLE)
                .build();
        editMode = false;
    }

    public void prepareEdit(BookDTO book) {
        this.selectedBook = BookDTO.builder()
                .id(book.getId())
                .isbn(book.getIsbn())
                .title(book.getTitle())
                .author(book.getAuthor())
                .publisher(book.getPublisher())
                .publicationDate(book.getPublicationDate())
                .totalCopies(book.getTotalCopies())
                .availableCopies(book.getAvailableCopies())
                .categoryId(book.getCategoryId())
                .status(book.getStatus())
                .build();
        editMode = true;
    }

    public void save() {
        try {
            if (editMode && selectedBook != null) {
                bookService.update(selectedBook.getId(), selectedBook);
                addInfoMessage(SummaryValues.SUCCESS.getDescription(), "Book updated successfully");
            } else {
                bookService.create(newBook);
                addInfoMessage(SummaryValues.SUCCESS.getDescription(), "Book created successfully");
                initNewBook();
            }
            loadBooks();
        } catch (ConstraintViolationException exception) {
            exception.getConstraintViolations().forEach(violation -> addErrorMessage(violation.getMessage()));
        } catch (Exception e) {
            addErrorMessage("Error creating book.");
        }
    }

    public void delete(BookDTO book) {
        try {
            bookService.delete(book.getId());
            addInfoMessage(SummaryValues.SUCCESS.getDescription(), "Book deleted successfully");
            loadBooks();
        } catch (Exception e) {
            addErrorMessage("Error deleting book.");
        }
    }

    public BookStatus[] getStatuses() {
        return BookStatus.values();
    }

    public String getCategoryName(Long categoryId) {
        if (categoryId == null) return "-";
        return categories.stream()
                .filter(c -> c.getId().equals(categoryId))
                .map(CategoryDTO::getName)
                .findFirst()
                .orElse("-");
    }
}
