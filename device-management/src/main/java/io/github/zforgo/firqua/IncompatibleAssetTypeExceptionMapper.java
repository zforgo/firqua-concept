package io.github.zforgo.firqua;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import io.github.zforgo.firqua.common.IncompatibleAssetTypeException;

@Provider
public class IncompatibleAssetTypeExceptionMapper implements ExceptionMapper<IncompatibleAssetTypeException> {

    public static final String HEADER_KEY = "X-Conflict-Key";
    public static final String HEADER_VALUE = "X-Conflict-Value";

    @Override
    public Response toResponse(IncompatibleAssetTypeException exception) {
        return Response.status(Response.Status.CONFLICT)
                .header(HEADER_KEY, "assetId")
                .header(HEADER_VALUE, exception.getAssetId())
                .header("X-Conflict-Expected", exception.getExpectedType())
                .entity(exception.getMessage())
                .build();
    }
}
