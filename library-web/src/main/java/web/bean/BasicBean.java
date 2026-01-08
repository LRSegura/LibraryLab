package web.bean;

import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;

public abstract class BasicBean {

    protected void addMessage(String summary, String detail, FacesMessage.Severity severity) {
        FacesMessage message = new FacesMessage(summary, detail);
        message.setSeverity(severity);
        getFacesContext().addMessage(null, message);
    }

    protected void addInfoMessage(String detailMessage) {
        addMessage(SummaryValues.INFO.getDescription(), detailMessage, FacesMessage.SEVERITY_INFO);
    }

    protected void addInfoMessage(String summary, String detail) {
        addMessage(summary, detail, FacesMessage.SEVERITY_INFO);
    }

    protected void addErrorMessage(String errorDetail) {
        addMessage(SummaryValues.ERROR.getDescription(), errorDetail, FacesMessage.SEVERITY_ERROR);
    }

    protected void addWarnMessage(String warningDetail) {
        addMessage(SummaryValues.WARNING.getDescription(), warningDetail, FacesMessage.SEVERITY_WARN);
    }

    protected FacesContext getFacesContext() {
        return FacesContext.getCurrentInstance();
    }

    protected void executeScript(String script) {
        org.primefaces.PrimeFaces.current().executeScript(script);
    }
}
