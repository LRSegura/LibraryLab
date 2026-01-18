package membership.adapter;

import common.adapter.BaseRepositoryJpa;
import jakarta.enterprise.context.ApplicationScoped;
import membership.model.Member;
import membership.model.MemberStatus;
import membership.port.MemberRepository;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class MemberRepositoryJpa extends BaseRepositoryJpa<Member> implements MemberRepository {

    private static final String MEMBERSHIP_NUMBER = "membershipNumber";
    private static final String EMAIL = "email";
    private static final String STATUS = "status";
    private static final String NAME = "name";

    @Override
    public Optional<Member> findByMembershipNumber(String membershipNumber) {
        String sql = "SELECT m FROM Member m WHERE m.membershipNumber = :membershipNumber";
        return getEntityManager().createQuery(
                        sql, Member.class)
                .setParameter(MEMBERSHIP_NUMBER, membershipNumber)
                .getResultStream()
                .findFirst();
    }

    @Override
    public Optional<Member> findByEmail(String email) {
        String sql = "SELECT m FROM Member m WHERE LOWER(m.email) = LOWER(:email)";
        return getEntityManager().createQuery(sql, Member.class)
                .setParameter(EMAIL, email)
                .getResultStream()
                .findFirst();
    }

    @Override
    public List<Member> findByStatus(MemberStatus status) {
        String sql = "SELECT m FROM Member m WHERE m.status = :status ORDER BY m.lastName, m.firstName";
        return getEntityManager().createQuery(sql, Member.class)
                .setParameter(STATUS, status)
                .getResultList();
    }

    @Override
    public List<Member> findByNameContaining(String name) {
        String sql = "SELECT m FROM Member m WHERE LOWER(m.firstName) LIKE LOWER(:name) " +
                "OR LOWER(m.lastName) LIKE LOWER(:name) ORDER BY m.lastName, m.firstName";
        return getEntityManager().createQuery(sql, Member.class)
                .setParameter(NAME, "%" + name + "%")
                .getResultList();
    }

    @Override
    public boolean existsByEmail(String email) {
        String sql = "SELECT COUNT(m) FROM Member m WHERE LOWER(m.email) = LOWER(:email)";
        Long count = getEntityManager().createQuery(sql, Long.class)
                .setParameter(EMAIL, email)
                .getSingleResult();
        return count > 0;
    }

    @Override
    public boolean existsByMembershipNumber(String membershipNumber) {
        String sql = "SELECT COUNT(m) FROM Member m WHERE m.membershipNumber = :membershipNumber";
        Long count = getEntityManager().createQuery(sql, Long.class)
                .setParameter(MEMBERSHIP_NUMBER, membershipNumber)
                .getSingleResult();
        return count > 0;
    }
}
