package web.rest;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import membership.dto.MemberDTO;
import membership.model.MemberStatus;
import membership.usecase.MemberService;
import web.rest.dto.MemberCreateRequest;
import web.rest.dto.MemberUpdateRequest;
import web.rest.mapper.MemberMapper;

import java.util.List;

@Path("/members")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class MemberResource {

    private MemberService memberService;
    private MemberMapper memberMapper;

    public MemberResource() {
    }

    @Inject
    public MemberResource(MemberService memberService, MemberMapper memberMapper) {
        this.memberService = memberService;
        this.memberMapper = memberMapper;
    }

    @GET
    public List<MemberDTO> findAll() {
        return memberService.findAll();
    }

    @GET
    @Path("/{id}")
    public Response findById(@PathParam("id") Long id) {
        return memberService.findById(id)
                .map(Response::ok)
                .orElse(Response.status(Response.Status.NOT_FOUND))
                .build();
    }

    @GET
    @Path("/membership/{membershipNumber}")
    public Response findByMembershipNumber(@PathParam("membershipNumber") String membershipNumber) {
        return memberService.findByMembershipNumber(membershipNumber)
                .map(Response::ok)
                .orElse(Response.status(Response.Status.NOT_FOUND))
                .build();
    }

    @GET
    @Path("/email/{email}")
    public Response findByEmail(@PathParam("email") String email) {
        return memberService.findByEmail(email)
                .map(Response::ok)
                .orElse(Response.status(Response.Status.NOT_FOUND))
                .build();
    }

    @GET
    @Path("/status/{status}")
    public List<MemberDTO> findByStatus(@PathParam("status") MemberStatus status) {
        return memberService.findByStatus(status);
    }

    @GET
    @Path("/search/name")
    public List<MemberDTO> findByName(@QueryParam("q") String name) {
        return memberService.findByName(name);
    }

    @POST
    public Response create(@Valid MemberCreateRequest request) {
        MemberDTO dto = memberMapper.toDto(request);
        MemberDTO created = memberService.create(dto);
        return Response.status(Response.Status.CREATED).entity(created).build();
    }

    @PUT
    @Path("/{id}")
    public Response update(@PathParam("id") Long id, @Valid MemberUpdateRequest request) {
        MemberDTO dto = memberMapper.toDto(id, request);
        MemberDTO updated = memberService.update(dto);
        return Response.ok(updated).build();
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") Long id) {
        memberService.delete(id);
        return Response.noContent().build();
    }

    @POST
    @Path("/{id}/suspend")
    public Response suspend(@PathParam("id") Long id) {
        MemberDTO member = memberService.suspend(id);
        return Response.ok(member).build();
    }

    @POST
    @Path("/{id}/activate")
    public Response activate(@PathParam("id") Long id) {
        MemberDTO member = memberService.activate(id);
        return Response.ok(member).build();
    }

    @POST
    @Path("/{id}/renew")
    public Response renewMembership(@PathParam("id") Long id, @QueryParam("years") @DefaultValue("1") int years) {
        MemberDTO member = memberService.renewMembership(id, years);
        return Response.ok(member).build();
    }

    @GET
    @Path("/generate-membership-number")
    public Response generateMembershipNumber() {
        String membershipNumber = memberService.generateMembershipNumber();
        return Response.ok(membershipNumber).build();
    }
}
