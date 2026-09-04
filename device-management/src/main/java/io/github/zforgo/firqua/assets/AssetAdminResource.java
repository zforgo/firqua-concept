package io.github.zforgo.firqua.assets;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
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

import io.quarkus.arc.profile.IfBuildProfile;
import io.quarkus.runtime.LaunchMode;

import static io.github.zforgo.firqua.openapi.OpenApiConstants.RESP_badRequest;
import static io.github.zforgo.firqua.openapi.OpenApiConstants.RESP_conflict;
import static io.github.zforgo.firqua.openapi.OpenApiConstants.RESP_notFound;
import static io.github.zforgo.firqua.openapi.OpenApiConstants.RESP_serverError;
import static io.github.zforgo.firqua.openapi.OpenApiConstants.TAG_admin;
import static io.github.zforgo.firqua.openapi.OpenApiConstants.TAG_assets;
import static io.github.zforgo.firqua.openapi.OpenApiConstants.TYPE_assetCreateUnion;
import static io.github.zforgo.firqua.openapi.OpenApiConstants.TYPE_assetUnion;

@IfBuildProfile(anyOf = { LaunchMode.DEV_PROFILE, LaunchMode.TEST_PROFILE })
@Path("/admin/assets")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Tag(ref = TAG_admin)
@Tag(ref = TAG_assets)
public class AssetAdminResource {

    @Inject
    AssetAdminService service;

    @POST
    @RequestBody(content = @Content(schema = @Schema(ref = TYPE_assetCreateUnion)))
    @APIResponse(responseCode = "201",
            name = "created",
            description = "Entity created successfully",
            content = @Content(schema = @Schema(ref = TYPE_assetUnion)))
    @APIResponse(ref = RESP_badRequest)
    @APIResponse(ref = RESP_conflict)
    @APIResponse(ref = RESP_serverError)
    @Operation(operationId = "createAsset", description = "Create typed asset")
    public Response create(@NotNull @Valid AssetCreateDto dto) {
        var asset = service.createAsset(dto);
        var createdUri = UriBuilder.fromResource(AssetResource.class)
                .path(AssetResource.class, "get")
                .build(asset.id);
        return Response.created(createdUri).entity(asset).build();
    }

    @PUT
    @Path("/{id}")
    @RequestBody(content = @Content(schema = @Schema(ref = TYPE_assetCreateUnion)))
    @APIResponse(responseCode = "200",
            name = "ok",
            description = "Entity modified successfully",
            content = @Content(schema = @Schema(ref = TYPE_assetUnion)))
    @APIResponse(ref = RESP_badRequest)
    @APIResponse(ref = RESP_notFound)
    @APIResponse(ref = RESP_conflict)
    @APIResponse(ref = RESP_serverError)
    @Operation(operationId = "modifyAsset", description = "Update typed asset")
    public AssetDto modify(@PathParam("id") Long id, @NotNull @Valid AssetCreateDto dto) {
        return service.updateAsset(id, dto);
    }
}
