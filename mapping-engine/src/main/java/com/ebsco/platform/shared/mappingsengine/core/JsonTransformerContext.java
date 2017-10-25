package com.ebsco.platform.shared.mappingsengine.core;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Predicate;
import kotlin.jvm.internal.Intrinsics;
import lombok.Getter;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class JsonTransformerContext {
    @NonNull
    @Getter
    private final DocumentContext jpathCtx;

    @NonNull
    @Getter
    private final DocumentContext jvalueCtx;

    @NonNull
    @Getter
    private final DocumentContext jvalueListCtx;

    @NonNull
    @Getter
    private final Object jsonObject;

    @NonNull
    @Getter
    private final Configuration jpathCfg;

    @NonNull
    @Getter
    private final Configuration jvalueCfg;

    @NonNull
    @Getter
    private final Configuration jvalueListCfg;

    @NotNull
    @SuppressWarnings("WeakerAccess")
    public final List<String> queryForPaths(@NotNull final String jsonPath) {
        return this.jpathCtx.read(jsonPath);
    }

    @NotNull
    @SuppressWarnings("WeakerAccess")
    public final List<String> queryForPaths(@NotNull final JsonPath jsonPath) {
        return this.jpathCtx.read(jsonPath);
    }

    @NotNull
    public final List<Object> queryForValues(@NotNull final String jsonPath) {
        return this.jvalueListCtx.read(jsonPath);
    }

    @NotNull
    public final List<Object> queryForValues(@NotNull final JsonPath jsonPath) {
        return this.jvalueListCtx.read(jsonPath);
    }

    @NotNull
    public final Object queryForValue(@NotNull final String jsonPath) {
        return this.jvalueCtx.read(jsonPath);
    }

    @NotNull
    public final Object queryForValue(@NotNull final JsonPath jsonPath) {
        return this.jvalueCtx.read(jsonPath);
    }

    @NotNull
    @SuppressWarnings({"unchecked", "WeakerAccess"})
    public final List<ResolvedPaths> resolveTargetPaths(@NotNull final String targetJsonPath, @NotNull final List<String> relativeToPaths, final boolean allowNonMatching) {
        return PathUtilsKt.resolveTargetPaths(this.jpathCtx, targetJsonPath, relativeToPaths, allowNonMatching);
    }

    @NotNull
    @SuppressWarnings("unchecked")
    public final List<ResolvedPaths> resolveTargetPaths(@NotNull final String targetJsonPath, @NotNull final List<String> relativeToPaths) {
        return resolveTargetPaths(targetJsonPath, relativeToPaths, false);
    }

    @NotNull
    @SuppressWarnings("WeakerAccess")
    public final List<ResolvedPaths> queryAndResolveTargetPaths(@NotNull final String jsonPath, @NotNull final String targetJsonPath, final boolean allowNonMatching) {
        return resolveTargetPaths(targetJsonPath, queryForPaths(jsonPath), allowNonMatching);
    }

    @NotNull
    public final List<ResolvedPaths> queryAndResolveTargetPaths(@NotNull final String jsonPath, @NotNull final String targetJsonPath) {
        return resolveTargetPaths(targetJsonPath, queryForPaths(jsonPath), false);
    }

    @NotNull
    @SuppressWarnings("WeakerAccess")
    public final List<ResolvedPaths> queryAndResolveTargetPaths(@NotNull final JsonPath jsonPath, @NotNull final String targetJsonPath, final boolean allowNonMatching) {
        return this.resolveTargetPaths(targetJsonPath, this.queryForPaths(jsonPath), allowNonMatching);
    }

    @NotNull
    public final List<ResolvedPaths> queryAndResolveTargetPaths(@NotNull final JsonPath jsonPath, @NotNull final String targetJsonPath) {
        return this.resolveTargetPaths(targetJsonPath, this.queryForPaths(jsonPath), false);
    }

    public final void applyUpdate(@NotNull final ResolvedPaths target, @NotNull final Object jsonFragment) {
        PathUtilsKt.applyUpdatePath(this.jpathCtx, target.getTargetBasePath(), target.getTargetUpdatePath(), jsonFragment);
    }

    public final void applyUpdate(@NotNull final String targetBasePath, @NotNull final String targetUpdatePath, @NotNull final Object jsonFragment) {
        PathUtilsKt.applyUpdatePath(this.jpathCtx, targetBasePath, targetUpdatePath, jsonFragment);
    }

    public final DocumentContext deleteValue(@NotNull final JsonPath jsonPath) {
        return this.jvalueCtx.delete(jsonPath);
    }

    public final DocumentContext deleteValue(@NotNull final String jsonPath) {
        return this.jvalueCtx.delete(jsonPath);
    }

    public JsonTransformerContext(@NotNull final Object jsonObject, @NotNull final Configuration jpathCfg, @NotNull final Configuration jvalueCfg, @NotNull final Configuration jvalueListCfg) {
        super();
        this.jsonObject = jsonObject;
        this.jpathCfg = jpathCfg;
        this.jvalueCfg = jvalueCfg;
        this.jvalueListCfg = jvalueListCfg;
        this.jpathCtx = JsonPath.using(this.jpathCfg).parse(this.jsonObject);
        this.jvalueCtx = JsonPath.using(this.jvalueCfg).parse(this.jsonObject);
        this.jvalueListCtx = JsonPath.using(this.jvalueListCfg).parse(this.jsonObject);
    }
}