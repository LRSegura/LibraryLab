package web.rest;

import lending.dto.LoanDTO;
import lending.model.LoanStatus;
import lending.usecase.LoanService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.time.LocalDate;
import java.util.List;

@Path("/loans")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class LoanResource {

    private LoanService loanService;

    public LoanResource() {
    }

    @Inject
    public LoanResource(LoanService loanService) {
        this.loanService = loanService;
    }

    @GET
    public List<LoanDTO> findAll() {
        return loanService.findAll();
    }

    @GET
    @Path("/{id}")
    public Response findById(@PathParam("id") Long id) {
        return loanService.findById(id)
                .map(Response::ok)
                .orElse(Response.status(Response.Status.NOT_FOUND))
                .build();
    }

    @GET
    @Path("/member/{memberId}")
    public List<LoanDTO> findByMember(@PathParam("memberId") Long memberId) {
        return loanService.findByMember(memberId);
    }

    @GET
    @Path("/member/{memberId}/active")
    public List<LoanDTO> findActiveByMember(@PathParam("memberId") Long memberId) {
        return loanService.findActiveByMember(memberId);
    }

    @GET
    @Path("/book/{bookId}")
    public List<LoanDTO> findByBook(@PathParam("bookId") Long bookId) {
        return loanService.findByBook(bookId);
    }

    @GET
    @Path("/status/{status}")
    public List<LoanDTO> findByStatus(@PathParam("status") LoanStatus status) {
        return loanService.findByStatus(status);
    }

    @GET
    @Path("/overdue")
    public List<LoanDTO> findOverdueLoans() {
        return loanService.findOverdueLoans();
    }

    @GET
    @Path("/due-before")
    public List<LoanDTO> findLoansDueBefore(@QueryParam("date") String dateStr) {
        LocalDate date = LocalDate.parse(dateStr);
        return loanService.findLoansDueBefore(date);
    }

    @POST
    @Path("/borrow")
    public Response borrowBook(
            @QueryParam("bookId") Long bookId,
            @QueryParam("memberId") Long memberId,
            @QueryParam("notes") String notes) {
        LoanDTO loan = loanService.borrowBook(bookId, memberId, notes);
        return Response.status(Response.Status.CREATED).entity(loan).build();
    }

    @POST
    @Path("/{id}/return")
    public Response returnBook(@PathParam("id") Long id) {
        LoanDTO loan = loanService.returnBook(id);
        return Response.ok(loan).build();
    }

    @POST
    @Path("/{id}/renew")
    public Response renewLoan(@PathParam("id") Long id) {
        LoanDTO loan = loanService.renewLoan(id);
        return Response.ok(loan).build();
    }

    @POST
    @Path("/{id}/lost")
    public Response markAsLost(@PathParam("id") Long id) {
        LoanDTO loan = loanService.markAsLost(id);
        return Response.ok(loan).build();
    }

    @POST
    @Path("/update-overdue-status")
    public Response updateOverdueStatus() {
        loanService.updateOverdueStatus();
        return Response.ok().build();
    }
}
