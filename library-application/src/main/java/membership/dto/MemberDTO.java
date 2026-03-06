package membership.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import membership.model.Member;
import membership.model.MemberStatus;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    @NotBlank(message = "The membership number is required")
    @Size(max = 20)
    private String membershipNumber;

    @NotBlank(message = "The first name is required")
    @Size(max = 100)
    private String firstName;

    @NotBlank(message = "The last name is required")
    @Size(max = 100)
    private String lastName;

    @NotBlank(message = "The email is required")
    @Email(message = "Invalid email")
    private String email;

    @Size(max = 20)
    private String phone;

    @Size(max = 255)
    private String address;

    private LocalDate registrationDate;

    private LocalDate expirationDate;

    private MemberStatus status;

    private int activeLoans;

    private int maxLoans;

    private String fullName;

    private boolean canBorrow;

    private boolean membershipExpired;

    public static MemberDTO fromEntity(Member member) {
        return MemberDTO.builder()
                .id(member.getId())
                .membershipNumber(member.getMembershipNumber())
                .firstName(member.getFirstName())
                .lastName(member.getLastName())
                .email(member.getEmail())
                .phone(member.getPhone())
                .address(member.getAddress())
                .registrationDate(member.getRegistrationDate())
                .expirationDate(member.getExpirationDate())
                .status(member.getStatus())
                .activeLoans(member.getActiveLoans())
                .maxLoans(member.getMaxLoans())
                .fullName(member.getFullName())
                .canBorrow(member.canBorrow())
                .membershipExpired(member.isMembershipExpired())
                .build();
    }

    public Member toEntity() {
        return toEntity(Member.DEFAULT_MAX_LOANS, Member.DEFAULT_MEMBERSHIP_YEARS);
    }

    /**
     * Creates entity using configured default values from MicroProfile Config.
     * If the DTO specifies maxLoans > 0, that value takes precedence over the
     * configured default (respecting user-specified overrides in the UI).
     *
     * @param configMaxLoans       default max loans from config
     * @param configMembershipYears default membership duration from config
     */
    public Member toEntity(int configMaxLoans, int configMembershipYears) {
        // Use DTO value if explicitly set, otherwise fall back to config default
        int effectiveMaxLoans = maxLoans > 0 ? maxLoans : configMaxLoans;
        Member member = new Member(membershipNumber, firstName, lastName, email,
                effectiveMaxLoans, configMembershipYears);
        member.setPhone(phone);
        member.setAddress(address);
        return member;
    }

    public void updateEntity(Member member) {
        member.setFirstName(this.firstName);
        member.setLastName(this.lastName);
        member.setEmail(this.email);
        member.setPhone(this.phone);
        member.setAddress(this.address);
        if (this.maxLoans > 0) {
            member.setMaxLoans(this.maxLoans);
        }
        if (this.status != null) {
            member.setStatus(this.status);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        MemberDTO memberDTO = (MemberDTO) o;
        return id.equals(memberDTO.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}