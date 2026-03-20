package membership.usecase;

import common.TestServiceHelper;
import common.exception.BusinessRuleException;
import common.exception.DuplicateEntityException;
import common.exception.EntityNotFoundException;
import jakarta.validation.Validator;
import membership.dto.MemberDTO;
import membership.model.Member;
import membership.model.MemberStatus;
import membership.port.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for MemberService.
 *
 * Covers the full member lifecycle: registration, profile update, removal,
 * status transitions (suspend / activate), and membership renewal.
 *
 * Key design note: MemberService.update() relies on JPA dirty-checking to
 * persist changes — it does NOT call memberRepository.update() explicitly.
 * Tests for update() therefore verify the returned DTO, not repository calls.
 *
 * Tested methods:
 *   - create(): email and membership-number uniqueness, save
 *   - update(): not-found, email collision, successful update
 *   - delete(): not-found, active-loans guard
 *   - suspend(): not-found, status transition
 *   - activate(): not-found, expired membership guard, status transition
 *   - renewMembership(): not-found, extend from current expiration, extend from today when expired
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MemberService")
class MemberServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private Validator validator;

    @InjectMocks
    private MemberService memberService;

    @BeforeEach
    void injectValidator() {
        TestServiceHelper.injectValidator(memberService, validator);
    }

    // =========================================================================
    // create()
    // =========================================================================

    @Nested
    @DisplayName("create()")
    class CreateTests {

        @Test
        @DisplayName("Should throw DuplicateEntityException and never save when email already exists")
        void shouldThrowWhenEmailAlreadyExists() {
            when(memberRepository.existsByEmail("john@example.com")).thenReturn(true);

            MemberDTO dto = MemberDTO.builder()
                    .membershipNumber("MEM-001")
                    .firstName("John")
                    .lastName("Doe")
                    .email("john@example.com")
                    .build();

            assertThrows(DuplicateEntityException.class, () -> memberService.create(dto));
            verify(memberRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw DuplicateEntityException and never save when membership number already exists")
        void shouldThrowWhenMembershipNumberAlreadyExists() {
            // Email passes but membership number is already taken
            when(memberRepository.existsByEmail(any())).thenReturn(false);
            when(memberRepository.existsByMembershipNumber("MEM-001")).thenReturn(true);

            MemberDTO dto = MemberDTO.builder()
                    .membershipNumber("MEM-001")
                    .firstName("John")
                    .lastName("Doe")
                    .email("john@example.com")
                    .build();

            assertThrows(DuplicateEntityException.class, () -> memberService.create(dto));
            verify(memberRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should save member and return DTO when both email and membership number are unique")
        void shouldSaveMemberWhenUnique() {
            when(memberRepository.existsByEmail(any())).thenReturn(false);
            when(memberRepository.existsByMembershipNumber(any())).thenReturn(false);

            MemberDTO dto = MemberDTO.builder()
                    .membershipNumber("MEM-001")
                    .firstName("John")
                    .lastName("Doe")
                    .email("john@example.com")
                    .build();

            MemberDTO result = memberService.create(dto);

            assertAll("Created member DTO",
                    () -> assertNotNull(result),
                    () -> assertEquals("john@example.com", result.getEmail()),
                    () -> assertEquals("MEM-001", result.getMembershipNumber()),
                    () -> assertEquals("John Doe", result.getFullName())
            );
            verify(memberRepository).save(any(Member.class));
        }
    }

    // =========================================================================
    // update()
    // =========================================================================

    @Nested
    @DisplayName("update()")
    class UpdateTests {

        private Member existingMember;

        @BeforeEach
        void setUp() {
            existingMember = new Member("MEM-001", "John", "Doe", "john@example.com");
            TestServiceHelper.setEntityId(existingMember, 1L);
        }

        @Test
        @DisplayName("Should throw EntityNotFoundException when member ID does not exist")
        void shouldThrowWhenMemberNotFound() {
            when(memberRepository.findById(1L)).thenReturn(Optional.empty());

            MemberDTO dto = MemberDTO.builder().id(1L).membershipNumber("MEM-001")
                    .firstName("John").lastName("Doe").email("john@example.com").build();

            assertThrows(EntityNotFoundException.class, () -> memberService.update(dto));
        }

        @Test
        @DisplayName("Should throw DuplicateEntityException when email is already held by a different member")
        void shouldThrowWhenEmailTakenByDifferentMember() {
            Member other = new Member("MEM-002", "Jane", "Smith", "john@example.com");
            TestServiceHelper.setEntityId(other, 2L); // different ID → conflict

            when(memberRepository.findById(1L)).thenReturn(Optional.of(existingMember));
            when(memberRepository.findByEmail("john@example.com")).thenReturn(Optional.of(other));

            MemberDTO dto = MemberDTO.builder().id(1L).membershipNumber("MEM-001")
                    .firstName("John").lastName("Doe").email("john@example.com").build();

            assertThrows(DuplicateEntityException.class, () -> memberService.update(dto));
        }

        @Test
        @DisplayName("Should update successfully when email belongs to the same member (no collision)")
        void shouldUpdateWhenEmailBelongsToSameMember() {
            // Email lookup returns the same entity — keeping one's own email is allowed
            when(memberRepository.findById(1L)).thenReturn(Optional.of(existingMember));
            when(memberRepository.findByEmail("john@example.com")).thenReturn(Optional.of(existingMember));

            MemberDTO dto = MemberDTO.builder()
                    .id(1L)
                    .membershipNumber("MEM-001")
                    .firstName("John")
                    .lastName("Doe Updated")
                    .email("john@example.com")
                    .build();

            MemberDTO result = memberService.update(dto);

            assertAll("Updated member",
                    () -> assertNotNull(result),
                    () -> assertEquals("Doe Updated", result.getLastName()),
                    () -> assertEquals("john@example.com", result.getEmail())
            );
            // update() relies on JPA dirty-checking — no explicit memberRepository.update() call
        }
    }

    // =========================================================================
    // delete()
    // =========================================================================

    @Nested
    @DisplayName("delete()")
    class DeleteTests {

        @Test
        @DisplayName("Should throw EntityNotFoundException when member ID does not exist")
        void shouldThrowWhenMemberNotFound() {
            when(memberRepository.findById(1L)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class, () -> memberService.delete(1L));
        }

        @Test
        @DisplayName("Should throw BusinessRuleException and never delete when member has active loans")
        void shouldThrowWhenMemberHasActiveLoans() {
            Member member = new Member("MEM-001", "John", "Doe", "john@example.com");
            member.setActiveLoans(2); // outstanding loans prevent deletion

            when(memberRepository.findById(1L)).thenReturn(Optional.of(member));

            assertThrows(BusinessRuleException.class, () -> memberService.delete(1L));
            verify(memberRepository, never()).delete(any());
        }

        @Test
        @DisplayName("Should delete member when no active loans exist")
        void shouldDeleteMemberWhenNoActiveLoans() {
            Member member = new Member("MEM-001", "John", "Doe", "john@example.com");
            // activeLoans defaults to 0

            when(memberRepository.findById(1L)).thenReturn(Optional.of(member));

            assertDoesNotThrow(() -> memberService.delete(1L));
            verify(memberRepository).delete(member);
        }
    }

    // =========================================================================
    // suspend()
    // =========================================================================

    @Nested
    @DisplayName("suspend()")
    class SuspendTests {

        @Test
        @DisplayName("Should throw EntityNotFoundException when member ID does not exist")
        void shouldThrowWhenMemberNotFound() {
            when(memberRepository.findById(1L)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class, () -> memberService.suspend(1L));
        }

        @Test
        @DisplayName("Should set member status to SUSPENDED")
        void shouldSuspendMember() {
            Member member = new Member("MEM-001", "John", "Doe", "john@example.com");
            when(memberRepository.findById(1L)).thenReturn(Optional.of(member));

            MemberDTO result = memberService.suspend(1L);

            assertEquals(MemberStatus.SUSPENDED, result.getStatus());
        }
    }

    // =========================================================================
    // activate()
    // =========================================================================

    @Nested
    @DisplayName("activate()")
    class ActivateTests {

        @Test
        @DisplayName("Should throw EntityNotFoundException when member ID does not exist")
        void shouldThrowWhenMemberNotFound() {
            when(memberRepository.findById(1L)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class, () -> memberService.activate(1L));
        }

        @Test
        @DisplayName("Should throw BusinessRuleException when membership is expired")
        void shouldThrowWhenMembershipExpired() {
            Member member = new Member("MEM-001", "John", "Doe", "john@example.com");
            member.setExpirationDate(LocalDate.now().minusDays(1)); // expired yesterday

            when(memberRepository.findById(1L)).thenReturn(Optional.of(member));

            assertThrows(BusinessRuleException.class, () -> memberService.activate(1L),
                    "An expired member must renew their membership before being re-activated");
        }

        @Test
        @DisplayName("Should set member status to ACTIVE when membership is still valid")
        void shouldActivateMemberWhenMembershipValid() {
            Member member = new Member("MEM-001", "John", "Doe", "john@example.com");
            member.setStatus(MemberStatus.SUSPENDED);
            // expirationDate defaults to 1 year from now — still valid

            when(memberRepository.findById(1L)).thenReturn(Optional.of(member));

            MemberDTO result = memberService.activate(1L);

            assertEquals(MemberStatus.ACTIVE, result.getStatus());
        }
    }

    // =========================================================================
    // renewMembership()
    // =========================================================================

    @Nested
    @DisplayName("renewMembership()")
    class RenewMembershipTests {

        @Test
        @DisplayName("Should throw EntityNotFoundException when member ID does not exist")
        void shouldThrowWhenMemberNotFound() {
            when(memberRepository.findById(1L)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class, () -> memberService.renewMembership(1L, 1));
        }

        @Test
        @DisplayName("Should extend membership from the current expiration date when still active")
        void shouldExtendMembershipFromCurrentExpiration() {
            Member member = new Member("MEM-001", "John", "Doe", "john@example.com");
            // expirationDate defaults to 1 year from now
            LocalDate currentExpiration = member.getExpirationDate();

            when(memberRepository.findById(1L)).thenReturn(Optional.of(member));

            MemberDTO result = memberService.renewMembership(1L, 2);

            // Member.renewMembership() extends from the existing expiration, not from today
            assertEquals(currentExpiration.plusYears(2), result.getExpirationDate(),
                    "Renewal should stack on top of the remaining membership, not reset from today");
        }

        @Test
        @DisplayName("Should renew from today when membership is already expired")
        void shouldRenewFromTodayWhenExpired() {
            Member member = new Member("MEM-001", "John", "Doe", "john@example.com");
            member.setExpirationDate(LocalDate.now().minusDays(10)); // expired 10 days ago

            when(memberRepository.findById(1L)).thenReturn(Optional.of(member));

            MemberDTO result = memberService.renewMembership(1L, 1);

            // Member.renewMembership() uses today as the base when the membership has lapsed
            assertEquals(LocalDate.now().plusYears(1), result.getExpirationDate());
        }
    }
}
