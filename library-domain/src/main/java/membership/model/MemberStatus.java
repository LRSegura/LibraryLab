package membership.model;

import lombok.Getter;

@Getter
public enum MemberStatus {
    ACTIVE( "Active"),
    SUSPENDED( "Suspended"),
    EXPIRED( "Expired"),
    INACTIVE( "Inactive");

    private final String description;

    MemberStatus(String description) {
        this.description = description;
    }

}
