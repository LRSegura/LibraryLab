package web.rest;

import jakarta.ws.rs.core.Response;
import membership.dto.MemberDTO;
import membership.model.MemberStatus;
import membership.usecase.MemberService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import web.rest.dto.MemberCreateRequest;
import web.rest.dto.MemberUpdateRequest;
import web.rest.mapper.MemberMapper;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for MemberResource.
 *
 * Verifies HTTP status codes, response bodies, and correct delegation to the
 * service layer. No container or database needed.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MemberResource")
class MemberResourceTest {

    @Mock
    private MemberService memberService;

    @Mock
    private MemberMapper memberMapper;

    @InjectMocks
    private MemberResource memberResource;

    private static MemberDTO aMember() {
        return MemberDTO.builder()
                .id(1L)
                .membershipNumber("MEM-001")
                .firstName("Jane")
                .lastName("Doe")
                .email("jane.doe@example.com")
                .status(MemberStatus.ACTIVE)
                .build();
    }

    @Nested
    @DisplayName("findAll")
    class FindAllTests {

        @Test
        @DisplayName("Should delegate to service and return all members")
        void shouldReturnAllMembers() {
            List<MemberDTO> members = List.of(aMember());
            when(memberService.findAll()).thenReturn(members);

            List<MemberDTO> result = memberResource.findAll();

            assertSame(members, result);
            verify(memberService).findAll();
        }
    }

    @Nested
    @DisplayName("findById")
    class FindByIdTests {

        @Test
        @DisplayName("Should return 200 OK with member when found")
        void shouldReturn200WhenFound() {
            MemberDTO member = aMember();
            when(memberService.findById(1L)).thenReturn(Optional.of(member));

            Response response = memberResource.findById(1L);

            assertAll(
                    () -> assertEquals(200, response.getStatus()),
                    () -> assertSame(member, response.getEntity())
            );
        }

        @Test
        @DisplayName("Should return 404 Not Found when member does not exist")
        void shouldReturn404WhenNotFound() {
            when(memberService.findById(99L)).thenReturn(Optional.empty());

            Response response = memberResource.findById(99L);

            assertEquals(404, response.getStatus());
        }
    }

    @Nested
    @DisplayName("findByMembershipNumber")
    class FindByMembershipNumberTests {

        @Test
        @DisplayName("Should return 200 OK with member when membership number is found")
        void shouldReturn200WhenFound() {
            MemberDTO member = aMember();
            when(memberService.findByMembershipNumber("MEM-001")).thenReturn(Optional.of(member));

            Response response = memberResource.findByMembershipNumber("MEM-001");

            assertAll(
                    () -> assertEquals(200, response.getStatus()),
                    () -> assertSame(member, response.getEntity())
            );
        }

        @Test
        @DisplayName("Should return 404 Not Found when membership number is not found")
        void shouldReturn404WhenNotFound() {
            when(memberService.findByMembershipNumber("MEM-999")).thenReturn(Optional.empty());

            Response response = memberResource.findByMembershipNumber("MEM-999");

            assertEquals(404, response.getStatus());
        }
    }

    @Nested
    @DisplayName("findByEmail")
    class FindByEmailTests {

        @Test
        @DisplayName("Should return 200 OK with member when email is found")
        void shouldReturn200WhenFound() {
            MemberDTO member = aMember();
            when(memberService.findByEmail("jane.doe@example.com")).thenReturn(Optional.of(member));

            Response response = memberResource.findByEmail("jane.doe@example.com");

            assertAll(
                    () -> assertEquals(200, response.getStatus()),
                    () -> assertSame(member, response.getEntity())
            );
        }

        @Test
        @DisplayName("Should return 404 Not Found when email is not found")
        void shouldReturn404WhenNotFound() {
            when(memberService.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

            Response response = memberResource.findByEmail("unknown@example.com");

            assertEquals(404, response.getStatus());
        }
    }

    @Nested
    @DisplayName("create")
    class CreateTests {

