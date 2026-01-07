package lending.port;



import catalog.model.Book;
import common.BaseRepository;
import lending.model.Loan;
import lending.model.LoanStatus;
import membership.model.Member;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface LoanRepository extends BaseRepository<Loan> {

    List<Loan> findByMember(Member member);

    List<Loan> findByBook(Book book);

    List<Loan> findByStatus(LoanStatus status);

    List<Loan> findActiveByMember(Member member);

    List<Loan> findOverdueLoans();

    List<Loan> findByDueDateBefore(LocalDate date);

    Optional<Loan> findActiveByBookAndMember(Book book, Member member);

    long countActiveByMember(Member member);
}
