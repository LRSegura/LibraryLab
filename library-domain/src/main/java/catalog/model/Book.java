package catalog.model;

import common.BaseEntity;
import common.exception.BusinessRuleException;
import common.exception.ExceptionMessage;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "books")
@Getter
@Setter
@AttributeOverride(name = "id", column = @Column(name = "book_id"))
@NoArgsConstructor
public class Book extends BaseEntity {

    @NotBlank(message = "{book.isbn.required}")
    @Size(message = "{book.isbn.size}", min = 10, max = 17)
    @Column(nullable = false, unique = true, length = 17)
    private String isbn;

    @NotBlank(message = "{book.title.required}")
    @Size(max = 255, message = "{book.title.size}")
    @Column(nullable = false)
    private String title;

    @NotBlank(message = "{book.author.required}")
    @Size(max = 255, message = "{book.author.size}")
    @Column(nullable = false)
    private String author;

    @Size(max = 100, message = "{book.publisher.size}")
    @Column(length = 100)
    private String publisher;

    @PastOrPresent(message = "{book.publication-date.past-or-present}")
    private LocalDate publicationDate;

    @Min(value = 1, message = "{book.total-copies.min}")
    @Column(nullable = false)
    private int totalCopies = 1;

    @Min(value = 0, message = "{book.available-copies.min}")
    @Column(nullable = false)
    private int availableCopies = 1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BookStatus status = BookStatus.AVAILABLE;

    public Book(String isbn, String title, String author) {
        this.isbn = isbn;
        this.title = title;
        this.author = author;
    }

    public boolean isAvailable() {
        return availableCopies > 0 && status == BookStatus.AVAILABLE;
    }

    public void borrowCopy() {
        if (!isAvailable()) {
            throw new BusinessRuleException(ExceptionMessage.BOOK_NO_COPIES_AVAILABLE, title);
        }
        availableCopies--;
    }

    public void returnCopy() {
        if (availableCopies >= totalCopies) {
            throw new BusinessRuleException(ExceptionMessage.BOOK_ALL_COPIES_AVAILABLE, title);
        }
        availableCopies++;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        Book book = (Book) o;
        return isbn.equals(book.isbn) && title.equals(book.title) && author.equals(book.author);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + isbn.hashCode();
        result = 31 * result + title.hashCode();
        result = 31 * result + author.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "Book{" +
                "isbn='" + isbn + '\'' +
                ", title='" + title + '\'' +
                ", author='" + author + '\'' +
                ", publisher='" + publisher + '\'' +
                ", publicationDate=" + publicationDate +
                ", totalCopies=" + totalCopies +
                ", availableCopies=" + availableCopies +
                "} " + super.toString();
    }
}