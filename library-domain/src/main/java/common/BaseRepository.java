package common;

import java.util.List;
import java.util.Optional;

public interface BaseRepository<T extends BaseEntity> {
    Optional<T> findById(Long id);
    List<T> findAll();
    void save(T entity);
    void delete(T entity);
    T update(T entity);
}
