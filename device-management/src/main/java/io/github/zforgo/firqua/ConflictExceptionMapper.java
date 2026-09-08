package io.github.zforgo.firqua;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import io.github.zforgo.firqua.common.ConflictException;

@Provider
public class ConflictExceptionMapper implements ExceptionMapper<ConflictException> {

    public static final String HEADER_KEY = "X-Conflict-Key";
    public static final String HEADER_VALUE = "X-Conflict-Value";

    @Override
    public Response toResponse(ConflictException exception) {
        return Response.status(Response.Status.CONFLICT)
                .header(HEADER_KEY, exception.getKey())
                .header(HEADER_VALUE, exception.getValue())
                .entity(exception.getMessage())
                .build();
    }
}
