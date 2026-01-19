package membership.model;

import common.BaseEntity;
import common.exception.BusinessRuleException;
import common.exception.ExceptionMessage;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "members")
@Getter
@Setter
@NoArgsConstructor
@AttributeOverride(name = "id", column = @Column(name = "member_id"))
public class Member extends BaseEntity {

    @NotBlank(message = "{member.membership-number.required}")
    @Size(max = 20, message = "{member.membership-number.size}")
    @Column(name = "membership_number", nullable = false, unique = true, length = 20)
    private String membershipNumber;

    @NotBlank(message = "{member.first-name.required}")
    @Size(max = 100, message = "{member.first-name.size}")
    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @NotBlank(message = "{member.last-name.required}")
    @Size(max = 100, message = "{member.last-name.size}")
    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @NotBlank(message = "{member.email.required}")
    @Email(message = "{member.email.invalid}")
    @Column(nullable = false, unique = true)
    private String email;

    @Size(max = 20, message = "{member.phone.size}")
    @Column(length = 20)
    private String phone;

    @Size(max = 255, message = "{member.address.size}")
    private String address;

    @PastOrPresent(message = "{member.registration-date.past-or-present}")
    @Column(name = "registration_date", nullable = false)
    private LocalDate registrationDate = LocalDate.now();

    @Column(name = "expiration_date")
    private LocalDate expirationDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MemberStatus status = MemberStatus.ACTIVE;

    @Min(value = 0, message = "{member.active-loans.min}")
    @Column(name = "active_loans", nullable = false)
    private int activeLoans = 0;

    @Column(name = "max_loans", nullable = false)
    private int maxLoans = 5;

    public Member(String membershipNumber, String firstName, String lastName, String email) {
        this.membershipNumber = membershipNumber;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.expirationDate = LocalDate.now().plusYears(1);
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }

    public boolean canBorrow() {
        return status == MemberStatus.ACTIVE
                && activeLoans < maxLoans
                && !isMembershipExpired();
    }

    public boolean isMembershipExpired() {
        return expirationDate != null && LocalDate.now().isAfter(expirationDate);
    }

    public void incrementActiveLoans() {
        if (!canBorrow()) {
            throw new BusinessRuleException(ExceptionMessage.MEMBER_CANNOT_BORROW, getFullName());
        }
        activeLoans++;
    }

    public void decrementActiveLoans() {
        if (activeLoans == 0) {
            throw new BusinessRuleException(ExceptionMessage.MEMBER_NO_ACTIVE_LOANS, getFullName());
        }
        activeLoans--;
    }

    public void renewMembership(int years) {
        LocalDate baseDate = isMembershipExpired() ? LocalDate.now() : expirationDate;
        this.expirationDate = baseDate.plusYears(years);
        if (this.status == MemberStatus.EXPIRED) {
            this.status = MemberStatus.ACTIVE;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        Member member = (Member) o;
        return membershipNumber.equals(member.membershipNumber) && firstName.equals(member.firstName)
                && lastName.equals(member.lastName) && email.equals(member.email);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + membershipNumber.hashCode();
        result = 31 * result + firstName.hashCode();
        result = 31 * result + lastName.hashCode();
        result = 31 * result + email.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "Member{" +
                "membershipNumber='" + membershipNumber + '\'' +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", email='" + email + '\'' +
                ", phone='" + phone + '\'' +
                ", address='" + address + '\'' +
                ", registrationDate=" + registrationDate +
                ", expirationDate=" + expirationDate +
                ", status=" + status +
                ", activeLoans=" + activeLoans +
                ", maxLoans=" + maxLoans +
                "} " + super.toString();
    }
}