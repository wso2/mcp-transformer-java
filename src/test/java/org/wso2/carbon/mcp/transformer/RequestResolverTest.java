/*
 * Copyright (c) 2025, WSO2 LLC. (http://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.mcp.transformer;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;
import org.wso2.carbon.mcp.transformer.impl.PrefixBasedSchemaMappingParser;
import org.wso2.carbon.mcp.transformer.impl.RequestResolver;
import org.wso2.carbon.mcp.transformer.model.ResolvedRequest;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;

/**
 * Test class for RequestResolver.
 */
public class RequestResolverTest {
    Map<String, Object> inputArgumentsForBasicSchema = Map.of("id", 123, "query", "name:John", "OrgID" , 456,
            "requestBody", Map.of("firstName", "John", "lastName", "Doe"));

//    @Test
//    public void testResolveRequest() throws Exception{
//        String schemaJson = IOUtils.toString(Objects.requireNonNull(
//                getClass().getClassLoader().getResourceAsStream("basic-schema.json")), StandardCharsets.UTF_8
//        );
//        PrefixBasedSchemaMappingParser parser = new PrefixBasedSchemaMappingParser();
//        RequestResolver requestResolver = new RequestResolver(parser);
//
//        ResolvedRequest resolvedRequest = requestResolver.resolve(schemaJson, inputArgumentsForBasicSchema);
//
//        Map<String, Object> pathParams = resolvedRequest.getPathParams();
//        Assert.assertNotNull(pathParams);
//        Assert.assertEquals(1, pathParams.size());
//        Assert.assertEquals("123", pathParams.get("id"));
//
//        Map<String, Object> queryParams = resolvedRequest.getQueryParams();
//        Assert.assertNotNull(queryParams);
//        Assert.assertEquals(1, queryParams.size());
//        Assert.assertEquals("name%3AJohn", queryParams.get("query"));
//
//        Map<String, Object> headerParams = resolvedRequest.getHeaderParams();
//        Assert.assertNotNull(headerParams);
//        Assert.assertEquals(1, headerParams.size());
//        Assert.assertEquals("456", headerParams.get("OrgID"));
//    }

    @Test
    public void testPathParamWithSpacesIsPercentEncoded() throws Exception {
        ResolvedRequest resolvedRequest = resolveWithoutBody(Map.of("id", "HP  1234567890"));

        // Spaces must become %20, not '+', which a backend reads as a literal plus sign in a URL path
        Assert.assertEquals("HP%20%201234567890", resolvedRequest.getPathParams().get("id"));
    }

    @Test
    public void testPathParamWithLiteralPlusIsEncoded() throws Exception {
        ResolvedRequest resolvedRequest = resolveWithoutBody(Map.of("id", "a+b c"));

        Assert.assertEquals("a%2Bb%20c", resolvedRequest.getPathParams().get("id"));
    }

    @Test
    public void testQueryParamWithSpacesIsPercentEncoded() throws Exception {
        ResolvedRequest resolvedRequest = resolveWithoutBody(Map.of("id", "123", "query", "first name:John"));

        Assert.assertEquals("123", resolvedRequest.getPathParams().get("id"));
        Assert.assertEquals("first%20name%3AJohn", resolvedRequest.getQueryParams().get("query"));
    }

    private ResolvedRequest resolveWithoutBody(Map<String, Object> inputArguments) throws Exception {
        String schemaJson = IOUtils.toString(Objects.requireNonNull(
                getClass().getClassLoader().getResourceAsStream("schema-no-request-body.json")),
                StandardCharsets.UTF_8);
        RequestResolver requestResolver = new RequestResolver(new PrefixBasedSchemaMappingParser());
        return requestResolver.resolve(schemaJson, inputArguments);
    }
}
