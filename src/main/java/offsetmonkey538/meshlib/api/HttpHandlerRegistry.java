package offsetmonkey538.meshlib.api;

import org.jetbrains.annotations.NotNull;
import offsetmonkey538.meshlib.impl.HttpHandlerRegistryImpl;

public interface HttpHandlerRegistry {
    HttpHandlerRegistry INSTANCE = new HttpHandlerRegistryImpl();

    void register(@NotNull String id, @NotNull HttpHandler handler) throws IllegalArgumentException;

    @NotNull
    HttpHandler get(@NotNull String id) throws IllegalStateException;

    boolean has(@NotNull String id);
}
