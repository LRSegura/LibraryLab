package common.exception;

import lombok.Getter;

import java.text.MessageFormat;
import java.util.ResourceBundle;

@Getter
public enum ExceptionMessage {

    // Common
    ENTITY_NOT_FOUND("entity.not.found"),
    ENTITY_DUPLICATE("entity.duplicate"),

    // Catalog - Book
    BOOK_NO_COPIES_AVAILABLE("book.no.copies.available"),
    BOOK_ALL_COPIES_AVAILABLE("book.all.copies.available"),

    // Catalog - Category
    CATEGORY_HAS_BOOKS("category.has.books"),

    // Membership - Member
    MEMBER_CANNOT_BORROW("member.cannot.borrow"),
    MEMBER_NO_ACTIVE_LOANS("member.no.active.loans"),
    MEMBER_MEMBERSHIP_EXPIRED("member.membership.expired"),

    // Lending - Loan
    LOAN_CANNOT_RENEW("loan.cannot.renew"),
    LOAN_ALREADY_RETURNED("loan.already.returned"),
    LOAN_BOOK_NOT_AVAILABLE("loan.book.not.available"),
    LOAN_MEMBER_NOT_ACTIVE("loan.member.not.active");

    private static final ResourceBundle BUNDLE = ResourceBundle.getBundle("ExceptionMessages");

    private final String key;

    ExceptionMessage(String key) {
        this.key = key;
    }

    public String getMessage() {
        return BUNDLE.getString(key);
    }

    public String getMessage(Object... params) {
        return MessageFormat.format(BUNDLE.getString(key), params);
    }
}