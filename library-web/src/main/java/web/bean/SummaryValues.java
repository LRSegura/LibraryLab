package web.bean;

import lombok.Getter;

@Getter
public enum SummaryValues {
    INFO("Info"),
    SUCCESS("Success"),
    ERROR("Error"),
    WARNING("Warning");

    private final String description;

    SummaryValues(String description) {
        this.description = description;
    }

}
