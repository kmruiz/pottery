/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package cat.pottery.engine.dependencies;

import cat.pottery.engine.dependencies.maven.MavenDependency;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

public final class PomContextRegistry {

    private static final String ID_FORMAT = "%s:%s:%s";

    private record Context(String parentContext, ConcurrentHashMap<String, String> parameters) {}
    private final Map<String, Context> contextMap;
    private final Map<String, String> defaultVersionsSpecified;

    public PomContextRegistry(Map<String, Context> contextMap, Map<String, String> defaultVersionsSpecified) {
        this.contextMap = contextMap;
        this.defaultVersionsSpecified = defaultVersionsSpecified;
    }

    public String contextIdFor(MavenDependency mavenDependency) {
        return ID_FORMAT.formatted(mavenDependency.groupId(), mavenDependency.artifactId(), mavenDependency.decidedVersion());
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
        for (var i = 0; i < 3 && result.contains("${") && result.contains("}"); i++) {
            result = resolveExpressionCycle(id, result);
        }

        return result;
    }

    private String resolveExpressionCycle(String id, String expression) {
        Context context = contextMap.get(id);
        Map<String, String> allParams = new HashMap<>();

        allParams.putAll(context.parameters);
        allParams.putAll(defaultVersionsSpecified);

        for (var entry : allParams.entrySet()) {
            if (!expression.contains("${")) {
                return expression;
            }

            var toReplace = "${" + entry.getKey() + "}";
            expression = expression.replace(toReplace, entry.getValue());
        }

        if (context.parentContext != null) {
            return resolveExpressionCycle(context.parentContext, expression);
        }

        return expression;
    }

    public void addVersionSuggestion(String context, String qualifiedName, String version) {
        defaultVersionsSpecified.put(qualifiedName, resolveExpression(context, version));
    }

    public boolean hasContext(String id) {
        return contextMap.containsKey(id);
    }

    public String resolveDefaultVersion(String qualifiedName) {
        return defaultVersionsSpecified.get(qualifiedName);

    }
}
