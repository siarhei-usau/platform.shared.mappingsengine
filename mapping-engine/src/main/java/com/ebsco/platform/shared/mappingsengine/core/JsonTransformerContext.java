package com.ebsco.platform.shared.mappingsengine.core;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import lombok.Getter;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class JsonTransformerContext {

    @NonNull
    @Getter
    private final Object jsonObject;
    @NonNull
    @Getter
    private final DocumentContext jpathCtx;
    @NonNull
    @Getter
    private final DocumentContext jvalueCtx;
    @NonNull
    @Getter
    private final DocumentContext jvalueListCtx;

    public JsonTransformerContext(Object jsonObject, Configuration jpathCfg, Configuration jvalueCfg, Configuration jvalueListCfg) {
        this.jsonObject = jsonObject;
        this.jpathCtx = JsonPath.using(jpathCfg).parse(jsonObject);
        this.jvalueCtx = JsonPath.using(jvalueCfg).parse(jsonObject);
        this.jvalueListCtx = JsonPath.using(jvalueListCfg).parse(jsonObject);
    }

    // TODO: temporary while testing missing changes from Kotlin code.
    public DocumentContext exposePathCtx() { return jpathCtx; }

    // we might be able to call CompiledPath.eval to get both values and paths at same time, but we lose path caching and have to do that ourselves
    @NotNull
    @SuppressWarnings("WeakerAccess")
    public List<String> queryForPaths(@NotNull final String jsonPath) {
        return jpathCtx.read(jsonPath);
    }

    @NotNull
    @SuppressWarnings("WeakerAccess")
    public List<String> queryForPaths(@NotNull final JsonPath jsonPath) {
        return jpathCtx.read(jsonPath);
    }

    @NotNull
    public List<Object> queryForValues(@NotNull final String jsonPath) {
        return jvalueListCtx.read(jsonPath);
    }

    @NotNull
    public List<Object> queryForValues(@NotNull final JsonPath jsonPath) {
        return jvalueListCtx.read(jsonPath);
    }

    @NotNull
    public Object queryForValue(@NotNull final String jsonPath) {
        return queryForValues(jsonPath).get(0);
    }

    @NotNull
    public Object queryForValue(@NotNull final JsonPath jsonPath) {
        return queryForValues(jsonPath).get(0);
    }

    @NotNull
    @SuppressWarnings({"unchecked", "WeakerAccess"})
    public List<ResolvedPaths> resolveTargetPaths(@NotNull final String targetJsonPath, @NotNull final List<String> relativeToPath, boolean allowNonMatching) {
        return PathUtils.resolveTargetPaths(jpathCtx, targetJsonPath, relativeToPath, allowNonMatching);
    }

    @NotNull
    @SuppressWarnings("unchecked")
    public List<ResolvedPaths> resolveTargetPaths(@NotNull final String targetJsonPath, @NotNull final List<String> relativeToPath) {
        return PathUtils.resolveTargetPaths(jpathCtx, targetJsonPath, relativeToPath, false);
    }

    @NotNull
    @SuppressWarnings("WeakerAccess")
    public List<ResolvedPaths> queryAndResolveTargetPaths(@NotNull final String jsonPath, @NotNull final String targetJsonPath, boolean allowNonMatching) {
        return resolveTargetPaths(targetJsonPath, queryForPaths(jsonPath), allowNonMatching);
    }

    @NotNull
    public List<ResolvedPaths> queryAndResolveTargetPaths(@NotNull final String jsonPath, @NotNull final String targetJsonPath) {
        return resolveTargetPaths(targetJsonPath, queryForPaths(jsonPath), false);
    }

    @NotNull
    @SuppressWarnings("WeakerAccess")
    public List<ResolvedPaths> queryAndResolveTargetPaths(@NotNull final JsonPath jsonPath, @NotNull final String targetJsonPath, boolean allowNonMatching) {
        return resolveTargetPaths(targetJsonPath, queryForPaths(jsonPath), allowNonMatching);
    }

    @NotNull
    public List<ResolvedPaths> queryAndResolveTargetPaths(@NotNull final JsonPath jsonPath, @NotNull final String targetJsonPath) {
        return resolveTargetPaths(targetJsonPath, queryForPaths(jsonPath), false);
    }

    public void applyUpdate(@NotNull final ResolvedPaths target, @NotNull final Object jsonFragment) {
        PathUtils.applyUpdatePath(jpathCtx, target.getTargetBasePath(), target.getTargetUpdatePath(), jsonFragment);
    }

    public void applyUpdate(@NotNull final String targetBasePath, @NotNull final String targetUpdatePath, Object jsonFragment) {
        PathUtils.applyUpdatePath(jpathCtx, targetBasePath, targetUpdatePath, jsonFragment);
    }

    public void deleteValue(@NotNull final JsonPath jsonPath) {
        jvalueCtx.delete(jsonPath);
    }

    public void deleteValue(@NotNull final String jsonPath) {
        jvalueCtx.delete(jsonPath);
    }
}
