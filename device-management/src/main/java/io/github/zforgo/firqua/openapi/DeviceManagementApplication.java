package io.github.zforgo.firqua.openapi;

import jakarta.inject.Singleton;
import jakarta.ws.rs.core.Application;

import org.eclipse.microprofile.openapi.annotations.Components;
import org.eclipse.microprofile.openapi.annotations.OpenAPIDefinition;
import org.eclipse.microprofile.openapi.annotations.info.Info;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;

@Singleton
@OpenAPIDefinition(
        info = @Info(
                title = "FIRQUA Device Management",
                version = "1.0.0"),
        components = @Components(
                responses = {
                        @APIResponse(responseCode = "200",
                                name = "ok",
                                description = "Requested request was processed and the response served successfully"),
                        @APIResponse(responseCode = "400",
                                name = "bad_request",
                                description = "The request is malformed or violates validation rules"),
                        @APIResponse(responseCode = "404",
                                name = "not_found",
                                description = "Requested resource does not exist"),
                        @APIResponse(responseCode = "409",
                                name = "conflict",
                                description = "Request conflict with the current state of the target resource, typically a unique constraint violation."),
                        @APIResponse(responseCode = "500",
                                name = "server_error",
                                description = "An unexpected error encountered while processing the request.")
                }))
public class DeviceManagementApplication extends Application {
}
