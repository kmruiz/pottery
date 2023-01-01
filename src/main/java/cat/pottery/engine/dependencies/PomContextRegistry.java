package cat.pottery.engine.dependencies;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

public final class PomContextRegistry {

    private static final String ID_FORMAT = "%s:%s:%s";

    private record Context(String parentGroup, String parentId, String parentVersion, Map<String, String> parameters) {}
    private final Map<String, Context> contextMap;

    public PomContextRegistry(Map<String, Context> contextMap) {
        this.contextMap = contextMap;
    }

    public String registerFromParent(String parentGroup, String parentId, String parentVersion, String group, String id, String version) {
        var parentContextId = ID_FORMAT.formatted(parentGroup, parentId, parentVersion);
        if (!contextMap.containsKey(parentContextId)) {
            contextMap.put(parentContextId, new Context(parentGroup, parentId, parentVersion, new ConcurrentHashMap<>()));
        }

        var ctx = new Context(parentGroup, parentId, parentVersion,
                new ConcurrentHashMap<>(contextMap.get(ID_FORMAT.formatted(parentGroup, parentId, parentVersion)).parameters())
        );

        String newId = ID_FORMAT.formatted(group, id, version);
        contextMap.put(newId, ctx);

        return newId;
    }

    public String register(String group, String id, String version) {
        var ctx = new Context(group, id, version,
                new ConcurrentHashMap<>()
        );

        String newId = ID_FORMAT.formatted(group, id, version);
        contextMap.put(newId, ctx);

        return newId;
    }

    public void addParameter(String id, String parameter, String value) {
        contextMap.get(id).parameters().put(parameter, value);
    }

    public String resolveExpression(String id, String expression) {
        var ref = new AtomicReference<>(expression);

        contextMap.get(id).parameters().forEach((key, value) ->
                ref.set(ref.get().replace("${" + key + "}", value))
        );

        return ref.get();
    }
}
