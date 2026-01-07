package catalog.port;

import catalog.model.Book;
import catalog.model.Category;
import common.BaseRepository;

import java.util.List;
import java.util.Optional;

public interface BookRepository extends BaseRepository<Book> {

    Optional<Book> findByIsbn(String isbn);

    List<Book> findByCategory(Category category);

    List<Book> findByTitleContaining(String title);

    List<Book> findByAuthorContaining(String author);

    List<Book> findAvailable();

    boolean existsByIsbn(String isbn);
}
