package membership.model;

import common.BaseEntity;
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

    @NotBlank(message = "The membership number is required")
    @Size(max = 20)
    @Column(name = "membership_number", nullable = false, unique = true, length = 20)
    private String membershipNumber;

    @NotBlank(message = "The first name is required")
    @Size(max = 100)
    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @NotBlank(message = "The last name is required")
    @Size(max = 100)
    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @NotBlank(message = "The email is required")
    @Email(message = "Invalid email")
    @Column(nullable = false, unique = true)
    private String email;

    @Size(max = 20)
    @Column(length = 20)
    private String phone;

    @Size(max = 255)
    private String address;

    @PastOrPresent
    @Column(name = "registration_date", nullable = false)
    private LocalDate registrationDate = LocalDate.now();

    @Column(name = "expiration_date")
    private LocalDate expirationDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MemberStatus status = MemberStatus.ACTIVE;

    @Min(0)
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
            throw new IllegalStateException("The member cannot borrow more books.");
        }
        activeLoans++;
    }

    public void decrementActiveLoans() {
        if (activeLoans <= 0) {
            throw new IllegalStateException("No active loans to decrement");
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
}
