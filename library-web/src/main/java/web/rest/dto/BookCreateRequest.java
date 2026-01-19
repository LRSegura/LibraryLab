package web.rest.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookCreateRequest {

    @NotBlank(message = "The ISBN is required")
    @Size(min = 10, max = 17, message = "ISBN must be between 10 and 17 characters")
    private String isbn;

    @NotBlank(message = "The title is required")
    @Size(max = 255, message = "The title must be less than 255 characters")
    private String title;

    @NotBlank(message = "The author is required")
    @Size(max = 255, message = "The author name must be less than 255 characters")
    private String author;

    @Size(max = 100, message = "The publisher name must be less than 100 characters")
    private String publisher;

    @PastOrPresent(message = "The publication date must be in the past or present")
    private LocalDate publicationDate;

    @Min(value = 1, message = "The total number of copies must be at least 1")
    private int totalCopies = 1;

    @NotNull(message = "The category is required")
    private Long categoryId;
}
