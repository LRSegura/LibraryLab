package lending.usecase;

import catalog.model.Book;
import catalog.port.BookRepository;
import lending.dto.LoanDTO;
import lending.model.Loan;
import lending.model.LoanStatus;
import lending.port.LoanRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import membership.model.Member;
import membership.port.MemberRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class LoanService {

    private LoanRepository loanRepository;

    private BookRepository bookRepository;

    private MemberRepository memberRepository;

    @Inject
    public LoanService(LoanRepository loanRepository, BookRepository bookRepository, MemberRepository memberRepository) {
        this.loanRepository = loanRepository;
        this.bookRepository = bookRepository;
        this.memberRepository = memberRepository;
    }

    public LoanService(){
        //Required by proxy
    }

    public List<LoanDTO> findAll() {
        return loanRepository.findAll().stream()
                .map(LoanDTO::fromEntity)
                .toList();
    }

    public Optional<LoanDTO> findById(Long id) {
        return loanRepository.findById(id)
                .map(LoanDTO::fromEntity);
    }

    public List<LoanDTO> findByMember(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("Member not found with id: " + memberId));
        return loanRepository.findByMember(member).stream()
                .map(LoanDTO::fromEntity)
                .toList();
    }

    public List<LoanDTO> findActiveByMember(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("Member not found with id: " + memberId));
        return loanRepository.findActiveByMember(member).stream()
                .map(LoanDTO::fromEntity)
                .toList();
    }

    public List<LoanDTO> findByBook(Long bookId) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new IllegalArgumentException("Book not found with id: " + bookId));
        return loanRepository.findByBook(book).stream()
                .map(LoanDTO::fromEntity)
                .toList();
    }

    public List<LoanDTO> findByStatus(LoanStatus status) {
        return loanRepository.findByStatus(status).stream()
                .map(LoanDTO::fromEntity)
                .toList();
    }

    public List<LoanDTO> findOverdueLoans() {
        return loanRepository.findOverdueLoans().stream()
                .map(LoanDTO::fromEntity)
                .toList();
    }

    public List<LoanDTO> findLoansDueBefore(LocalDate date) {
        return loanRepository.findByDueDateBefore(date).stream()
                .map(LoanDTO::fromEntity)
                .toList();
    }

    @Transactional
    public LoanDTO borrowBook(Long bookId, Long memberId) {
        return borrowBook(bookId, memberId, null);
    }

    @Transactional
    public LoanDTO borrowBook(Long bookId, Long memberId, String notes) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new IllegalArgumentException("Book not found with id: " + bookId));

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("Member not found with id: " + memberId));

        if (!book.isAvailable()) {
            throw new IllegalStateException("Book '" + book.getTitle() + "' is not available for borrowing");
        }

        if (!member.canBorrow()) {
            throw new IllegalStateException("Member '" + member.getFullName() + "' cannot borrow books");
        }

        Optional<Loan> existingLoan = loanRepository.findActiveByBookAndMember(book, member);
        if (existingLoan.isPresent()) {
            throw new IllegalStateException("Member already has an active loan for this book");
        }

        book.borrowCopy();
        member.incrementActiveLoans();

        Loan loan = new Loan(book, member);
        if (notes != null && !notes.isBlank()) {
            loan.setNotes(notes);
        }

        loanRepository.save(loan);
        bookRepository.update(book);

        return LoanDTO.fromEntity(loan);
    }

    @Transactional
    public LoanDTO returnBook(Long loanId) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new IllegalArgumentException("Loan not found with id: " + loanId));

        if (loan.getStatus() == LoanStatus.RETURNED) {
            throw new IllegalStateException("Book has already been returned");
        }

        loan.returnBook();
        loan.getBook().returnCopy();
        loan.getMember().decrementActiveLoans();

        loanRepository.update(loan);
        bookRepository.update(loan.getBook());

        return LoanDTO.fromEntity(loan);
    }

    @Transactional
    public LoanDTO renewLoan(Long loanId) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new IllegalArgumentException("Loan not found with id: " + loanId));

        if (!loan.canRenew()) {
            throw new IllegalStateException("This loan cannot be renewed");
        }

        loan.renew();
        loanRepository.update(loan);

        return LoanDTO.fromEntity(loan);
    }

    @Transactional
    public LoanDTO markAsLost(Long loanId) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new IllegalArgumentException("Loan not found with id: " + loanId));

        if (loan.getStatus() == LoanStatus.RETURNED) {
            throw new IllegalStateException("Cannot mark a returned book as lost");
        }

        loan.markAsLost();
        loan.getMember().decrementActiveLoans();

        Book book = loan.getBook();
        book.setTotalCopies(book.getTotalCopies() - 1);

        loanRepository.update(loan);
        bookRepository.update(book);

        return LoanDTO.fromEntity(loan);
    }

    @Transactional
    public void updateOverdueStatus() {
        List<Loan> overdueLoans = loanRepository.findByStatus(LoanStatus.ACTIVE).stream()
                .filter(Loan::isOverdue)
                .toList();

        for (Loan loan : overdueLoans) {
            loan.setStatus(LoanStatus.OVERDUE);
            loanRepository.update(loan);
        }
    }
}
