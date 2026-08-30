package io.github.zforgo.firqua.common.jandex;

import java.util.stream.Stream;

import jakarta.validation.Valid;
import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.OPTIONS;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Application;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.jboss.jandex.DotName;
import org.jboss.jandex.EmptyIndex;
import org.jboss.jandex.Index;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class JandexUtilTest {
    static final String CONFIG_KEY = "quarkus.rest.path";

    @Nested
    @DisplayName("Tests for resolveHttpBasePath")
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class BasePathTest {

        private Config originalConfig;

        @ApplicationPath("annotated")
        static class TestApp extends Application {
        }

        @BeforeAll
        void setUp() {
            var resolver = ConfigProviderResolver.instance();
            originalConfig = ConfigProvider.getConfig();
            resolver.releaseConfig(originalConfig);

            var withoutAppConfig = resolver.getBuilder()
                    .addDefaultSources()
                    .build();
            resolver.registerConfig(withoutAppConfig, Thread.currentThread().getContextClassLoader());
        }

        @AfterAll
        void tearDown() {
            var resolver = ConfigProviderResolver.instance();
            var currentConfig = ConfigProvider.getConfig();
            resolver.releaseConfig(currentConfig);
            resolver.registerConfig(originalConfig, Thread.currentThread().getContextClassLoader());
        }

        @AfterEach
        void clear() {
            System.clearProperty(CONFIG_KEY);
        }

        @Test
        @DisplayName("Resolves base path from configuration when @ApplicationPath is absent")
        void onlyConfig() {
            System.setProperty(CONFIG_KEY, "custom");
            var base = JandexUtil.resolveHttpBasePath(EmptyIndex.INSTANCE);
            assertEquals("custom", base);
        }

        @Test
        @DisplayName("Resolves base path from @ApplicationPath when configuration is absent")
        void onlyAnnotation() {
            var index = IndexBuilder.build(TestApp.class);
            System.clearProperty(CONFIG_KEY);
            var base = JandexUtil.resolveHttpBasePath(index);
            assertEquals("annotated", base);
        }

        @Test
        @DisplayName("Prefers @ApplicationPath over configuration when both are present")
        void bothConfigured() {
            var index = IndexBuilder.build(TestApp.class);
            System.setProperty(CONFIG_KEY, "custom");
            var base = JandexUtil.resolveHttpBasePath(index);
            assertEquals("annotated", base);

        }

        @Test
        @DisplayName("Resolves to empty base path when neither @ApplicationPath nor configuration is present")
        void neitherConfiguredNorAnnotated() {
            System.clearProperty(CONFIG_KEY);
            var base = JandexUtil.resolveHttpBasePath(EmptyIndex.INSTANCE);
            assertEquals("", base);
        }
    }

    @Nested
    @DisplayName("Tests for resolveHttpPath")
    class HttpPathTests {

        @Path("/")
        static class DummyRootResource {
            @OPTIONS
            public String list() {
                return null;
            }

            @DELETE
            @Path("/{id}")
            public String get() {
                return null;
            }
        }

        @Path("foo")
        static class DummyResource {
            @GET
            public String list() {
                return null;
            }

            @POST
            @Path("{id}")
            public String get() {
                return null;
            }

            @PUT
            @Path("bar/")
            public String bar() {
                return null;
            }
        }

        private final Index index = IndexBuilder.build(DummyRootResource.class, DummyResource.class);

        @ParameterizedTest
        @MethodSource("pathByParts")
        void normalizeFragments(String expected, String... parts) {
            assertEquals(expected, JandexUtil.normalize(parts));
        }

        @Test
        void normalizeNull() {
            final String[] parts = null;
            assertThrows(NullPointerException.class, () -> JandexUtil.normalize(parts));
        }

        static Stream<Arguments> pathByParts() {
            return Stream.of(
                    arguments("/", new String[] {}),
                    arguments("/", new String[] { "" }),
                    arguments("/", new String[] { "  /  " }),
                    arguments("/", new String[] { "", "" }),
                    arguments("/", new String[] { "/", "//", "/" }),
                    arguments("/foo/bar", new String[] { "/foo//", "/bar/", "/" }),
                    arguments("/foo/bar", new String[] { "foo", "bar", "/" }),
                    arguments("/foo/bar", new String[] { "foo", "bar" }),
                    arguments("/foo/bar", new String[] { "foo ", "bar /" }),
                    arguments("/foo/bar", new String[] { "foo", "bar/" }),
                    arguments("/foo/{id}/bar", new String[] { "foo", "/{id}", "bar" })
            );
        }

        @ParameterizedTest
        @MethodSource("pathByAnnotationProvider")
        @DisplayName("Resolves normalized HTTP path by @Path annotations")
        void resolvePathByAnnotation(DotName annotation, String expectedPath) {
            var ann = index.getAnnotations(annotation).getFirst();
            assertNotNull(ann);
            var mi = ann.target().asMethod();
            var path = JandexUtil.resolveHttpPath(mi);
            assertEquals(expectedPath, path);
        }

        static Stream<Arguments> pathByAnnotationProvider() {
            return Stream.of(
                    argumentSet(
                            "@Path presents only on class",
                            DotName.createSimple(OPTIONS.class.getName()), "/"
                    ),
                    argumentSet(
                            "@Path present on both class and method",
                            DotName.createSimple(DELETE.class.getName()), "/{id}"
                    ),
                    argumentSet(
                            "@Path presents only on class without leading slash",
                            DotName.createSimple(GET.class.getName()), "/foo"
                    ),
                    argumentSet(
                            "@Path present on both class and method without leading slashes",
                            DotName.createSimple(POST.class.getName()), "/foo/{id}"
                    ),
                    argumentSet(
                            "@Path on method contains trailing slash",
                            DotName.createSimple(PUT.class.getName()), "/foo/bar"
                    )

            );
        }
    }

    @Nested
    class AnnotationValueTest {

        @Path("somePath")
        static class DummyResource {
            @POST
            @RequestBody(name = "something")
            public void doSomething(@Valid Object ignoredPayload) {

            }
        }

        private final Index index = IndexBuilder.build(DummyResource.class);

        @ParameterizedTest
        @MethodSource("annotations")
        @DisplayName("Extracts annotation value as String or empty when absent")
        void resolveValue(DotName dotName, String expected) {
            var ann = index.getAnnotations(dotName).getFirst();
            assertEquals(expected, JandexUtil.annotationValue(ann));
        }

        static Stream<Arguments> annotations() {
            return Stream.of(
                    argumentSet("@Path has value attribute", DotName.createSimple(Path.class.getName()), "somePath"),
                    argumentSet("@POST has no attributes", DotName.createSimple(POST.class.getName()), ""),
                    argumentSet(
                            "@RequestBody has no value attribute but name was set",
                            DotName.createSimple(RequestBody.class.getName()), ""
                    ),
                    argumentSet("@Valid has no value attribute", DotName.createSimple(Valid.class.getName()), "")
            );
        }
    }

}
