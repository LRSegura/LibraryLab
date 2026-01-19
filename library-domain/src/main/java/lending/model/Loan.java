package lending.model;

import catalog.model.Book;
import common.BaseEntity;
import common.exception.BusinessRuleException;
import common.exception.ExceptionMessage;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import membership.model.Member;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Entity
@Table(name = "loans")
@Getter
@Setter
@NoArgsConstructor
@AttributeOverride(name = "id", column = @Column(name = "loan_id"))
public class Loan extends BaseEntity {

    private static final int DEFAULT_LOAN_DAYS = 14;
    private static final int MAX_RENEWALS = 2;

    @NotNull(message = "{loan.book.required}")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @NotNull(message = "{loan.member.required}")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @NotNull(message = "{loan.loan-date.required}")
    @Column(name = "loan_date", nullable = false)
    private LocalDate loanDate;

    @NotNull(message = "{loan.due-date.required}")
    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "return_date")
    private LocalDate returnDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LoanStatus status = LoanStatus.ACTIVE;

    @Column(name = "renewal_count", nullable = false)
    private int renewalCount = 0;

    @Column(length = 500)
    private String notes;

    public Loan(Book book, Member member) {
        this.book = book;
        this.member = member;
        this.loanDate = LocalDate.now();
        this.dueDate = loanDate.plusDays(DEFAULT_LOAN_DAYS);
    }

    public Loan(Book book, Member member, int loanDays) {
        this.book = book;
        this.member = member;
        this.loanDate = LocalDate.now();
        this.dueDate = loanDate.plusDays(loanDays);
    }

    public boolean isOverdue() {
        return returnDate == null && LocalDate.now().isAfter(dueDate);
    }

    public long getDaysOverdue() {
        if (!isOverdue()) {
            return 0;
        }
        return ChronoUnit.DAYS.between(dueDate, LocalDate.now());
    }

    public long getDaysUntilDue() {
        if (returnDate != null) {
            return 0;
        }
        return ChronoUnit.DAYS.between(LocalDate.now(), dueDate);
    }

    public boolean canRenew() {
        return status == LoanStatus.ACTIVE
                && renewalCount < MAX_RENEWALS
                && !isOverdue();
    }

    public void renew() {
        renew(DEFAULT_LOAN_DAYS);
    }

    public void renew(int additionalDays) {
        if (!canRenew()) {
            throw new BusinessRuleException(ExceptionMessage.LOAN_CANNOT_RENEW,
                    member.getFullName(), book.getTitle());
        }
        this.dueDate = LocalDate.now().plusDays(additionalDays);
        this.renewalCount++;
    }

    public void returnBook() {
        if (status == LoanStatus.RETURNED) {
            throw new BusinessRuleException(ExceptionMessage.LOAN_ALREADY_RETURNED,
                    member.getFullName(), book.getTitle());
        }
        this.returnDate = LocalDate.now();
        this.status = LoanStatus.RETURNED;
    }

    public void markAsLost() {
        this.status = LoanStatus.LOST;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        Loan loan = (Loan) o;
        return book.equals(loan.book) && member.equals(loan.member) && loanDate.equals(loan.loanDate) && dueDate.equals(loan.dueDate);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + book.hashCode();
        result = 31 * result + member.hashCode();
        result = 31 * result + loanDate.hashCode();
        result = 31 * result + dueDate.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "Loan{" +
                "book=" + book +
                ", member=" + member +
                ", loanDate=" + loanDate +
                ", dueDate=" + dueDate +
                ", returnDate=" + returnDate +
                ", status=" + status +
                ", renewalCount=" + renewalCount +
                ", notes='" + notes + '\'' +
                "} " + super.toString();
    }
}