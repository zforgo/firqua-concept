package io.github.zforgo.firqua.common.jandex;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.Path;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;

public final class JandexUtil {

    private static final DotName APPLICATION_PATH = DotName.createSimple(ApplicationPath.class.getName());
    private static final DotName PATH = DotName.createSimple(Path.class.getName());
    private static final Supplier<Optional<String>> httpBasePathByConfig = () -> ConfigProvider.getConfig()
            .getOptionalValue("quarkus.rest.path", String.class);

    private JandexUtil() {
        //avoid direct instantiation
    }

    public static String resolveHttpBasePath(IndexView view) {
        return view.getAnnotations(APPLICATION_PATH).stream()
                .findFirst()
                .map(AnnotationInstance::value)
                .map(AnnotationValue::asString)
                .filter(StringUtils::isNotBlank)
                .or(httpBasePathByConfig)
                .orElse("");
    }

    public static String resolveHttpPath(MethodInfo method) {
        return resolveHttpPath("", method);
    }

    public static String resolveHttpPath(String basePath, MethodInfo method) {
        var classPath = annotationValue(method.declaringClass().declaredAnnotation(PATH));
        var methodPath = annotationValue(method.annotation(PATH));
        return normalize(basePath, classPath, methodPath);
    }

    public static String annotationValue(AnnotationInstance annotation) {
        return Optional.ofNullable(annotation)
                .map(AnnotationInstance::value)
                .map(AnnotationValue::asString)
                .orElse("");
    }

    static String normalize(String... parts) {
        Objects.requireNonNull(parts, "parts must not be null");
		var path = Arrays.stream(parts)
                .map(JandexUtil::stripSlashes)
                .collect(Collectors.joining("/"))
                .replaceAll("/{2,}", "/");
        return Optional.of(path)
                .map(JandexUtil::stripSlashes)
                .map(s -> "/" + s)
                .orElse("/");
    }

    private static String stripSlashes(String path) {
        return Optional.ofNullable(path)
                .map(String::trim)
                .map(s -> StringUtils.strip(s, "/"))
                .orElse("");
    }
}
