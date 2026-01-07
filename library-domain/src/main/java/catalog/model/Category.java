package catalog.model;

import common.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "categories")
@Getter
@Setter
@NoArgsConstructor
@AttributeOverride(name = "id", column = @Column(name = "category_id"))
public class Category extends BaseEntity {


    @NotBlank(message = "The name is required")
    @Size(max = 100)
    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Size(max = 500)
    @Column(length = 500)
    private String description;

    @OneToMany(mappedBy = "category", fetch = FetchType.LAZY)
    private List<Book> books = new ArrayList<>();

    public Category(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public void addBook(Book book) {
        books.add(book);
        book.setCategory(this);
    }

    public void removeBook(Book book) {
        books.remove(book);
        book.setCategory(null);
    }
}
