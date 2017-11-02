package com.ebsco.platform.shared.mappingsengine.core;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import lombok.Getter;

import java.util.List;

public class JsonTransformerContext {

    @Getter
    private final Object jsonObject;
    private final DocumentContext jpathCtx;
    private final DocumentContext jvalueCtx;
    private final DocumentContext jvalueListCtx;

    public JsonTransformerContext(Object jsonObject, Configuration jpathCfg, Configuration jvalueCfg, Configuration jvalueListCfg) {
        this.jsonObject = jsonObject;
        this.jpathCtx = JsonPath.using(jpathCfg).parse(jsonObject);
        this.jvalueCtx = JsonPath.using(jvalueCfg).parse(jsonObject);
        this.jvalueListCtx = JsonPath.using(jvalueListCfg).parse(jsonObject);
    }

    // we might be able to call CompiledPath.eval to get both values and paths at same time, but we lose path caching and have to do that ourselves

    public List<String> queryForPaths(String jsonPath) {
        return jpathCtx.read(jsonPath);
    }

    public List<String> queryForPaths(JsonPath jsonPath) {
        return jpathCtx.read(jsonPath);
    }

    public List<Object> queryForValues(String jsonPath) {
        return jvalueListCtx.read(jsonPath);
    }

    public List<Object> queryForValues(JsonPath jsonPath) {
        return jvalueListCtx.read(jsonPath);
    }

    public Object queryForValue(String jsonPath) {
        return jvalueCtx.read(jsonPath);
    }

    public Object queryForValue(JsonPath jsonPath) {
        return jvalueCtx.read(jsonPath);
    }

    public List<ResolvedPaths> resolveTargetPaths(String targetJsonPath, List<String> relativeToPath, boolean allowNonMatching) {
        return PathUtils.resolveTargetPaths(jpathCtx, targetJsonPath, relativeToPath, allowNonMatching);
    }

    public List<ResolvedPaths> resolveTargetPaths(String targetJsonPath, List<String> relativeToPath) {
        return PathUtils.resolveTargetPaths(jpathCtx, targetJsonPath, relativeToPath, false);
    }

    public List<ResolvedPaths> queryAndResolveTargetPaths(String jsonPath, String targetJsonPath, boolean allowNonMatching) {
        return resolveTargetPaths(targetJsonPath, queryForPaths(jsonPath), allowNonMatching);
    }

    public List<ResolvedPaths> queryAndResolveTargetPaths(String jsonPath, String targetJsonPath) {
        return resolveTargetPaths(targetJsonPath, queryForPaths(jsonPath), false);
    }

    public List<ResolvedPaths> queryAndResolveTargetPaths(JsonPath jsonPath, String targetJsonPath, boolean allowNonMatching) {
        return resolveTargetPaths(targetJsonPath, queryForPaths(jsonPath), allowNonMatching);
    }

    public List<ResolvedPaths> queryAndResolveTargetPaths(JsonPath jsonPath, String targetJsonPath) {
        return resolveTargetPaths(targetJsonPath, queryForPaths(jsonPath), false);
    }

    public void applyUpdate(ResolvedPaths target, Object jsonFragment) {
        PathUtils.applyUpdatePath(jpathCtx, target.getTargetBasePath(), target.getTargetUpdatePath(), jsonFragment);
    }

    public void applyUpdate(String targetBasePath, String targetUpdatePath, Object jsonFragment) {
        PathUtils.applyUpdatePath(jpathCtx, targetBasePath, targetUpdatePath, jsonFragment);
    }

    public void deleteValue(JsonPath jsonPath) {
        jvalueCtx.delete(jsonPath);
    }

    public void deleteValue(String jsonPath) {
        jvalueCtx.delete(jsonPath);
    }
}
