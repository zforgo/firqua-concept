package io.github.zforgo.firqua.common.jandex;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Arrays;

import org.jboss.jandex.Index;
import org.jboss.jandex.Indexer;

public class IndexBuilder {

    private IndexBuilder() {
        //avoid direct instantiation
    }

    public static Index build(Class<?>... classes) {
        var indexer = new Indexer();
        Arrays.stream(classes)
                .forEach(c -> {
                    try (var is = c.getClassLoader()
                            .getResourceAsStream(c.getName().replace('.', '/') + ".class")) {
                        assert is != null;
                        indexer.index(is);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                });
        return indexer.complete();
    }
}
