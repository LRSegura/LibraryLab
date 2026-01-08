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
import java.util.List;

@Named
@ViewScoped
@Getter
@Setter
public class MemberBean extends BasicBean implements Serializable {


    private MemberService memberService;

    private List<MemberDTO> members;
    private List<MemberDTO> filteredMembers;
    private MemberDTO selectedMember;
    private MemberDTO newMember;
    private boolean editMode;

    @Inject
    public MemberBean(MemberService memberService) {
        this.memberService = memberService;
    }

    public MemberBean(){
        //Required by proxy
    }

    @PostConstruct
    public void init() {
        loadMembers();
        initNewMember();
    }

    public void loadMembers() {
        members = memberService.findAll();
    }

    public void initNewMember() {
        newMember = MemberDTO.builder()
                .membershipNumber(memberService.generateMembershipNumber())
                .maxLoans(5)
                .build();
        editMode = false;
    }

    public void prepareEdit(MemberDTO member) {
        this.selectedMember = MemberDTO.builder()
                .id(member.getId())
                .membershipNumber(member.getMembershipNumber())
                .firstName(member.getFirstName())
                .lastName(member.getLastName())
                .email(member.getEmail())
                .phone(member.getPhone())
                .address(member.getAddress())
                .status(member.getStatus())
                .maxLoans(member.getMaxLoans())
                .build();
        editMode = true;
    }

    public void save() {
        try {
            if (editMode && selectedMember != null) {
                memberService.update(selectedMember.getId(), selectedMember);
                addInfoMessage(SummaryValues.SUCCESS.getDescription(), "Member updated successfully");
            } else {
                memberService.create(newMember);
                addInfoMessage(SummaryValues.SUCCESS.getDescription(), "Member created successfully");
                initNewMember();
            }
            loadMembers();
        } catch (Exception e) {
            addErrorMessage("Error saving member.");
        }
    }

    public void delete(MemberDTO member) {
        try {
            memberService.delete(member.getId());
            addInfoMessage(SummaryValues.SUCCESS.getDescription(), "Member deleted successfully");
            loadMembers();
        } catch (Exception e) {
            addErrorMessage("Error deleting member.");
        }
    }

    public void suspend(MemberDTO member) {
        try {
            memberService.suspend(member.getId());
            addInfoMessage(SummaryValues.SUCCESS.getDescription(), "Member suspended");
            loadMembers();
        } catch (Exception e) {
            addErrorMessage("Error suspending member.");
            addErrorMessage("Error suspending member.");
        }
    }

    public void activate(MemberDTO member) {
        try {
            memberService.activate(member.getId());
            addInfoMessage(SummaryValues.SUCCESS.getDescription(), "Member activated");
            loadMembers();
        } catch (Exception e) {
            addErrorMessage("Error activating member.");
        }
    }

    public void renewMembership(MemberDTO member) {
        try {
            memberService.renewMembership(member.getId(), 1);
            addInfoMessage(SummaryValues.SUCCESS.getDescription(), "Membership renewed for 1 year");
            loadMembers();
        } catch (Exception e) {
            addErrorMessage("Error renewing membership.");
        }
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