        @Test
        @DisplayName("Should return 201 Created with the saved member in the body")
        void shouldReturn201WithCreatedMember() {
            MemberCreateRequest request = new MemberCreateRequest();
            MemberDTO mappedDto = MemberDTO.builder().membershipNumber("MEM-001").firstName("Jane").lastName("Doe").email("jane@example.com").build();
            MemberDTO createdDto = aMember();

            when(memberMapper.toDto(request)).thenReturn(mappedDto);
            when(memberService.create(mappedDto)).thenReturn(createdDto);

            Response response = memberResource.create(request);

            assertAll(
                    () -> assertEquals(201, response.getStatus()),
                    () -> assertSame(createdDto, response.getEntity())
            );
            verify(memberService).create(mappedDto);
        }
    }

    @Nested
    @DisplayName("update")
    class UpdateTests {

        @Test
        @DisplayName("Should return 200 OK with the updated member in the body")
        void shouldReturn200WithUpdatedMember() {
            MemberUpdateRequest request = new MemberUpdateRequest();
            MemberDTO mappedDto = MemberDTO.builder().id(1L).firstName("Jane").lastName("Doe").email("jane@example.com").build();
            MemberDTO updatedDto = aMember();

            when(memberMapper.toDto(1L, request)).thenReturn(mappedDto);
            when(memberService.update(mappedDto)).thenReturn(updatedDto);

            Response response = memberResource.update(1L, request);

            assertAll(
                    () -> assertEquals(200, response.getStatus()),
                    () -> assertSame(updatedDto, response.getEntity())
            );
        }
    }

    @Nested
    @DisplayName("delete")
    class DeleteTests {

        @Test
        @DisplayName("Should return 204 No Content and delegate to service")
        void shouldReturn204() {
            Response response = memberResource.delete(1L);

            assertEquals(204, response.getStatus());
            verify(memberService).delete(1L);
        }
    }

    @Nested
    @DisplayName("suspend")
    class SuspendTests {

        @Test
        @DisplayName("Should return 200 OK with the suspended member in the body")
        void shouldReturn200WithSuspendedMember() {
            MemberDTO suspended = MemberDTO.builder().id(1L).status(MemberStatus.SUSPENDED).build();
            when(memberService.suspend(1L)).thenReturn(suspended);

            Response response = memberResource.suspend(1L);

            assertAll(
                    () -> assertEquals(200, response.getStatus()),
                    () -> assertSame(suspended, response.getEntity())
            );
        }
    }

    @Nested
    @DisplayName("activate")
    class ActivateTests {

        @Test
        @DisplayName("Should return 200 OK with the activated member in the body")
        void shouldReturn200WithActivatedMember() {
            MemberDTO activated = MemberDTO.builder().id(1L).status(MemberStatus.ACTIVE).build();
            when(memberService.activate(1L)).thenReturn(activated);

            Response response = memberResource.activate(1L);

            assertAll(
                    () -> assertEquals(200, response.getStatus()),
                    () -> assertSame(activated, response.getEntity())
            );
        }
    }

    @Nested
    @DisplayName("renewMembership")
    class RenewMembershipTests {

        @Test
        @DisplayName("Should return 200 OK with the renewed member in the body")
        void shouldReturn200WithRenewedMember() {
            MemberDTO renewed = aMember();
            when(memberService.renewMembership(1L, 1)).thenReturn(renewed);

            Response response = memberResource.renewMembership(1L, 1);

            assertAll(
                    () -> assertEquals(200, response.getStatus()),
                    () -> assertSame(renewed, response.getEntity())
            );
        }
    }

    @Nested
    @DisplayName("generateMembershipNumber")
    class GenerateMembershipNumberTests {

        @Test
        @DisplayName("Should return 200 OK with the generated membership number")
        void shouldReturn200WithGeneratedNumber() {
            when(memberService.generateMembershipNumber()).thenReturn("MEM-2026-001");

            Response response = memberResource.generateMembershipNumber();

            assertAll(
                    () -> assertEquals(200, response.getStatus()),
                    () -> assertEquals("MEM-2026-001", response.getEntity())
            );
        }
    }
}
