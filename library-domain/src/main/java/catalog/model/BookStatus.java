package catalog.model;

import lombok.Getter;

@Getter
public enum BookStatus {
    AVAILABLE("Available"),
    UNAVAILABLE("Unavailable"),
    RESERVED("Reserved"),
    DISCONTINUED("Discontinued");

    private final String description;

    BookStatus(String description){
        this.description = description;
    }
}
