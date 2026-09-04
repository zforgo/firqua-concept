package io.github.zforgo.firqua;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import io.github.zforgo.firqua.common.NonUniqueIpAddressException;

@Provider
public class NonUniqueIpAddressExceptionMapper implements ExceptionMapper<NonUniqueIpAddressException> {

    @Override
    public Response toResponse(NonUniqueIpAddressException exception) {
        return Response.status(Response.Status.CONFLICT).header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN)
                .entity(exception.getMessage()).build();
    }
}
