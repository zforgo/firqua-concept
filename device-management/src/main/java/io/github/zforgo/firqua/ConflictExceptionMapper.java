package io.github.zforgo.firqua;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import io.github.zforgo.firqua.common.ConflictException;
import io.github.zforgo.firqua.common.UnexpectedValueException;

@Provider
public class ConflictExceptionMapper implements ExceptionMapper<ConflictException> {

    public static final String HEADER_KEY = "X-Conflict-Key";
    public static final String HEADER_VALUE = "X-Conflict-Value";
    public static final String HEADER_EXPECTED = "X-Conflict-Expected";

    @Override
    public Response toResponse(ConflictException exception) {
        var response = Response.status(Response.Status.CONFLICT)
                .entity(exception.getMessage());
        if (exception instanceof UnexpectedValueException uex) {
            response
                    .header(HEADER_KEY, uex.getKey())
                    .header(HEADER_VALUE, uex.getValue())
                    .header(HEADER_EXPECTED, uex.getExpected());
        }
        return response.build();
    }
}
