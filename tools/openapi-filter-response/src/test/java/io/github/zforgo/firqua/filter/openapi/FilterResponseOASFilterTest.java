package io.github.zforgo.firqua.filter.openapi;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.openapi.OASFactory;
import org.eclipse.microprofile.openapi.annotations.media.DiscriminatorMapping;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.media.SchemaProperty;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.PathItem;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.zforgo.firqua.filter.FilterResult;
import io.github.zforgo.firqua.filter.jandex.IndexBuilder;
import io.github.zforgo.firqua.filter.jandex.JandexUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class FilterResponseOASFilterTest {

    private final Index index = IndexBuilder.build(
            DummyResource.class, DummyApp.class, ResponseDto.class,
            ResponseDtoUnion.class
    );
    private final String basePath = JandexUtil.resolveHttpBasePath(index);
    private final FilterResponseOASFilter filter = new FilterResponseOASFilter(index);

    private static OpenAPI apiForPath(String path) {
        var openAPI = OASFactory.createOpenAPI()
                .components(OASFactory.createComponents());
        var response200 = OASFactory.createAPIResponse()
                .content(
                        OASFactory.createContent()
                                .addMediaType(MediaType.APPLICATION_JSON, OASFactory.createMediaType())
                );
        var operation = OASFactory.createOperation()
                .responses(OASFactory.createAPIResponses().addAPIResponse("200", response200));
        openAPI.paths(OASFactory.createPaths().addPathItem(path, OASFactory.createPathItem().GET(operation)));
        return openAPI;
    }

    @Test
    @DisplayName("Resolves schema from @FilterResponse's reference attribute when explicitly set")
    void referenced() {
        var mi = index.getClassByName(DotName.createSimple(DummyResource.class.getName())).firstMethod("unionList");
        var path = JandexUtil.resolveHttpPath(basePath, mi);
        var openAPI = apiForPath(path);
        filter.filterOpenAPI(openAPI);

        var wrapper = openAPI.getComponents().getSchemas().get("FilterResultResponseUnion");
        assertNotNull(wrapper);
        assertEquals("#/components/schemas/ResponseUnion", wrapper.getProperties().get("items").getItems().getRef());
        var responseSchema = openAPI.getPaths().getPathItem(path)
                .getOperations()
                .get(PathItem.HttpMethod.GET).getResponses()
                .getAPIResponse("200").getContent().getMediaType(MediaType.APPLICATION_JSON)
                .getSchema();
        assertEquals("#/components/schemas/FilterResultResponseUnion", responseSchema.getRef());
    }

    @Test
    @DisplayName("Resolves schema from the generic type argument when @FilterResponse's reference attribute is not set")
    void nonReferenced() {
        var mi = index.getClassByName(DotName.createSimple(DummyResource.class.getName())).firstMethod("list");
        var path = JandexUtil.resolveHttpPath(basePath, mi);
        var openAPI = apiForPath(path);
        filter.filterOpenAPI(openAPI);

        var wrapper = openAPI.getComponents().getSchemas().get("FilterResultResponseDto");
        assertNotNull(wrapper);
        assertEquals("#/components/schemas/ResponseDto", wrapper.getProperties().get("items").getItems().getRef());
        var responseSchema = openAPI.getPaths().getPathItem(path)
                .getOperations()
                .get(PathItem.HttpMethod.GET).getResponses()
                .getAPIResponse("200").getContent().getMediaType(MediaType.APPLICATION_JSON)
                .getSchema();
        assertEquals("#/components/schemas/FilterResultResponseDto", responseSchema.getRef());
    }

    @Schema(name = "ResponseUnion",
            oneOf = { FooResponseDto.class, BarResponseDto.class },
            discriminatorProperty = "type",
            discriminatorMapping = {
                    @DiscriminatorMapping(value = "FOO", schema = FooResponseDto.class),
                    @DiscriminatorMapping(value = "BAR", schema = BarResponseDto.class)
            })
    public interface ResponseDtoUnion {}

    @SuppressWarnings("unused")
    @Schema(discriminatorProperty = "type")
    static sealed abstract class ResponseDto permits FooResponseDto, BarResponseDto {

        Long id;
        String type;
    }

    @SuppressWarnings("unused")
    @Schema(allOf = { ResponseDto.class },
            properties = @SchemaProperty(name = "type", constValue = "FOO"))
    static final class FooResponseDto extends ResponseDto {

        String fooSpecific;
    }

    @SuppressWarnings("unused")
    @Schema(allOf = { ResponseDto.class },
            properties = @SchemaProperty(name = "type", constValue = "BAR"))
    static final class BarResponseDto extends ResponseDto {

        String barSpecific;
    }

    @Path("/test")
    @Produces(MediaType.APPLICATION_JSON)
    static class DummyResource {

        @GET
        @Path("/list")
        @FilterResponse
        public FilterResult<ResponseDto> list() {
            return null;
        }

        @GET
        @Path("/unionList")
        @FilterResponse(reference = "ResponseUnion")
        public FilterResult<ResponseDto> unionList() {
            return null;
        }
    }

    @ApplicationPath("/testApi")
    static class DummyApp extends Application {}
}
