package web.bean;

import catalog.dto.BookDTO;
import catalog.usecase.BookService;
import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
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
public class LoanBean implements Serializable {

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
                addMessage(FacesMessage.SEVERITY_WARN, "Warning", "Please select both a book and a member");
                return;
            }
            loanService.borrowBook(selectedBookId, selectedMemberId, loanNotes);
            addMessage(FacesMessage.SEVERITY_INFO, "Success", "Book borrowed successfully");
            initNewLoan();
            loadLoans();
        } catch (Exception e) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Error", e.getMessage());
        }
    }

    public void returnBook(LoanDTO loan) {
        try {
            loanService.returnBook(loan.getId());
            addMessage(FacesMessage.SEVERITY_INFO, "Success", "Book returned successfully");
            loadLoans();
            loadAvailableBooks();
        } catch (Exception e) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Error", e.getMessage());
        }
    }

    public void renewLoan(LoanDTO loan) {
        try {
            loanService.renewLoan(loan.getId());
            addMessage(FacesMessage.SEVERITY_INFO, "Success", "Loan renewed successfully");
            loadLoans();
        } catch (Exception e) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Error", e.getMessage());
        }
    }

    public void markAsLost(LoanDTO loan) {
        try {
            loanService.markAsLost(loan.getId());
            addMessage(FacesMessage.SEVERITY_INFO, "Success", "Book marked as lost");
            loadLoans();
        } catch (Exception e) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Error", e.getMessage());
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

    private void addMessage(FacesMessage.Severity severity, String summary, String detail) {
        FacesContext.getCurrentInstance().addMessage(null, 
            new FacesMessage(severity, summary, detail));
    }
}
