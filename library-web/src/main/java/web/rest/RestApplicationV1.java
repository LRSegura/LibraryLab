package web.rest;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;
import web.rest.exception.BusinessRuleExceptionMapper;
import web.rest.exception.ConstraintViolationExceptionMapper;
import web.rest.exception.DuplicateEntityExceptionMapper;
import web.rest.exception.EntityNotFoundExceptionMapper;

import java.util.Set;

@ApplicationPath("/api/v1")
public class RestApplicationV1 extends Application {

    @Override
    public Set<Class<?>> getClasses() {
        return Set.of(BookResource.class,
                MemberResource.class,
                CategoryResource.class,
                LoanResource.class,
                DuplicateEntityExceptionMapper.class,
                EntityNotFoundExceptionMapper.class,
                ConstraintViolationExceptionMapper.class,
                BusinessRuleExceptionMapper.class);
    }
}
