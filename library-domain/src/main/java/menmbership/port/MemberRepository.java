package menmbership.port;

import menmbership.model.Member;
import menmbership.model.MemberStatus;

import java.util.List;
import java.util.Optional;

public interface MemberRepository {

    Member save(Member member);

    Optional<Member> findById(Long id);

    Optional<Member> findByMembershipNumber(String membershipNumber);

    Optional<Member> findByEmail(String email);

    List<Member> findAll();

    List<Member> findByStatus(MemberStatus status);

    List<Member> findByNameContaining(String name);

    void delete(Member member);

    boolean existsByEmail(String email);

    boolean existsByMembershipNumber(String membershipNumber);
}
