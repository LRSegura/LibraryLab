package membership.usecase;

import common.BaseService;
import membership.dto.MemberDTO;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import membership.model.Member;
import membership.model.MemberStatus;
import membership.port.MemberRepository;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class MemberService extends BaseService<Member> {

    private MemberRepository memberRepository;

    @Inject
    public MemberService(MemberRepository memberRepository){
        this.memberRepository = memberRepository;
    }

    public MemberService(){
        //Required by proxy
    }

    public List<MemberDTO> findAll() {
        return memberRepository.findAll().stream()
                .map(MemberDTO::fromEntity)
                .toList();
    }

    public Optional<MemberDTO> findById(Long id) {
        return memberRepository.findById(id)
                .map(MemberDTO::fromEntity);
    }

    public Optional<MemberDTO> findByMembershipNumber(String membershipNumber) {
        return memberRepository.findByMembershipNumber(membershipNumber)
                .map(MemberDTO::fromEntity);
    }

    public Optional<MemberDTO> findByEmail(String email) {
        return memberRepository.findByEmail(email)
                .map(MemberDTO::fromEntity);
    }

    public List<MemberDTO> findByStatus(MemberStatus status) {
        return memberRepository.findByStatus(status).stream()
                .map(MemberDTO::fromEntity)
                .toList();
    }

    public List<MemberDTO> findByName(String name) {
        return memberRepository.findByNameContaining(name).stream()
                .map(MemberDTO::fromEntity)
                .toList();
    }

    @Transactional
    public MemberDTO create(MemberDTO dto) {
        if (memberRepository.existsByEmail(dto.getEmail())) {
            throw new IllegalArgumentException("Member with email '" + dto.getEmail() + "' already exists");
        }
        if (memberRepository.existsByMembershipNumber(dto.getMembershipNumber())) {
            throw new IllegalArgumentException("Member with membership number '" + dto.getMembershipNumber() + "' already exists");
        }

        Member member = dto.toEntity();
        validateFieldsConstraint(member);
        memberRepository.save(member);
        return MemberDTO.fromEntity(member);
    }

    @Transactional
    public MemberDTO update(MemberDTO dto) {
        Member member = memberRepository.findById(dto.getId())
                .orElseThrow(() -> new IllegalArgumentException("Member not found with id: " + dto.getId()));

        Optional<Member> existingByEmail = memberRepository.findByEmail(dto.getEmail());
        if (existingByEmail.isPresent() && !existingByEmail.get().getId().equals(dto.getId())) {
            throw new IllegalArgumentException("Member with email '" + dto.getEmail() + "' already exists");
        }

        dto.updateEntity(member);
        validateFieldsConstraint(member);
        return MemberDTO.fromEntity(member);
    }

    @Transactional
    public void delete(Long id) {
        Member member = memberRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Member not found with id: " + id));

        if (member.getActiveLoans() > 0) {
            throw new IllegalStateException("Cannot delete member with active loans");
        }

        memberRepository.delete(member);
    }

    @Transactional
    public MemberDTO suspend(Long id) {
        Member member = memberRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Member not found with id: " + id));

        member.setStatus(MemberStatus.SUSPENDED);
        return MemberDTO.fromEntity(member);
    }

    @Transactional
    public MemberDTO activate(Long id) {
        Member member = memberRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Member not found with id: " + id));

        if (member.isMembershipExpired()) {
            throw new IllegalStateException("Cannot activate expired membership. Renew first.");
        }

        member.setStatus(MemberStatus.ACTIVE);
        return MemberDTO.fromEntity(member);
    }

    @Transactional
    public MemberDTO renewMembership(Long id, int years) {
        Member member = memberRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Member not found with id: " + id));

        member.renewMembership(years);
        return MemberDTO.fromEntity(member);
    }

    public String generateMembershipNumber() {
        return "MEM-" + System.currentTimeMillis();
    }
}