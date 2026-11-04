package org.wso2.carbon.mcp.transformer.impl;

import com.google.gson.JsonObject;
import org.apache.commons.lang3.StringUtils;
import org.wso2.carbon.mcp.transformer.exception.MCPRequestResolverException;
import org.wso2.carbon.mcp.transformer.exception.SchemaMappingException;
import org.wso2.carbon.mcp.transformer.model.Param;
import org.wso2.carbon.mcp.transformer.model.ResolvedRequest;
import org.wso2.carbon.mcp.transformer.model.SchemaMapping;
import org.wso2.carbon.mcp.transformer.model.SchemaMappingParser;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.mcp.transformer.util.ResolverConstants;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * RequestResolver is responsible for resolving request parameters based on a given schema.
 */
public class RequestResolver {
    private static final Log log = LogFactory.getLog(RequestResolver.class);

    private final SchemaMappingParser parser;

    public RequestResolver(SchemaMappingParser parser) {
        this.parser = parser;
    }

    //todo: Should we accept entire MCP request object instead of arguments only ?
    /**
     * Resolves the input parameters based on the provided schema JSON.
     * @param schema        schema to be parsed
     * @param inputArguments   request arguments
     * @return resolved request
     */
    public ResolvedRequest resolve(Object schema, Map<String, Object> inputArguments) throws MCPRequestResolverException {
        ResolvedRequest resolvedRequest = new ResolvedRequest();
        try {
            SchemaMapping mapping = parser.parse(schema);

            // Implement the logic to resolve the request based on the mapping and inputArguments
            resolvePathParams(mapping, inputArguments, resolvedRequest);
            resolveQueryParams(mapping, inputArguments, resolvedRequest);
            resolveHeaderParams(mapping, inputArguments, resolvedRequest);
            resolveBody(mapping, inputArguments, resolvedRequest);
        } catch (SchemaMappingException e) {
            String msg = "Error while parsing schema mapping";
            log.error(msg, e);
            throw new MCPRequestResolverException(msg, e);
        }

        return resolvedRequest;
    }

    /**
     * Resolve path parameters from the input arguments based on the schema mapping.
     *
     * @param schemaMapping     schema mapping
     * @param inputArguments    input arguments
     * @param resolvedRequest   resolved request object to populate
     * @throws MCPRequestResolverException  if a required path parameter is missing
     */
    private void resolvePathParams(SchemaMapping schemaMapping, Map<String, Object> inputArguments,
                                   ResolvedRequest resolvedRequest) throws MCPRequestResolverException {
        for (Param pathParam : schemaMapping.getPathParams()) {
            Object paramObj = inputArguments.get(pathParam.getName());
            String paramValue = paramObj != null ? paramObj.toString() : null;
            if (inputArguments.containsKey(pathParam.getName()) && !StringUtils.isEmpty(paramValue)) {
                //trim and url encode the path param value
                String encodedPathParam = null;
                try {
                    encodedPathParam = URLEncoder.encode(paramValue.trim(), String.valueOf(StandardCharsets.UTF_8))
                            .replace("+", "%20");
                } catch (UnsupportedEncodingException e) {
                    String msg = "Unsupported encoding exception while encoding path parameter: " + pathParam.getName();
                    log.error(msg);
                    throw new MCPRequestResolverException(msg, e);
                }
                resolvedRequest.getPathParams().put(pathParam.getName(), encodedPathParam);
            } else {
                String msg = "Missing required path parameter: " + pathParam.getName() + " in the mcp request";
                log.error(msg);
                throw new MCPRequestResolverException(msg);
            }
        }
    }

    /**
     * Resolve query parameters from the input arguments based on the schema mapping.
     *
     * @param schemaMapping     schema mapping
     * @param inputArguments    input arguments
     * @param resolvedRequest   resolved request object to populate
     * @throws MCPRequestResolverException  if a required query parameter is missing
     */
    private void resolveQueryParams(SchemaMapping schemaMapping, Map<String, Object> inputArguments,
                                    ResolvedRequest resolvedRequest) throws MCPRequestResolverException {
        for (Param queryParam : schemaMapping.getQueryParams()) {
            String paramName = queryParam.getName();
            Object paramObj = inputArguments.get(queryParam.getName());
            String paramValue = paramObj != null ? paramObj.toString() : null;

            if (StringUtils.isEmpty(paramValue)) {
                if (queryParam.isRequired()) {
                    String msg = "Missing required query parameter: " + paramName + " in the mcp request";
                    log.error(msg);
                    throw new MCPRequestResolverException(msg);
                } else {
                    if (log.isDebugEnabled()) {
                        log.debug("Optional query parameter: " + paramName + " is not provided in the mcp request");
                    }
                    continue;
                }
            }

            // Trim and url encode the query param value
            String encodedQueryParam = null;
            try {
                encodedQueryParam = URLEncoder.encode(paramValue.trim(), String.valueOf(StandardCharsets.UTF_8));
            } catch (UnsupportedEncodingException e) {
                String msg = "Unsupported encoding exception while encoding query parameter: " + queryParam.getName();
                log.error(msg);
                throw new MCPRequestResolverException(msg, e);
            }
            resolvedRequest.getQueryParams().put(paramName, encodedQueryParam);
        }
    }

    /**
     * Resolve header parameters from the input arguments based on the schema mapping.
     *
     * @param schemaMapping     schema mapping
     * @param inputArguments    input arguments
     * @param resolvedRequest   resolved request object to populate
     * @throws MCPRequestResolverException  if a required header parameter is missing
     */
    private void resolveHeaderParams(SchemaMapping schemaMapping, Map<String, Object> inputArguments,
                                     ResolvedRequest resolvedRequest) throws MCPRequestResolverException {

        for (Param headerParam : schemaMapping.getHeaderParams()) {
            String paramName = headerParam.getName();
            Object paramObj = inputArguments.get(headerParam.getName());
            String paramValue = paramObj != null ? paramObj.toString() : null;

            if (StringUtils.isEmpty(paramValue)) {
                if (headerParam.isRequired()) {
                    String msg = "Missing required header parameter: " + paramName + " in the mcp request";
                    log.error(msg);
                    throw new MCPRequestResolverException(msg);
                } else {
                    if (log.isDebugEnabled()) {
                        log.debug("Optional header parameter: " + paramName + " is not provided in the mcp request");
                    }
                    continue;
                }
            }
            resolvedRequest.getHeaderParams().put(paramName, paramValue);
        }
    }

    /**
     * Resolve the request body from the input arguments based on the schema mapping.
     *
     * @param schemaMapping     schema mapping
     * @param inputArguments    input arguments
     * @param resolvedRequest   resolved request object to populate
     * @throws MCPRequestResolverException  if the request body is required but missing
     */
    private void resolveBody(SchemaMapping schemaMapping, Map<String, Object> inputArguments,
                             ResolvedRequest resolvedRequest) throws MCPRequestResolverException {
        if (schemaMapping.isHasBody()) {
            JsonObject body = (JsonObject) inputArguments.get(ResolverConstants.REQUEST_BODY_KEY);
            if (body != null && !body.isEmpty()) {
                resolvedRequest.setBody(body);
            } else {
                String msg = "Missing required request body in the mcp request";
                log.error(msg);
                throw new MCPRequestResolverException(msg);
            }
        }
    }

}

