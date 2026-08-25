package io.github.zforgo.firqua.openapi;

import java.util.Map;
import java.util.Optional;

import org.eclipse.microprofile.openapi.OASFilter;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.media.Schema.SchemaType;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;

import io.quarkus.logging.Log;
import io.quarkus.smallrye.openapi.OpenApiFilter;
import io.quarkus.smallrye.openapi.OpenApiFilter.RunStage;

import io.github.zforgo.firqua.common.FilterResult;
import io.github.zforgo.firqua.common.jandex.JandexUtil;

import static io.github.zforgo.firqua.common.jandex.JandexUtil.resolveHttpPath;
import static java.util.stream.Collectors.toMap;
import static org.eclipse.microprofile.openapi.OASFactory.createSchema;

@OpenApiFilter(stages = { RunStage.BUILD })
public class FilterResponseOASFilter implements OASFilter {

    private static final DotName ANN = DotName.createSimple(FilterResponse.class.getName());
    private static final DotName RETURN_TYPE = DotName.createSimple(FilterResult.class.getName());
    private static final DotName SCHEMA_ANN = DotName.createSimple(Schema.class.getName());

    private final Map<String, String> paths;

    public FilterResponseOASFilter(IndexView view) {
        final var basePath = JandexUtil.resolveHttpBasePath(view);
        paths = view.getAnnotations(ANN).stream()
                .filter(ann -> ann.target().kind() == AnnotationTarget.Kind.METHOD)
                .distinct()
                .filter(ann -> {
                    var mi = ann.target().asMethod();
                    return RETURN_TYPE.equals(mi.returnType().name())
                            && mi.returnType().kind() == Type.Kind.PARAMETERIZED_TYPE;
                }).collect(toMap(
                        ann -> {
                            var mi = ann.target().asMethod();
                            return resolveHttpPath(basePath, mi);
                        },
                        ann -> resolveReference(view, ann)));
    }

    private static String resolveReference(IndexView view, AnnotationInstance ann) {
        return Optional.of(ann)
                .map(a -> a.value("reference"))
                .filter(v -> !v.asString().isBlank())
                .map(AnnotationValue::asString)
                .orElseGet(() -> resolveReferenceByIndex(view, ann.target().asMethod()));
    }

    private static String resolveReferenceByIndex(IndexView view, MethodInfo mi) {
        var itemType = resolveItemType(mi);
        return Optional.ofNullable(view.getClassByName(itemType.name()))
                .map(ci -> {
                    if (ci.isSealed() && !ci.permittedSubclasses().isEmpty()) {
                        Log.warnf("Sealed result type found (%s) on %s#%s, but no explicit reference() set.",
                                ci.simpleName(), mi.declaringClass().simpleName(), mi.name());
                    }
                    return ci;
                })
                .map(ci -> ci.declaredAnnotation(SCHEMA_ANN))
                .map(schemaAnn -> schemaAnn.value("name"))
                .filter(v -> !v.asString().isBlank())
                .map(AnnotationValue::asString)
                .orElse(itemType.name().local());

    }

    private static Type resolveItemType(MethodInfo mi) {
        var pt = mi.returnType().asParameterizedType();
        var arguments = pt.arguments();
        if (arguments.isEmpty()) {
            throw new IllegalStateException("No type argument found on " + mi.returnType());
        }
        var itemType = arguments.getFirst();
        if (itemType.kind() != Type.Kind.CLASS) {
            throw new IllegalStateException("Expected a concrete class type argument, found: " + itemType.kind());
        }
        return itemType;
    }

    @Override
    public void filterOpenAPI(OpenAPI openAPI) {
        if (openAPI.getPaths() == null) {
            return;
        }
        openAPI.getPaths().getPathItems().forEach((path, item) -> {
            if (item.getOperations() == null) {
                return;
            }
            item.getOperations().forEach((_, operation) -> {
                if (paths.containsKey(path)) {
                    final var genericType = paths.get(path);
                    final var schemaName = "FilterResult" + genericType;
                    final var responseSchema = createSchema()
                            .addType(SchemaType.OBJECT)
                            .addProperty("items", createSchema()
                                    .addType(SchemaType.ARRAY)
                                    .items(createSchema().ref("#/components/schemas/" + genericType)))
                            .addProperty("pagination", createSchema().ref("#/components/schemas/Pagination"));
                    final var referencedSchema = createSchema().ref("#/components/schemas/" + schemaName);
                    openAPI.getComponents().addSchema(schemaName, responseSchema);
                    operation.getResponses().getAPIResponse("200").getContent().getMediaTypes()
                            .forEach((_, m) -> m.schema(referencedSchema));
                }
            });
        });
    }
}
