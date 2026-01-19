package web.rest.mapper;

import jakarta.enterprise.context.ApplicationScoped;
import membership.dto.MemberDTO;
import web.rest.dto.MemberCreateRequest;
import web.rest.dto.MemberUpdateRequest;

@ApplicationScoped
public class MemberMapper {

    public MemberDTO toDto(MemberCreateRequest request) {
        return MemberDTO.builder()
                .membershipNumber(request.getMembershipNumber())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .address(request.getAddress())
                .maxLoans(request.getMaxLoans())
                .build();
    }

    public MemberDTO toDto(Long id, MemberUpdateRequest request) {
        return MemberDTO.builder()
                .id(id)
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .address(request.getAddress())
                .maxLoans(request.getMaxLoans())
                .status(request.getStatus())
                .build();
    }
}
