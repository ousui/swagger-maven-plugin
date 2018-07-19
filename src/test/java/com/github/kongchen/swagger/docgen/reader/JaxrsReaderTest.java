package com.github.kongchen.swagger.docgen.reader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.apache.maven.plugin.logging.Log;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.jaxrs.ext.SwaggerExtension;
import io.swagger.jaxrs.ext.SwaggerExtensions;
import io.swagger.models.Operation;
import io.swagger.models.Swagger;
import io.swagger.models.Tag;
import io.swagger.models.parameters.HeaderParameter;
import io.swagger.models.parameters.Parameter;
import io.swagger.models.parameters.QueryParameter;
import io.swagger.models.parameters.RefParameter;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

public class JaxrsReaderTest {
    @Mock
    private Log log;

    private JaxrsReader reader;

    List<SwaggerExtension> extensions = SwaggerExtensions.getExtensions();

    @BeforeMethod
    public void setup() {
        reader = new JaxrsReader(new Swagger(), log);
    }

    @AfterMethod
    public void resetExtenstions() {
        SwaggerExtensions.setExtensions(extensions);
    }

    @Test
    public void ignoreClassIfNoApiAnnotation() {
        Swagger result = reader.read(NotAnnotatedApi.class);

        assertEmptySwaggerResponse(result);
    }

    @Test
    public void ignoreApiIfHiddenAttributeIsTrue() {
        Swagger result = reader.read(HiddenApi.class);

        assertEmptySwaggerResponse(result);
    }

    @Test
    public void includeApiIfHiddenParameterIsTrueAndApiHiddenAttributeIsTrue() {
        Swagger result = reader.read(HiddenApi.class, "", null, true, new String[0], new String[0], new HashMap<String, Tag>(), new ArrayList<Parameter>());

        assertNotNull(result, "No Swagger object created");
        assertFalse(result.getTags().isEmpty(), "Should contain api tags");
        assertFalse(result.getPaths().isEmpty(), "Should contain operation paths");
    }

    @Test
    public void discoverApiOperation() {
        Tag expectedTag = new Tag();
        expectedTag.name("atag");
        Swagger result = reader.read(AnApi.class);

        assertSwaggerResponseContents(expectedTag, result);
    }

    @Test
    public void createNewSwaggerInstanceIfNoneProvided() {
        JaxrsReader nullReader = new JaxrsReader(null, log);
        Tag expectedTag = new Tag();
        expectedTag.name("atag");
        Swagger result = nullReader.read(AnApi.class);

        assertSwaggerResponseContents(expectedTag, result);
    }

    private void assertEmptySwaggerResponse(Swagger result) {
        assertNotNull(result, "No Swagger object created");
        assertNull(result.getTags(), "Should not have any tags");
        assertNull(result.getPaths(), "Should not have any paths");
    }

    private void assertSwaggerResponseContents(Tag expectedTag, Swagger result) {
        assertNotNull(result, "No Swagger object created");
        assertFalse(result.getTags().isEmpty(), "Should contain api tags");
        assertTrue(result.getTags().contains(expectedTag), "Expected tag missing");
        assertFalse(result.getPaths().isEmpty(), "Should contain operation paths");
        assertTrue(result.getPaths().containsKey("/apath"), "Path missing from paths map");
        io.swagger.models.Path path = result.getPaths().get("/apath");
        assertFalse(path.getOperations().isEmpty(), "Should be a get operation");
    }

    @Test
    public void createCommonParameters() {
        reader = new JaxrsReader(new Swagger(), Mockito.mock(Log.class));
        Swagger result = reader.read(CommonParametersApi.class);
        Parameter headerParam = result.getParameter("headerParam");
        assertTrue(headerParam instanceof HeaderParameter);
        Parameter queryParam = result.getParameter("queryParam");
        assertTrue(queryParam instanceof QueryParameter);

        result = reader.read(CommonParametersApiWithPathAnnotation.class);
        Operation get = result.getPath("/apath").getGet();
        List<Parameter> parameters = get.getParameters();
        for (Parameter parameter : parameters) {
            assertTrue(parameter instanceof RefParameter);
        }
    }

    @Test
    public void ignoreCommonParametersWithPathAnnotation() {
        reader = new JaxrsReader(new Swagger(), Mockito.mock(Log.class));
        Swagger result = reader.read(CommonParametersApiWithPathAnnotation.class);
        assertNull(result.getParameter("headerParam"));
        assertNull(result.getParameter("queryParam"));
    }

    @Api(tags = "atag")
    @Path("/apath")
    static class AnApi {
        @ApiOperation(value = "Get a model.")
        @GET
        public Response getOperation() {
            return Response.ok().build();
        }
    }

    @Api(hidden = true, tags = "atag")
    @Path("/hidden/path")
    static class HiddenApi {
        @ApiOperation(value = "Get a model.")
        @GET
        public Response getOperation() {
            return Response.ok().build();
        }
    }

    @Path("/apath")
    static class NotAnnotatedApi {
    }

    @Api
    static class CommonParametersApi {
        @HeaderParam("headerParam")
        String headerParam;

        @QueryParam("queryParam")
        String queryParam;
    }

    @Api
    @Path("/apath")
    static class CommonParametersApiWithPathAnnotation {
        @GET
        public Response getOperation(
            @HeaderParam("headerParam") String headerParam,
            @QueryParam("queryParam") String queryParam) {
            return Response.ok().build();
        }
    }
}
