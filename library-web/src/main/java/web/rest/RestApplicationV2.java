package web.rest;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

import java.util.Set;

@ApplicationPath("/api/v2")
public class RestApplicationV2 extends Application {

    @Override
    public Set<Class<?>> getClasses() {
        return Set.of(ApplicationState.class);
    }
}
