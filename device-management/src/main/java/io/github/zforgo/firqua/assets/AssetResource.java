package io.github.zforgo.firqua.assets;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import io.github.zforgo.firqua.common.PagingAndSorting;
import io.github.zforgo.firqua.filter.FilterResult;
import io.github.zforgo.firqua.filter.openapi.FilterResponse;

import static io.github.zforgo.firqua.openapi.OpenApiConstants.RESP_badRequest;
import static io.github.zforgo.firqua.openapi.OpenApiConstants.RESP_notFound;
import static io.github.zforgo.firqua.openapi.OpenApiConstants.STATUS_ok;
import static io.github.zforgo.firqua.openapi.OpenApiConstants.TAG_assets;
import static io.github.zforgo.firqua.openapi.OpenApiConstants.TYPE_assetUnion;

@Path("/assets")
@Produces(MediaType.APPLICATION_JSON)
@Tag(ref = TAG_assets)
public class AssetResource {

    @Inject
    AssetQueryService assetQueryService;

    @GET
    @Operation(operationId = "listAssets", summary = "Paged list of assets and their subtypes")
    @FilterResponse(reference = TYPE_assetUnion)
    @APIResponse(ref = RESP_badRequest)
    public FilterResult<AssetDto> list(@Valid @BeanParam PagingAndSorting pas) {
        return assetQueryService.filter(pas);
    }

    @GET
    @Path("/{id}")
    @Operation(operationId = "getAsset", summary = "Get asset and their subtypes by id")
    @APIResponse(responseCode = STATUS_ok, content = @Content(schema = @Schema(ref = TYPE_assetUnion)))
    @APIResponse(ref = RESP_notFound)
    public AssetDto get(@NotNull @PathParam("id") Long id) {
        return assetQueryService.getById(id);
    }
}
