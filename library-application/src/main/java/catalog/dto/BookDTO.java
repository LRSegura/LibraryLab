package catalog.dto;

import catalog.model.Book;
import catalog.model.BookStatus;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    @NotBlank(message = "The ISBN is required")
    @Size(min = 10, max = 17)
    private String isbn;

    @NotBlank(message = "The title is required")
    @Size(max = 255)
    private String title;

    @NotBlank(message = "The author is required")
    @Size(max = 255)
    private String author;

    @Size(max = 100)
    private String publisher;

    @PastOrPresent
    private LocalDate publicationDate;

    @Min(value = 1)
    private int totalCopies;

    private int availableCopies;

    private Long categoryId;

    private String categoryName;

    private BookStatus status;

    private boolean available;

    public static BookDTO fromEntity(Book book) {
        BookDTOBuilder builder = BookDTO.builder()
                .id(book.getId())
                .isbn(book.getIsbn())
                .title(book.getTitle())
                .author(book.getAuthor())
                .publisher(book.getPublisher())
                .publicationDate(book.getPublicationDate())
                .totalCopies(book.getTotalCopies())
                .availableCopies(book.getAvailableCopies())
                .status(book.getStatus())
                .available(book.isAvailable());

        if (book.getCategory() != null) {
            builder.categoryId(book.getCategory().getId())
                   .categoryName(book.getCategory().getName());
        }

        return builder.build();
    }

    public Book toEntity() {
        Book book = new Book(isbn, title, author);
        book.setPublisher(publisher);
        book.setPublicationDate(publicationDate);
        book.setTotalCopies(totalCopies);
        book.setAvailableCopies(totalCopies);
        book.setStatus(status != null ? status : BookStatus.AVAILABLE);
        return book;
    }

    public void updateEntity(Book book) {
        book.setIsbn(this.isbn);
        book.setTitle(this.title);
        book.setAuthor(this.author);
        book.setPublisher(this.publisher);
        book.setPublicationDate(this.publicationDate);
        book.setTotalCopies(this.totalCopies);
        if (this.status != null) {
            book.setStatus(this.status);
        }
    }



    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        BookDTO bookDTO = (BookDTO) o;
        return id.equals(bookDTO.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
