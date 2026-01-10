package catalog.model;

import common.BaseEntity;
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

    @NotBlank(message = "The ISBN is required")
    @Size(message = "ISBN must be between 10 and 17 characters", min = 10, max = 17)
    @Column(nullable = false, unique = true, length = 17)
    private String isbn;

    @NotBlank(message = "The title is required")
    @Size(max = 255, message = "The title must be less than 255 characters")
    @Column(nullable = false)
    private String title;

    @NotBlank(message = "The author is required")
    @Size(max = 255, message = "The author name must be less than 255 characters")
    @Column(nullable = false)
    private String author;

    @Size(max = 100, message = "The publisher name must be less than 100 characters")
    @Column(length = 100)
    private String publisher;

    @PastOrPresent
    private LocalDate publicationDate;

    @Min(value = 1, message = "The total number of copies must be at least 1")
    @Column(nullable = false)
    private int totalCopies = 1;

    @Min(value = 0, message = "The number of available copies must be greater than or equal to zero")
    @Column(nullable = false)
    private int availableCopies = 1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
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
            throw new IllegalStateException("No copies are available for the book: " + title);
        }
        availableCopies--;
    }

    public void returnCopy() {
        if (availableCopies >= totalCopies) {
            throw new IllegalStateException("All copies are already available");
        }
        availableCopies++;
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
