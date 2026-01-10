package lending.dto;

import lending.model.Loan;
import lending.model.LoanStatus;
import jakarta.validation.constraints.NotNull;
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
public class LoanDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    @NotNull(message = "The book is required")
    private Long bookId;

    private String bookTitle;

    private String bookIsbn;

    @NotNull(message = "The member is required")
    private Long memberId;

    private String memberName;

    private String membershipNumber;

    private LocalDate loanDate;

    private LocalDate dueDate;

    private LocalDate returnDate;

    private LoanStatus status;

    private int renewalCount;

    private String notes;

    private boolean overdue;

    private long daysOverdue;

    private long daysUntilDue;

    private boolean canRenew;

    public static LoanDTO fromEntity(Loan loan) {
        LoanDTOBuilder builder = LoanDTO.builder()
                .id(loan.getId())
                .loanDate(loan.getLoanDate())
                .dueDate(loan.getDueDate())
                .returnDate(loan.getReturnDate())
                .status(loan.getStatus())
                .renewalCount(loan.getRenewalCount())
                .notes(loan.getNotes())
                .overdue(loan.isOverdue())
                .daysOverdue(loan.getDaysOverdue())
                .daysUntilDue(loan.getDaysUntilDue())
                .canRenew(loan.canRenew());

        if (loan.getBook() != null) {
            builder.bookId(loan.getBook().getId())
                   .bookTitle(loan.getBook().getTitle())
                   .bookIsbn(loan.getBook().getIsbn());
        }

        if (loan.getMember() != null) {
            builder.memberId(loan.getMember().getId())
                   .memberName(loan.getMember().getFullName())
                   .membershipNumber(loan.getMember().getMembershipNumber());
        }

        return builder.build();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        LoanDTO loanDTO = (LoanDTO) o;
        return id.equals(loanDTO.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
