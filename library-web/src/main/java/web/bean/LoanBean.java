package web.bean;

import catalog.dto.BookDTO;
import catalog.usecase.BookService;
import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lending.dto.LoanDTO;
import lending.model.LoanStatus;
import lending.usecase.LoanService;
import lombok.Getter;
import lombok.Setter;
import membership.dto.MemberDTO;
import membership.usecase.MemberService;

import java.io.Serializable;
import java.util.List;

@Named
@ViewScoped
@Getter
@Setter
public class LoanBean extends BasicBean implements Serializable {

    private LoanService loanService;
    private BookService bookService;
    private MemberService memberService;

    private List<LoanDTO> loans;
    private List<LoanDTO> filteredLoans;
    private List<BookDTO> availableBooks;
    private List<MemberDTO> activeMembers;
    
    private Long selectedBookId;
    private Long selectedMemberId;
    private String loanNotes;
    
    private LoanStatus statusFilter;

    @Inject
    public LoanBean(LoanService loanService, BookService bookService, MemberService memberService) {
        this.loanService = loanService;
        this.bookService = bookService;
        this.memberService = memberService;
    }

    public LoanBean(){
        //Required by proxy
    }

    @PostConstruct
    public void init() {
        loadLoans();
        loadAvailableBooks();
        loadActiveMembers();
    }

    public void loadLoans() {
        if (statusFilter != null) {
            loans = loanService.findByStatus(statusFilter);
        } else {
            loans = loanService.findAll();
        }
    }

    public void loadAvailableBooks() {
        availableBooks = bookService.findAvailable();
    }

    public void loadActiveMembers() {
        activeMembers = memberService.findByStatus(membership.model.MemberStatus.ACTIVE);
    }

    public void initNewLoan() {
        selectedBookId = null;
        selectedMemberId = null;
        loanNotes = null;
        loadAvailableBooks();
        loadActiveMembers();
    }

    public void borrowBook() {
        try {
            if (selectedBookId == null || selectedMemberId == null) {
                addWarnMessage( "Please select both a book and a member");
                return;
            }
            loanService.borrowBook(selectedBookId, selectedMemberId, loanNotes);
            addInfoMessage(SummaryValues.SUCCESS.getDescription(), "Book borrowed successfully");
            initNewLoan();
            loadLoans();
        } catch (Exception e) {
            addErrorMessage("Error borrowing book.");
        }
    }

    public void returnBook(LoanDTO loan) {
        try {
            loanService.returnBook(loan.getId());
            addInfoMessage(SummaryValues.SUCCESS.getDescription(), "Book returned successfully");
            loadLoans();
            loadAvailableBooks();
        } catch (Exception e) {
            addErrorMessage("Error returning book.");
        }
    }

    public void renewLoan(LoanDTO loan) {
        try {
            loanService.renewLoan(loan.getId());
            addInfoMessage(SummaryValues.SUCCESS.getDescription(), "Loan renewed successfully");
            loadLoans();
        } catch (Exception e) {
            addErrorMessage("Error renewing loan.");
        }
    }

    public void markAsLost(LoanDTO loan) {
        try {
            loanService.markAsLost(loan.getId());
            addInfoMessage(SummaryValues.SUCCESS.getDescription(), "Book marked as lost");
            loadLoans();
        } catch (Exception e) {
            addErrorMessage("Error marking book as lost.");
        }
    }

    public void filterByStatus() {
        loadLoans();
    }

    public void clearFilter() {
        statusFilter = null;
        loadLoans();
    }

    public LoanStatus[] getStatuses() {
        return LoanStatus.values();
    }

    public String getStatusSeverity(LoanStatus status) {
        return switch (status) {
            case ACTIVE -> "info";
            case RETURNED -> "success";
            case OVERDUE -> "danger";
            case LOST -> "warning";
        };
    }
}
