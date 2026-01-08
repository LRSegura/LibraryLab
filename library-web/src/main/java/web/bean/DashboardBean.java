package web.bean;

import catalog.dto.BookDTO;
import catalog.usecase.BookService;
import catalog.usecase.CategoryService;
import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lending.dto.LoanDTO;
import lending.model.LoanStatus;
import lending.usecase.LoanService;
import lombok.Getter;
import membership.model.MemberStatus;
import membership.usecase.MemberService;

import java.io.Serializable;
import java.util.List;

@Named
@ViewScoped
@Getter
public class DashboardBean implements Serializable {

    private BookService bookService;
    private CategoryService categoryService;
    private MemberService memberService;
    private LoanService loanService;

    @Inject
    public DashboardBean(BookService bookService, CategoryService categoryService, MemberService memberService, LoanService loanService) {
        this.bookService = bookService;
        this.categoryService = categoryService;
        this.memberService = memberService;
        this.loanService = loanService;
    }

    public DashboardBean(){
        //Required by proxy
    }

    private long totalBooks;
    private long availableBooks;
    private long totalMembers;
    private long activeMembers;
    private long activeLoans;
    private long overdueLoans;
    private long totalCategories;
    private List<LoanDTO> recentLoans;
    private List<LoanDTO> overdueLoansDetail;

    @PostConstruct
    public void init() {
        loadStatistics();
    }

    private void loadStatistics() {
        var books = bookService.findAll();
        totalBooks = books.size();
        availableBooks = books.stream().filter(BookDTO::isAvailable).count();

        var members = memberService.findAll();
        totalMembers = members.size();
        activeMembers = memberService.findByStatus(MemberStatus.ACTIVE).size();

        var loans = loanService.findByStatus(LoanStatus.ACTIVE);
        activeLoans = loans.size();
        
        overdueLoansDetail = loanService.findOverdueLoans();
        overdueLoans = overdueLoansDetail.size();

        totalCategories = categoryService.findAll().size();

        recentLoans = loanService.findAll().stream()
                .limit(5)
                .toList();
    }
}
