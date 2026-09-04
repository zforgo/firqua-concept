package io.github.zforgo.firqua.openapi;

import jakarta.inject.Singleton;
import jakarta.ws.rs.core.Application;

import org.eclipse.microprofile.openapi.annotations.Components;
import org.eclipse.microprofile.openapi.annotations.OpenAPIDefinition;
import org.eclipse.microprofile.openapi.annotations.info.Info;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import static io.github.zforgo.firqua.openapi.OpenApiConstants.RESP_badRequest;
import static io.github.zforgo.firqua.openapi.OpenApiConstants.RESP_conflict;
import static io.github.zforgo.firqua.openapi.OpenApiConstants.RESP_notFound;
import static io.github.zforgo.firqua.openapi.OpenApiConstants.RESP_ok;
import static io.github.zforgo.firqua.openapi.OpenApiConstants.RESP_serverError;
import static io.github.zforgo.firqua.openapi.OpenApiConstants.TAG_admin;
import static io.github.zforgo.firqua.openapi.OpenApiConstants.TAG_assets;

@Singleton
@OpenAPIDefinition(
        info = @Info(
                title = "FIRQUA Device Management",
                version = "1.0.0"),
        tags = {
                @Tag(name = TAG_assets, description = "Operations on Assets and their subtypes"),
                @Tag(name = TAG_admin,
                        description = "Temporary endpoints for development and testing; Not exposed in production"),
        },
        components = @Components(
                responses = {
                        @APIResponse(name = RESP_ok,
                                responseCode = "200",
                                description = "Requested request was processed and the response served successfully"),
                        @APIResponse(
                                responseCode = "201",
                                description = "Entity created successfully"),
                        @APIResponse(name = RESP_badRequest,
                                responseCode = "400",
                                description = "The request is malformed or violates validation rules"),
                        @APIResponse(name = RESP_notFound,
                                responseCode = "404",
                                description = "Requested resource does not exist"),
                        @APIResponse(name = RESP_conflict,
                                responseCode = "409",
                                description = "Request conflict with the current state of the target resource, typically a unique constraint violation."),
                        @APIResponse(name = RESP_serverError,
                                responseCode = "500",
                                description = "An unexpected error encountered while processing the request.")
                }))
public class DeviceManagementApplication extends Application {
}
