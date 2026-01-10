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
import membership.model.MemberStatus;
import membership.usecase.MemberService;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

@Named
@ViewScoped
@Getter
@Setter
public class LoanBean extends BasicBean implements Serializable {

    private static final Logger logger = Logger.getLogger(LoanBean.class.getName());
    private LoanService loanService;
    private BookService bookService;
    private MemberService memberService;

    private List<LoanDTO> loans;
    private List<LoanDTO> filteredLoans;
    private List<BookDTO> availableBooks;
    private List<MemberDTO> activeMembers;

    private LoanDTO currentLoan;
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
        initNewLoan();
    }

    public void loadLoans() {
        if (statusFilter != null) {
            loans = new ArrayList<>(loanService.findByStatus(statusFilter));
        } else {
            loans = new ArrayList<>(loanService.findAll());
        }
        logEntities(loans, logger);
    }

    public void loadAvailableBooks() {
        availableBooks = bookService.findAvailable();
        logEntities(availableBooks, logger);
    }

    public void loadActiveMembers() {
        activeMembers = memberService.findByStatus(MemberStatus.ACTIVE);
    }

    public void initNewLoan() {
        currentLoan = new LoanDTO();
        loadAvailableBooks();
        loadActiveMembers();
    }

    public void borrowBook() {
        Runnable operation = () -> {
            if (currentLoan.getBookId() == null || currentLoan.getMemberId() == null) {
                addWarnMessage("Please select both a book and a member");
                return;
            }
            loanService.borrowBook(currentLoan.getBookId(), currentLoan.getMemberId(), currentLoan.getNotes());
            addInfoMessage(SummaryValues.SUCCESS.getDescription(), "Book borrowed successfully");
            initNewLoan();
            loadLoans();
        };
        executeOperation(operation, "Borrowing book");
    }

    public void returnBook(LoanDTO loan) {
        Runnable operation = () -> {
            loanService.returnBook(loan.getId());
            addInfoMessage(SummaryValues.SUCCESS.getDescription(), "Book returned successfully");
            loadLoans();
            loadAvailableBooks();
        };
        executeOperation(operation, "Returning book");
    }

    public void renewLoan(LoanDTO loan) {
        Runnable operation = () -> {
            loanService.renewLoan(loan.getId());
            addInfoMessage(SummaryValues.SUCCESS.getDescription(), "Loan renewed successfully");
            loadLoans();
        };
        executeOperation(operation, "Renewing loan");
    }

    public void markAsLost(LoanDTO loan) {
        Runnable operation = () -> {
            loanService.markAsLost(loan.getId());
            addInfoMessage(SummaryValues.SUCCESS.getDescription(), "Book marked as lost");
            loadLoans();
        };
        executeOperation(operation, "Marking book as lost");
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