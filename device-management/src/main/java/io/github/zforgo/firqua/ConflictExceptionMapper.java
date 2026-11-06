package io.github.zforgo.firqua;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import io.github.zforgo.firqua.common.ConflictException;
import io.github.zforgo.firqua.common.NonUniqueException;

@Provider
public class ConflictExceptionMapper implements ExceptionMapper<ConflictException> {

    public static final String HEADER_KEY = "X-Conflict-Key";
    public static final String HEADER_VALUE = "X-Conflict-Value";

    @Override
    public Response toResponse(ConflictException exception) {
        var response = Response.status(Response.Status.CONFLICT)
                .entity(exception.getMessage());
        if (exception instanceof NonUniqueException nex) {
            response
                    .header(HEADER_KEY, nex.getKey())
                    .header(HEADER_VALUE, nex.getValue());
        }
        return response.build();
    }
}
