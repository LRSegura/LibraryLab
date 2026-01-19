package web.rest.mapper;

import catalog.dto.BookDTO;
import catalog.model.BookStatus;
import jakarta.enterprise.context.ApplicationScoped;
import web.rest.dto.BookCreateRequest;
import web.rest.dto.BookUpdateRequest;

@ApplicationScoped
public class BookMapper {

    public BookDTO toDto(BookCreateRequest request) {
        return BookDTO.builder()
                .isbn(request.getIsbn())
                .title(request.getTitle())
                .author(request.getAuthor())
                .publisher(request.getPublisher())
                .publicationDate(request.getPublicationDate())
                .totalCopies(request.getTotalCopies())
                .categoryId(request.getCategoryId())
                .status(BookStatus.AVAILABLE)
                .build();
    }

    public BookDTO toDto(Long id, BookUpdateRequest request) {
        return BookDTO.builder()
                .id(id)
                .isbn(request.getIsbn())
                .title(request.getTitle())
                .author(request.getAuthor())
                .publisher(request.getPublisher())
                .publicationDate(request.getPublicationDate())
                .totalCopies(request.getTotalCopies())
                .categoryId(request.getCategoryId())
                .status(request.getStatus())
                .build();
    }
}
