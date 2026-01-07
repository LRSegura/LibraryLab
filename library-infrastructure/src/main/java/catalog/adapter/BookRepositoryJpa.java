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

    @Override
    public Optional<Book> findByIsbn(String isbn) {
        return em.createQuery(
                "SELECT b FROM Book b WHERE b.isbn = :isbn", Book.class)
                .setParameter("isbn", isbn)
                .getResultStream()
                .findFirst();
    }

    @Override
    public List<Book> findByCategory(Category category) {
        return em.createQuery(
                "SELECT b FROM Book b WHERE b.category = :category ORDER BY b.title", Book.class)
                .setParameter("category", category)
                .getResultList();
    }

    @Override
    public List<Book> findByTitleContaining(String title) {
        return em.createQuery(
                "SELECT b FROM Book b WHERE LOWER(b.title) LIKE LOWER(:title) ORDER BY b.title", Book.class)
                .setParameter("title", "%" + title + "%")
                .getResultList();
    }

    @Override
    public List<Book> findByAuthorContaining(String author) {
        return em.createQuery(
                "SELECT b FROM Book b WHERE LOWER(b.author) LIKE LOWER(:author) ORDER BY b.author, b.title", Book.class)
                .setParameter("author", "%" + author + "%")
                .getResultList();
    }

    @Override
    public List<Book> findAvailable() {
        return em.createQuery(
                "SELECT b FROM Book b WHERE b.availableCopies > 0 AND b.status = :status ORDER BY b.title", Book.class)
                .setParameter("status", BookStatus.AVAILABLE)
                .getResultList();
    }

    @Override
    public boolean existsByIsbn(String isbn) {
        Long count = em.createQuery(
                "SELECT COUNT(b) FROM Book b WHERE b.isbn = :isbn", Long.class)
                .setParameter("isbn", isbn)
                .getSingleResult();
        return count > 0;
    }
}
