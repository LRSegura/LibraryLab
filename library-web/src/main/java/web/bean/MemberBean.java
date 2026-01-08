package web.bean;

import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
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
public class MemberBean implements Serializable {


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
                addMessage(FacesMessage.SEVERITY_INFO, "Success", "Member updated successfully");
            } else {
                memberService.create(newMember);
                addMessage(FacesMessage.SEVERITY_INFO, "Success", "Member created successfully");
                initNewMember();
            }
            loadMembers();
        } catch (Exception e) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Error", e.getMessage());
        }
    }

    public void delete(MemberDTO member) {
        try {
            memberService.delete(member.getId());
            addMessage(FacesMessage.SEVERITY_INFO, "Success", "Member deleted successfully");
            loadMembers();
        } catch (Exception e) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Error", e.getMessage());
        }
    }

    public void suspend(MemberDTO member) {
        try {
            memberService.suspend(member.getId());
            addMessage(FacesMessage.SEVERITY_INFO, "Success", "Member suspended");
            loadMembers();
        } catch (Exception e) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Error", e.getMessage());
        }
    }

    public void activate(MemberDTO member) {
        try {
            memberService.activate(member.getId());
            addMessage(FacesMessage.SEVERITY_INFO, "Success", "Member activated");
            loadMembers();
        } catch (Exception e) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Error", e.getMessage());
        }
    }

    public void renewMembership(MemberDTO member) {
        try {
            memberService.renewMembership(member.getId(), 1);
            addMessage(FacesMessage.SEVERITY_INFO, "Success", "Membership renewed for 1 year");
            loadMembers();
        } catch (Exception e) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Error", e.getMessage());
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

    private void addMessage(FacesMessage.Severity severity, String summary, String detail) {
        FacesContext.getCurrentInstance().addMessage(null, 
            new FacesMessage(severity, summary, detail));
    }
}
