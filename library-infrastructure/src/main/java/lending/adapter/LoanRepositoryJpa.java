package lending.adapter;

import catalog.model.Book;
import common.adapter.BaseRepositoryJpa;
import lending.model.Loan;
import lending.model.LoanStatus;
import lending.port.LoanRepository;
import jakarta.enterprise.context.ApplicationScoped;
import membership.model.Member;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class LoanRepositoryJpa extends BaseRepositoryJpa<Loan> implements LoanRepository {

    private static final String MEMBER = "member";
    private static final String BOOK = "book";
    private static final String STATUS = "status";
    private static final String DUE_DATE = "dueDate";
    private static final String TODAY = "today";
    private static final String STATUSES = "statuses";

    @Override
    public List<Loan> findByMember(Member member) {
        String sql = "SELECT l FROM Loan l WHERE l.member = :member ORDER BY l.loanDate DESC";
        return em.createQuery(sql, Loan.class)
                .setParameter(MEMBER, member)
                .getResultList();
    }

    @Override
    public List<Loan> findByBook(Book book) {
        String sql = "SELECT l FROM Loan l WHERE l.book = :book ORDER BY l.loanDate DESC";
        return em.createQuery(sql, Loan.class)
                .setParameter(BOOK, book)
                .getResultList();
    }

    @Override
    public List<Loan> findByStatus(LoanStatus status) {
        String sql = "SELECT l FROM Loan l WHERE l.status = :status ORDER BY l.dueDate";
        return em.createQuery(sql, Loan.class)
                .setParameter(STATUS, status)
                .getResultList();
    }

    @Override
    public List<Loan> findActiveByMember(Member member) {
        String sql = "SELECT l FROM Loan l WHERE l.member = :member AND l.status IN :statuses ORDER BY l.dueDate";
        return em.createQuery(sql, Loan.class)
                .setParameter(MEMBER, member)
                .setParameter(STATUSES, List.of(LoanStatus.ACTIVE, LoanStatus.OVERDUE))
                .getResultList();
    }

    @Override
    public List<Loan> findOverdueLoans() {
        String sql = "SELECT l FROM Loan l WHERE l.status = :status AND l.dueDate < :today ORDER BY l.dueDate";
        return em.createQuery(sql, Loan.class)
                .setParameter(STATUS, LoanStatus.ACTIVE)
                .setParameter(TODAY, LocalDate.now())
                .getResultList();
    }

    @Override
    public List<Loan> findByDueDateBefore(LocalDate date) {
        String sql = "SELECT l FROM Loan l WHERE l.dueDate < :date AND l.status IN :statuses ORDER BY l.dueDate";
        return em.createQuery(sql, Loan.class)
                .setParameter(DUE_DATE, date)
                .setParameter(STATUSES, List.of(LoanStatus.ACTIVE, LoanStatus.OVERDUE))
                .getResultList();
    }

    @Override
    public Optional<Loan> findActiveByBookAndMember(Book book, Member member) {
        String sql = "SELECT l FROM Loan l WHERE l.book = :book AND l.member = :member AND l.status IN :statuses";
        return em.createQuery(
                        sql, Loan.class)
                .setParameter(BOOK, book)
                .setParameter(MEMBER, member)
                .setParameter(STATUSES, List.of(LoanStatus.ACTIVE, LoanStatus.OVERDUE))
                .getResultStream()
                .findFirst();
    }

    @Override
    public long countActiveByMember(Member member) {
        String sql = "SELECT COUNT(l) FROM Loan l WHERE l.member = :member AND l.status IN :statuses";
        return em.createQuery(
                        sql, Long.class)
                .setParameter(MEMBER, member)
                .setParameter(STATUSES, List.of(LoanStatus.ACTIVE, LoanStatus.OVERDUE))
                .getSingleResult();
    }
}
