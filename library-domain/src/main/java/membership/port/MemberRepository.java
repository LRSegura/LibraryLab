package membership.port;

import common.BaseRepository;
import membership.model.Member;
import membership.model.MemberStatus;

import java.util.List;
import java.util.Optional;

public interface MemberRepository extends BaseRepository<Member> {

    Optional<Member> findByMembershipNumber(String membershipNumber);

    Optional<Member> findByEmail(String email);

    List<Member> findByStatus(MemberStatus status);

    List<Member> findByNameContaining(String name);

    boolean existsByEmail(String email);

    boolean existsByMembershipNumber(String membershipNumber);
}
