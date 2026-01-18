package catalog.usecase;

import catalog.dto.BookDTO;
import catalog.model.Book;
import catalog.model.Category;
import catalog.port.BookRepository;
import catalog.port.CategoryRepository;
import com.sun.jdi.request.DuplicateRequestException;
import common.BaseService;
import common.exception.BusinessRuleException;
import common.exception.EntityNotFoundException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class BookService extends BaseService<Book> {

    private BookRepository bookRepository;
    private CategoryRepository categoryRepository;

    @Inject
    public BookService(BookRepository bookRepository, CategoryRepository categoryRepository) {
        this.bookRepository = bookRepository;
        this.categoryRepository = categoryRepository;
    }

    public BookService() {
        //Required by proxy
    }

    public List<BookDTO> findAll() {
        return bookRepository.findAll()
                .stream()
                .map(BookDTO::fromEntity)
                .toList();
    }

    public Optional<BookDTO> findById(Long id) {
        return bookRepository.findById(id)
                .map(BookDTO::fromEntity);
    }

    public Optional<BookDTO> findByIsbn(String isbn) {
        return bookRepository.findByIsbn(isbn)
                .map(BookDTO::fromEntity);
    }

    public List<BookDTO> findByTitle(String title) {
        return bookRepository.findByTitleContaining(title).stream()
                .map(BookDTO::fromEntity)
                .toList();
    }

    public List<BookDTO> findByAuthor(String author) {
        return bookRepository.findByAuthorContaining(author).stream()
                .map(BookDTO::fromEntity)
                .toList();
    }

    public List<BookDTO> findAvailable() {
        return bookRepository.findAvailable().stream()
                .map(BookDTO::fromEntity)
                .toList();
    }

    public List<BookDTO> findByCategory(Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new EntityNotFoundException("Category not found with id: " , categoryId));
        return bookRepository.findByCategory(category).stream()
                .map(BookDTO::fromEntity)
                .toList();
    }

    @Transactional
    public BookDTO create(BookDTO dto) {
        if (bookRepository.existsByIsbn(dto.getIsbn())) {
            throw new DuplicateRequestException("Book with ISBN '" + dto.getIsbn() + "' already exists");
        }

        Book book = dto.toEntity();

        if (dto.getCategoryId() != null) {
            Category category = categoryRepository.findById(dto.getCategoryId())
                    .orElseThrow(() -> new EntityNotFoundException("Category not found with id: " , dto.getCategoryId()));
            book.setCategory(category);
        }

        validateFieldsConstraint(book);

        bookRepository.save(book);
        return BookDTO.fromEntity(book);
    }

    @Transactional
    public BookDTO update(BookDTO dto) {
        Book book = bookRepository.findById(dto.getId())
                .orElseThrow(() -> new EntityNotFoundException("Book not found with id: ", dto.getId()));

        Optional<Book> existingByIsbn = bookRepository.findByIsbn(dto.getIsbn());
        if (existingByIsbn.isPresent() && !existingByIsbn.get().getId().equals(dto.getId())) {
            throw new DuplicateRequestException("Book with ISBN '" + dto.getIsbn() + "' already exists");
        }

        dto.updateEntity(book);

        if (dto.getCategoryId() != null) {
            Category category = categoryRepository.findById(dto.getCategoryId())
                    .orElseThrow(() -> new EntityNotFoundException("Category not found with id: " , dto.getCategoryId()));
            book.setCategory(category);
        }

        validateFieldsConstraint(book);
        bookRepository.update(book);
        return BookDTO.fromEntity(book);
    }

    @Transactional
    public void delete(Long id) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Book not found with id: ", id));

        if (book.getAvailableCopies() < book.getTotalCopies()) {
            throw new BusinessRuleException("Cannot delete book with active loans");
        }

        bookRepository.delete(book);
    }

    @Transactional
    public void updateCopies(Long id, int totalCopies) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Book not found with id: ", id));

        int loanedCopies = book.getTotalCopies() - book.getAvailableCopies();
        if (totalCopies < loanedCopies) {
            throw new BusinessRuleException("Cannot set total copies below currently loaned copies");
        }

        int availableDiff = totalCopies - book.getTotalCopies();
        book.setTotalCopies(totalCopies);
        book.setAvailableCopies(book.getAvailableCopies() + availableDiff);
        bookRepository.update(book);
    }
}