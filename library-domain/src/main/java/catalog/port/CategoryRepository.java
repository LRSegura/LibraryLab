package catalog.port;

import catalog.model.Category;
import common.BaseRepository;

import java.util.Optional;

public interface CategoryRepository extends BaseRepository<Category> {

    Optional<Category> findByName(String name);

    boolean existsByName(String name);
}
