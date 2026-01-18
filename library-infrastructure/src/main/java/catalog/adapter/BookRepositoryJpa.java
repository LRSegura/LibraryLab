package catalog.adapter;

import catalog.model.Book;
import catalog.model.BookStatus;
import catalog.model.Category;
import catalog.port.BookRepository;
import common.adapter.BaseRepositoryJpa;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class BookRepositoryJpa extends BaseRepositoryJpa<Book> implements BookRepository {

    private static final String ISBN = "isbn";
    private static final String CATEGORY = "category";
    private static final String TITLE = "title";
    private static final String AUTHOR = "author";
    private static final String STATUS = "status";

    @Override
    public Optional<Book> findByIsbn(String isbn) {
        String sql = "SELECT b FROM Book b WHERE b.isbn = :isbn";
        return getEntityManager().createQuery(sql, Book.class)
                .setParameter(ISBN, isbn)
                .getResultStream()
                .findFirst();
    }

    @Override
    public List<Book> findByCategory(Category category) {
        String sql = "SELECT b FROM Book b WHERE b.category = :category ORDER BY b.title";
        return getEntityManager().createQuery(sql, Book.class)
                .setParameter(CATEGORY, category)
                .getResultList();
    }

    @Override
    public List<Book> findByTitleContaining(String title) {
        String sql = "SELECT b FROM Book b WHERE LOWER(b.title) LIKE LOWER(:title) ORDER BY b.title";
        return getEntityManager().createQuery(sql, Book.class)
                .setParameter(TITLE, "%" + title + "%")
                .getResultList();
    }

    @Override
    public List<Book> findByAuthorContaining(String author) {
        String sql = "SELECT b FROM Book b WHERE LOWER(b.author) LIKE LOWER(:author) ORDER BY b.author, b.title";
        return getEntityManager().createQuery(sql, Book.class)
                .setParameter(AUTHOR, "%" + author + "%")
                .getResultList();
    }

    @Override
    public List<Book> findAvailable() {
        String sql = "SELECT b FROM Book b WHERE b.availableCopies > 0 AND b.status = :status ORDER BY b.title";
        return getEntityManager().createQuery(sql, Book.class)
                .setParameter(STATUS, BookStatus.AVAILABLE)
                .getResultList();
    }

    @Override
    public boolean existsByIsbn(String isbn) {
        String sql = "SELECT COUNT(b) FROM Book b WHERE b.isbn = :isbn";
        Long count = getEntityManager().createQuery(sql, Long.class)
                .setParameter(ISBN, isbn)
                .getSingleResult();
        return count > 0;
    }
}
