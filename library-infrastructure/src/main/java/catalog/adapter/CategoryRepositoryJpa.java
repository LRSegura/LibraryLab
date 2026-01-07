package catalog.adapter;

import catalog.model.Category;
import catalog.port.CategoryRepository;
import common.adapter.BaseRepositoryJpa;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;

@ApplicationScoped
public class CategoryRepositoryJpa extends BaseRepositoryJpa<Category> implements CategoryRepository {

    @Override
    public Optional<Category> findByName(String name) {
        return em.createQuery(
                "SELECT c FROM Category c WHERE LOWER(c.name) = LOWER(:name)", Category.class)
                .setParameter("name", name)
                .getResultStream()
                .findFirst();
    }

    @Override
    public boolean existsByName(String name) {
        Long count = em.createQuery(
                "SELECT COUNT(c) FROM Category c WHERE LOWER(c.name) = LOWER(:name)", Long.class)
                .setParameter("name", name)
                .getSingleResult();
        return count > 0;
    }
}
