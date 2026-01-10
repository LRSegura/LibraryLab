package web.bean;

import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.Setter;
import membership.dto.MemberDTO;
import membership.model.MemberStatus;
import membership.usecase.MemberService;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Named
@ViewScoped
@Getter
@Setter
public class MemberBean extends BasicBean implements Serializable {

    private MemberService memberService;

    private List<MemberDTO> members;
    private List<MemberDTO> filteredMembers;
    private MemberDTO currentMember;

    @Inject
    public MemberBean(MemberService memberService) {
        this.memberService = memberService;
    }

    public MemberBean() {
        //Required by proxy
    }

    @PostConstruct
    public void init() {
        loadMembers();
        initNewMember();
    }

    public void loadMembers() {
        members = new ArrayList<>(memberService.findAll());
    }

    public void initNewMember() {
        currentMember = MemberDTO.builder()
                .membershipNumber(memberService.generateMembershipNumber())
                .maxLoans(5)
                .build();
    }

    public void save() {
        Runnable operation = () -> {
            memberService.create(currentMember);
            addInfoMessage(SummaryValues.SUCCESS.getDescription(), "Member created successfully");
            initNewMember();
            loadMembers();
        };
        executeOperation(operation, "Saving member");
    }

    public void update() {
        Runnable operation = () -> {
            memberService.update(currentMember);
            addInfoMessage(SummaryValues.SUCCESS.getDescription(), "Member updated successfully");
            loadMembers();
        };
        executeOperation(operation, "Updating member");
    }

    public void delete(MemberDTO member) {
        Runnable operation = () -> {
            memberService.delete(member.getId());
            addInfoMessage(SummaryValues.SUCCESS.getDescription(), "Member deleted successfully");
            loadMembers();
        };
        executeOperation(operation, "Deleting member");
    }

    public void suspend(MemberDTO member) {
        Runnable operation = () -> {
            memberService.suspend(member.getId());
            addInfoMessage(SummaryValues.SUCCESS.getDescription(), "Member suspended");
            loadMembers();
        };
        executeOperation(operation, "Suspending member");
    }

    public void activate(MemberDTO member) {
        Runnable operation = () -> {
            memberService.activate(member.getId());
            addInfoMessage(SummaryValues.SUCCESS.getDescription(), "Member activated");
            loadMembers();
        };
        executeOperation(operation, "Activating member");
    }

    public void renewMembership(MemberDTO member) {
        Runnable operation = () -> {
            memberService.renewMembership(member.getId(), 1);
            addInfoMessage(SummaryValues.SUCCESS.getDescription(), "Membership renewed for 1 year");
            loadMembers();
        };
        executeOperation(operation, "Renewing membership");
    }

    public MemberStatus[] getStatuses() {
        return MemberStatus.values();
    }

    public String getStatusSeverity(MemberStatus status) {
        return switch (status) {
            case ACTIVE -> "success";
            case SUSPENDED -> "warning";
            case EXPIRED -> "danger";
            case INACTIVE -> "secondary";
        };
    }
}