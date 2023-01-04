/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package cat.pottery.engine.dependencies;

import cat.pottery.engine.dependencies.maven.MavenDependency;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

public final class PomContextRegistry {

    private static final String ID_FORMAT = "%s:%s:%s";

    private record Context(String parentContext, ConcurrentHashMap<String, String> parameters) {}
    private final Map<String, Context> contextMap;

    public PomContextRegistry(Map<String, Context> contextMap) {
        this.contextMap = contextMap;
    }

    public String contextIdFor(MavenDependency mavenDependency) {
        return ID_FORMAT.formatted(mavenDependency.groupId(), mavenDependency.artifactId(), mavenDependency.version());
    }

    public String contextIdFor(String group, String artifact, String version) {
        return ID_FORMAT.formatted(group, artifact, version);
    }

    public String registerFromParent(String parentGroup, String parentId, String parentVersion, String group, String id, String version) {
        var parentContextId = contextIdFor(parentGroup, parentId, parentVersion);
        if (!contextMap.containsKey(parentContextId)) {
            contextMap.put(parentContextId, new Context(null, new ConcurrentHashMap<>()));
        }

        var parameters = new ConcurrentHashMap<String, String>(0);
        parameters.put("project.version", version);

        var ctx = new Context(parentContextId, parameters);

        var newId = contextIdFor(group, id, version);
        contextMap.put(newId, ctx);

        return newId;
    }

    public String register(String group, String id, String version) {
        var parameters = new ConcurrentHashMap<String, String>();
        parameters.put("project.version", version);

        var ctx = new Context(null, parameters);

        var newId = contextIdFor(group, id, version);
        contextMap.put(newId, ctx);

        return newId;
    }

    public void addParameter(String id, String parameter, String value) {
        contextMap.get(id).parameters().put(parameter, value);
    }

    public String resolveExpression(String id, String expression) {
        if (expression == null) {
            return "";
        }

        var result = expression;
        while (result.contains("${") && result.contains("}")) {
            result = resolveExpressionCycle(id, result);
        }

        return result;
    }

    private String resolveExpressionCycle(String id, String expression) {
        var ref = new AtomicReference<>(expression);

        Context context = contextMap.get(id);
        context.parameters().forEach((key, value) -> {
            var toReplace = "${" + key + "}";
            if (ref.get().contains(toReplace)) {
                ref.getAndUpdate(x -> x.replace(toReplace, value));
            }
        });

        if (context.parentContext != null) {
            ref.getAndUpdate(x -> resolveExpressionCycle(context.parentContext, x));
        }

        return ref.get();
    }

    public boolean hasContext(String id) {
        return contextMap.containsKey(id);
    }
}
