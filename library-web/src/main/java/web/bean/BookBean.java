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
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

@Named
@ViewScoped
@Getter
@Setter
public class BookBean extends BasicBean implements Serializable {

    private BookService bookService;
    private CategoryService categoryService;
    private static final Logger logger = Logger.getLogger(BookBean.class.getName());

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
    private BookDTO currentBook;

    @PostConstruct
    public void init() {
        loadBooks();
        loadCategories();
        initNewBook();
    }

    public void loadBooks() {
        books = new ArrayList<>(bookService.findAll());
        logEntities(books, logger);
    }

    public void loadCategories() {
        categories = categoryService.findAll();
        logEntities(categories, logger);
    }

    public void initNewBook() {
        currentBook = BookDTO.builder()
                .totalCopies(1)
                .status(BookStatus.AVAILABLE)
                .build();
    }

    public void save() {
        Runnable operation = () -> {
            bookService.create(currentBook);
            addInfoMessage(SummaryValues.SUCCESS.getDescription(), "Book created successfully");
            initNewBook();
            loadBooks();
        };
        executeOperation(operation, "Saving book");
    }

    public void update() {
        Runnable operation = () -> {
            bookService.update(currentBook);
            addInfoMessage(SummaryValues.SUCCESS.getDescription(), "Book updated successfully");
            loadBooks();
        };
        executeOperation(operation, "Updating book");
    }

    public void delete(BookDTO book) {
        Runnable operation = () -> {
            bookService.delete(book.getId());
            addInfoMessage(SummaryValues.SUCCESS.getDescription(), "Book deleted successfully");
            loadBooks();
        };
        executeOperation(operation, "Deleting book");
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