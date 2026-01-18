package catalog.adapter;

import catalog.model.Category;
import catalog.port.CategoryRepository;
import common.adapter.BaseRepositoryJpa;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;

@ApplicationScoped
public class CategoryRepositoryJpa extends BaseRepositoryJpa<Category> implements CategoryRepository {

    private static final String NAME = "name";

    @Override
    public Optional<Category> findByName(String name) {
        String sql = "SELECT c FROM Category c WHERE LOWER(c.name) = LOWER(:name)";
        return getEntityManager().createQuery(sql, Category.class)
                .setParameter(NAME, name)
                .getResultStream()
                .findFirst();
    }

    @Override
    public boolean existsByName(String name) {
        String sql = "SELECT COUNT(c) FROM Category c WHERE LOWER(c.name) = LOWER(:name)";
        Long count = getEntityManager().createQuery(sql, Long.class)
                .setParameter(NAME, name)
                .getSingleResult();
        return count > 0;
    }
}
