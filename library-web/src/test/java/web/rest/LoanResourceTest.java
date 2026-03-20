package web.rest;

import jakarta.ws.rs.core.Response;
import lending.dto.LoanDTO;
import lending.model.LoanStatus;
import lending.usecase.LoanService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for LoanResource.
 *
 * Verifies HTTP status codes, response bodies, and correct delegation to the
 * service layer. No container or database needed.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("LoanResource")
class LoanResourceTest {

    @Mock
    private LoanService loanService;

    @InjectMocks
    private LoanResource loanResource;

    private static LoanDTO aLoan() {
        return LoanDTO.builder()
                .id(1L)
                .bookId(10L)
                .memberId(20L)
                .status(LoanStatus.ACTIVE)
                .build();
    }

    @Nested
    @DisplayName("findAll")
    class FindAllTests {

        @Test
        @DisplayName("Should delegate to service and return all loans")
        void shouldReturnAllLoans() {
            List<LoanDTO> loans = List.of(aLoan());
            when(loanService.findAll()).thenReturn(loans);

            List<LoanDTO> result = loanResource.findAll();

            assertSame(loans, result);
            verify(loanService).findAll();
        }
    }

    @Nested
    @DisplayName("findById")
    class FindByIdTests {

        @Test
        @DisplayName("Should return 200 OK with loan when found")
        void shouldReturn200WhenFound() {
            LoanDTO loan = aLoan();
            when(loanService.findById(1L)).thenReturn(Optional.of(loan));

            Response response = loanResource.findById(1L);

            assertAll(
                    () -> assertEquals(200, response.getStatus()),
                    () -> assertSame(loan, response.getEntity())
            );
        }

        @Test
        @DisplayName("Should return 404 Not Found when loan does not exist")
        void shouldReturn404WhenNotFound() {
            when(loanService.findById(99L)).thenReturn(Optional.empty());

            Response response = loanResource.findById(99L);

            assertEquals(404, response.getStatus());
        }
    }

    @Nested
    @DisplayName("borrowBook")
    class BorrowBookTests {

        @Test
        @DisplayName("Should return 201 Created with the new loan in the body")
        void shouldReturn201WithCreatedLoan() {
            LoanDTO loan = aLoan();
            when(loanService.borrowBook(10L, 20L, null)).thenReturn(loan);

            Response response = loanResource.borrowBook(10L, 20L, null);

            assertAll(
                    () -> assertEquals(201, response.getStatus()),
                    () -> assertSame(loan, response.getEntity())
            );
            verify(loanService).borrowBook(10L, 20L, null);
        }

        @Test
        @DisplayName("Should pass notes to service when provided")
        void shouldPassNotesToService() {
            LoanDTO loan = aLoan();
            when(loanService.borrowBook(10L, 20L, "Handle with care")).thenReturn(loan);

            Response response = loanResource.borrowBook(10L, 20L, "Handle with care");

            assertEquals(201, response.getStatus());
            verify(loanService).borrowBook(10L, 20L, "Handle with care");
        }
    }

    @Nested
    @DisplayName("returnBook")
    class ReturnBookTests {

        @Test
        @DisplayName("Should return 200 OK with the returned loan in the body")
        void shouldReturn200WithReturnedLoan() {
            LoanDTO returned = LoanDTO.builder().id(1L).status(LoanStatus.RETURNED).build();
            when(loanService.returnBook(1L)).thenReturn(returned);

            Response response = loanResource.returnBook(1L);

            assertAll(
                    () -> assertEquals(200, response.getStatus()),
                    () -> assertSame(returned, response.getEntity())
            );
            verify(loanService).returnBook(1L);
        }
    }

    @Nested
    @DisplayName("renewLoan")
    class RenewLoanTests {

        @Test
        @DisplayName("Should return 200 OK with the renewed loan in the body")
        void shouldReturn200WithRenewedLoan() {
            LoanDTO renewed = LoanDTO.builder().id(1L).renewalCount(1).status(LoanStatus.ACTIVE).build();
            when(loanService.renewLoan(1L)).thenReturn(renewed);

            Response response = loanResource.renewLoan(1L);

            assertAll(
                    () -> assertEquals(200, response.getStatus()),
                    () -> assertSame(renewed, response.getEntity())
            );
            verify(loanService).renewLoan(1L);
        }
    }

    @Nested
    @DisplayName("markAsLost")
    class MarkAsLostTests {

        @Test
        @DisplayName("Should return 200 OK with the updated loan in the body")
        void shouldReturn200WithLostLoan() {
            LoanDTO lost = LoanDTO.builder().id(1L).status(LoanStatus.LOST).build();
            when(loanService.markAsLost(1L)).thenReturn(lost);

            Response response = loanResource.markAsLost(1L);

            assertAll(
                    () -> assertEquals(200, response.getStatus()),
                    () -> assertSame(lost, response.getEntity())
            );
            verify(loanService).markAsLost(1L);
        }
    }

    @Nested
    @DisplayName("updateOverdueStatus")
    class UpdateOverdueStatusTests {

        @Test
        @DisplayName("Should return 200 OK and delegate batch update to service")
        void shouldReturn200AndDelegateToService() {
            Response response = loanResource.updateOverdueStatus();

            assertEquals(200, response.getStatus());
            verify(loanService).updateOverdueStatus();
        }
    }
}
