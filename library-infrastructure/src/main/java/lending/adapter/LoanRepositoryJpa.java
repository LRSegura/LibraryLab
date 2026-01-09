package lending.adapter;

import catalog.model.Book;
import common.adapter.BaseRepositoryJpa;
import lending.model.Loan;
import lending.model.LoanStatus;
import lending.port.LoanRepository;
import jakarta.enterprise.context.ApplicationScoped;
import membership.model.Member;

import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class LoanRepositoryJpa extends BaseRepositoryJpa<Loan> implements LoanRepository {

    @Override
    public List<Loan> findByMember(Member member) {
        return em.createQuery(
                "SELECT l FROM Loan l WHERE l.member = :member ORDER BY l.loanDate DESC", Loan.class)
                .setParameter("member", member)
                .getResultList();
    }

    @Override
    public List<Loan> findByBook(Book book) {
        return em.createQuery(
                "SELECT l FROM Loan l WHERE l.book = :book ORDER BY l.loanDate DESC", Loan.class)
                .setParameter("book", book)
                .getResultList();
    }

    @Override
    public List<Loan> findByStatus(LoanStatus status) {
        return em.createQuery(
                "SELECT l FROM Loan l WHERE l.status = :status ORDER BY l.dueDate", Loan.class)
                .setParameter("status", status)
                .getResultList();
    }

    @Override
    public List<Loan> findActiveByMember(Member member) {
        return em.createQuery(
                "SELECT l FROM Loan l WHERE l.member = :member AND l.status IN :statuses ORDER BY l.dueDate", Loan.class)
                .setParameter("member", member)
                .setParameter("statuses", List.of(LoanStatus.ACTIVE, LoanStatus.OVERDUE))
                .getResultList();
    }

    @Override
    public List<Loan> findOverdueLoans() {

        return em.createQuery(
                "SELECT l FROM Loan l WHERE l.status = :status AND l.dueDate < :today ORDER BY l.dueDate", Loan.class)
                .setParameter("status", LoanStatus.ACTIVE)
                .setParameter("today", LocalDate.now())
                .getResultList();
    }

    @Override
    public List<Loan> findByDueDateBefore(LocalDate date) {

        return em.createQuery(
                "SELECT l FROM Loan l WHERE l.dueDate < :date AND l.status IN :statuses ORDER BY l.dueDate", Loan.class)
                .setParameter("date", date)
                .setParameter("statuses", List.of(LoanStatus.ACTIVE, LoanStatus.OVERDUE))
                .getResultList();
    }

    @Override
    public Optional<Loan> findActiveByBookAndMember(Book book, Member member) {
        return em.createQuery(
                        "SELECT l FROM Loan l WHERE l.book = :book AND l.member = :member AND l.status IN :statuses", Loan.class)
                .setParameter("book", book)
                .setParameter("member", member)
                .setParameter("statuses", List.of(LoanStatus.ACTIVE, LoanStatus.OVERDUE))
                .getResultStream()
                .findFirst();
    }

    @Override
    public long countActiveByMember(Member member) {
        return em.createQuery(
                "SELECT COUNT(l) FROM Loan l WHERE l.member = :member AND l.status IN :statuses", Long.class)
                .setParameter("member", member)
                .setParameter("statuses", List.of(LoanStatus.ACTIVE, LoanStatus.OVERDUE))
                .getSingleResult();
    }
}
