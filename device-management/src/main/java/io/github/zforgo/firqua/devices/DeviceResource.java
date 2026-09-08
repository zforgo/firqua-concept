package io.github.zforgo.firqua.devices;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import io.github.zforgo.firqua.assets.AssetDto;
import io.github.zforgo.firqua.common.PagingAndSorting;
import io.github.zforgo.firqua.filter.FilterResult;
import io.github.zforgo.firqua.filter.openapi.FilterResponse;

import static io.github.zforgo.firqua.openapi.OpenApiConstants.RESP_badRequest;
import static io.github.zforgo.firqua.openapi.OpenApiConstants.RESP_conflict;
import static io.github.zforgo.firqua.openapi.OpenApiConstants.RESP_notFound;
import static io.github.zforgo.firqua.openapi.OpenApiConstants.RESP_serverError;
import static io.github.zforgo.firqua.openapi.OpenApiConstants.STATUS_created;
import static io.github.zforgo.firqua.openapi.OpenApiConstants.STATUS_ok;
import static io.github.zforgo.firqua.openapi.OpenApiConstants.TAG_devices;
import static io.github.zforgo.firqua.openapi.OpenApiConstants.TYPE_deviceCreateUnion;
import static io.github.zforgo.firqua.openapi.OpenApiConstants.TYPE_deviceUnion;

@Path("/devices")
@Produces(MediaType.APPLICATION_JSON)
@Tag(ref = TAG_devices)
public class DeviceResource {

    @Inject
    DeviceService deviceService;

    @GET
    @Operation(operationId = "listDevices", summary = "Paged list of devices and their subtypes")
    @FilterResponse(reference = TYPE_deviceUnion)
    @APIResponse(ref = RESP_badRequest)
    public FilterResult<DeviceDto<? extends AssetDto>> list(@Valid @BeanParam PagingAndSorting pas) {
        return deviceService.filter(pas);
    }

    @GET
    @Path("/{id}")
    @Operation(operationId = "getDevice", summary = "Get device and their subtypes by id")
    @APIResponse(responseCode = STATUS_ok, content = @Content(schema = @Schema(ref = TYPE_deviceUnion)))
    @APIResponse(ref = RESP_notFound)
    public DeviceDto<?> get(@NotNull @PathParam("id") Long id) {
        return deviceService.getById(id);
    }

    @POST
    @RequestBody(content = @Content(schema = @Schema(ref = TYPE_deviceCreateUnion)))
    @APIResponse(responseCode = STATUS_created,
            name = "created",
            description = "Entity created successfully",
            content = @Content(schema = @Schema(ref = TYPE_deviceUnion)))
    @APIResponse(ref = RESP_badRequest)
    @APIResponse(ref = RESP_conflict)
    @APIResponse(ref = RESP_serverError)
    @Operation(operationId = "createDevice", description = "Create typed device")
    public Response create(@NotNull @Valid DeviceCreateDto dto) {
        var device = deviceService.createDevice(dto);
        var createdUri = UriBuilder.fromResource(DeviceResource.class)
                .path(DeviceResource.class, "get")
                .build(device.id);
        return Response.created(createdUri).entity(device).build();
    }

    @PUT
    @Path("/{id}")
    @RequestBody(content = @Content(schema = @Schema(ref = TYPE_deviceCreateUnion)))
    @APIResponse(responseCode = STATUS_ok,
            name = "ok",
            description = "Entity modified successfully",
            content = @Content(schema = @Schema(ref = TYPE_deviceUnion)))
    @APIResponse(ref = RESP_badRequest)
    @APIResponse(ref = RESP_notFound)
    @APIResponse(ref = RESP_conflict)
    @APIResponse(ref = RESP_serverError)
    @Operation(operationId = "modifyDevice", description = "Update typed device")
    public DeviceDto<? extends AssetDto> modify(@PathParam("id") Long id, @NotNull @Valid DeviceCreateDto dto) {
        return deviceService.updateDevice(id, dto);
    }

}
