package membership.adapter;

import common.adapter.BaseRepositoryJpa;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import membership.model.Member;
import membership.model.MemberStatus;
import membership.port.MemberRepository;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class MemberRepositoryJpa  extends BaseRepositoryJpa<Member> implements MemberRepository {

    @PersistenceContext
    private EntityManager em;


    @Override
    public Optional<Member> findByMembershipNumber(String membershipNumber) {
        return em.createQuery(
                "SELECT m FROM Member m WHERE m.membershipNumber = :membershipNumber", Member.class)
                .setParameter("membershipNumber", membershipNumber)
                .getResultStream()
                .findFirst();
    }

    @Override
    public Optional<Member> findByEmail(String email) {
        return em.createQuery(
                "SELECT m FROM Member m WHERE LOWER(m.email) = LOWER(:email)", Member.class)
                .setParameter("email", email)
                .getResultStream()
                .findFirst();
    }

    @Override
    public List<Member> findByStatus(MemberStatus status) {
        return em.createQuery(
                "SELECT m FROM Member m WHERE m.status = :status ORDER BY m.lastName, m.firstName", Member.class)
                .setParameter("status", status)
                .getResultList();
    }

    @Override
    public List<Member> findByNameContaining(String name) {
        return em.createQuery(
                "SELECT m FROM Member m WHERE LOWER(m.firstName) LIKE LOWER(:name) " +
                "OR LOWER(m.lastName) LIKE LOWER(:name) ORDER BY m.lastName, m.firstName", Member.class)
                .setParameter("name", "%" + name + "%")
                .getResultList();
    }

    @Override
    public boolean existsByEmail(String email) {
        Long count = em.createQuery(
                "SELECT COUNT(m) FROM Member m WHERE LOWER(m.email) = LOWER(:email)", Long.class)
                .setParameter("email", email)
                .getSingleResult();
        return count > 0;
    }

    @Override
    public boolean existsByMembershipNumber(String membershipNumber) {
        Long count = em.createQuery(
                "SELECT COUNT(m) FROM Member m WHERE m.membershipNumber = :membershipNumber", Long.class)
                .setParameter("membershipNumber", membershipNumber)
                .getSingleResult();
        return count > 0;
    }
}
