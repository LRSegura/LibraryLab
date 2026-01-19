package web.bean;

import common.exception.BusinessRuleException;
import common.exception.DuplicateEntityException;
import common.exception.EntityNotFoundException;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.validation.ConstraintViolationException;

import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class BasicBean {


    protected void executeOperation(Runnable operation, String taskDescription, Logger logger) {
        try {
            operation.run();
        } catch (ConstraintViolationException exception) {
            exception.getConstraintViolations().forEach(violation -> addErrorMessage(violation.getMessage()));
        } catch (BusinessRuleException businessRuleException) {
            addErrorMessage(businessRuleException.getMessage());
        } catch (Exception exception) {
            if(exception instanceof EntityNotFoundException){
                logError(logger, "Entity not found: " + taskDescription, exception);
            }
            if(exception instanceof DuplicateEntityException){
                logError(logger, "Duplicate entity: " + taskDescription, exception);
            }
            addErrorMessage("An error occurred while executing operation: " + taskDescription);
        }
    }

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

    protected void logEntities(Collection<?> entities, Logger logger){
        String message = "Logging entities: " + entities.size() + " entities";
        logger.info(message);
        entities.forEach(entity -> logger.info(entity.toString()));
    }

    protected void logError(Logger logger, String message, Exception exception){
        logger.log(Level.SEVERE, message, exception);
    }

}
