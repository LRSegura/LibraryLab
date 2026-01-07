package lending.model;

import lombok.Getter;

@Getter
public enum LoanStatus {
    ACTIVE( "Active"),
    RETURNED( "Returned"),
    OVERDUE( "Overdue"),
    LOST( "Lost");

    private final String description;

    LoanStatus(String description){
        this.description = description;
    }

}
