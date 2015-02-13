/*
 * Copyright 2013 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package feign.jaxrs;

import feign.MethodMetadata;
import feign.MethodMetadataUtil;
import feign.Response;
import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.net.URI;
import java.util.List;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static feign.MethodMetadataUtil.findMethodMetadata;
import static feign.assertj.FeignAssertions.assertThat;
import static java.util.Arrays.asList;
import static org.assertj.core.data.MapEntry.entry;

/**
 * Tests interfaces defined per {@link JAXRSContract} are interpreted into expected {@link feign
 * .RequestTemplate template} instances.
 */
public class JAXRSContractTest {

  private static final List<String> STRING_LIST = null;
  @Rule
  public final ExpectedException thrown = ExpectedException.none();
  JAXRSContract contract = new JAXRSContract();

  @Test
  public void httpMethods() throws Exception {
    MethodMetadata md = MethodMetadataUtil.buildMethodMetadata();
    contract.parseAndValidatateMetadata(md, Methods.class.getDeclaredMethod("post"));
    assertThat(md.template())
        .hasMethod("POST");

    md = MethodMetadataUtil.buildMethodMetadata();
    contract.parseAndValidatateMetadata(md, Methods.class.getDeclaredMethod("put"));
    assertThat(md.template())
        .hasMethod("PUT");

    md = MethodMetadataUtil.buildMethodMetadata();
    contract.parseAndValidatateMetadata(md, Methods.class.getDeclaredMethod("get"));
    assertThat(md.template())
        .hasMethod("GET");

    md = MethodMetadataUtil.buildMethodMetadata();
    contract.parseAndValidatateMetadata(md, Methods.class.getDeclaredMethod("delete"));
    assertThat(md.template())
        .hasMethod("DELETE");
  }

  @Test
  public void customMethodWithoutPath() throws Exception {
    MethodMetadata md = MethodMetadataUtil.buildMethodMetadata();
    contract.parseAndValidatateMetadata(md, CustomMethod.class.getDeclaredMethod("patch"));
    assertThat(md.template())
        .hasMethod("PATCH")
        .hasUrl("");
  }

  @Test
  public void queryParamsInPathExtract() throws Exception {
    MethodMetadata md = MethodMetadataUtil.buildMethodMetadata();
    contract.parseAndValidatateMetadata(md, WithQueryParamsInPath.class.getDeclaredMethod("none"));
    assertThat(md.template())
        .hasUrl("/")
        .hasQueries();

    md = MethodMetadataUtil.buildMethodMetadata();
    contract.parseAndValidatateMetadata(md, WithQueryParamsInPath.class.getDeclaredMethod("one"));
    assertThat(md.template())
        .hasUrl("/")
        .hasQueries(
            entry("Action", asList("GetUser"))
        );

    md = MethodMetadataUtil.buildMethodMetadata();
    contract.parseAndValidatateMetadata(md, WithQueryParamsInPath.class.getDeclaredMethod("two"));
    assertThat(md.template())
        .hasUrl("/")
        .hasQueries(
            entry("Action", asList("GetUser")),
            entry("Version", asList("2010-05-08"))
        );

    md = MethodMetadataUtil.buildMethodMetadata();
    contract.parseAndValidatateMetadata(md, WithQueryParamsInPath.class.getDeclaredMethod("three"));
    assertThat(md.template())
        .hasUrl("/")
        .hasQueries(
            entry("Action", asList("GetUser")),
            entry("Version", asList("2010-05-08")),
            entry("limit", asList("1"))
        );

    md = MethodMetadataUtil.buildMethodMetadata();
    contract.parseAndValidatateMetadata(md, WithQueryParamsInPath.class.getDeclaredMethod("empty"));
    assertThat(md.template())
        .hasUrl("/")
        .hasQueries(
            entry("flag", asList(new String[]{null})),
            entry("Action", asList("GetUser")),
            entry("Version", asList("2010-05-08"))
        );
  }

  @Test
  public void producesAddsAcceptHeader() throws Exception {
    MethodMetadata md = MethodMetadataUtil.buildMethodMetadata();
    contract.parseAndValidatateMetadata(md,
            ProducesAndConsumes.class.getDeclaredMethod("produces"));

    assertThat(md.template())
        .hasHeaders(entry("Accept", asList("application/xml")));
  }

  @Test
  public void producesNada() throws Exception {
    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("Produces.value() was empty on method producesNada");

    MethodMetadata md = MethodMetadataUtil.buildMethodMetadata();
    contract.parseAndValidatateMetadata(md,
            ProducesAndConsumes.class.getDeclaredMethod("producesNada"));
  }

  @Test
  public void producesEmpty() throws Exception {
    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("Produces.value() was empty on method producesEmpty");

    MethodMetadata md = MethodMetadataUtil.buildMethodMetadata();
    contract.parseAndValidatateMetadata(md,
            ProducesAndConsumes.class.getDeclaredMethod("producesEmpty"));
  }

  @Test
  public void consumesAddsContentTypeHeader() throws Exception {
    MethodMetadata md = MethodMetadataUtil.buildMethodMetadata();
    contract.parseAndValidatateMetadata(md,
            ProducesAndConsumes.class.getDeclaredMethod("consumes"));

    assertThat(md.template())
        .hasHeaders(entry("Content-Type", asList("application/xml")));
  }

  @Test
  public void consumesNada() throws Exception {
    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("Consumes.value() was empty on method consumesNada");

    MethodMetadata md = MethodMetadataUtil.buildMethodMetadata();
    contract.parseAndValidatateMetadata(md,
            ProducesAndConsumes.class.getDeclaredMethod("consumesNada"));
  }

  @Test
  public void consumesEmpty() throws Exception {
    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("Consumes.value() was empty on method consumesEmpty");

    MethodMetadata md = MethodMetadataUtil.buildMethodMetadata();
    contract.parseAndValidatateMetadata(md,
            ProducesAndConsumes.class.getDeclaredMethod("consumesEmpty"));
  }

  @Test
  public void bodyParamIsGeneric() throws Exception {
    MethodMetadata md = MethodMetadataUtil.buildMethodMetadata();
    contract.parseAndValidatateMetadata(md, BodyParams.class.getDeclaredMethod("post",
                                                                               List.class));

    assertThat(md.bodyIndex())
        .isEqualTo(0);
    assertThat(md.bodyType())
        .isEqualTo(getClass().getDeclaredField("STRING_LIST").getGenericType());
  }

  @Test
  public void tooManyBodies() throws Exception {
    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("Method has too many Body");

    MethodMetadata md = MethodMetadataUtil.buildMethodMetadata();
    contract.parseAndValidatateMetadata(md,
        BodyParams.class.getDeclaredMethod("tooMany", List.class, List.class));
  }

  @Test
  public void emptyPathOnType() throws Exception {
    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("Path.value() was empty on feign.jaxrs.JAXRSContractTest$EmptyPathOnType");

    contract.parseAndValidatateMetadata(EmptyPathOnType.class);
  }

  private MethodMetadata parsePathOnTypeMethod(String name) throws Exception {
    MethodMetadata data = MethodMetadataUtil.buildMethodMetadata();

    for (Annotation annotation : PathOnType.class.getAnnotations()) {
      contract.processAnnotationOnInterface(data, annotation, PathOnType.class);
    }

    contract.parseAndValidatateMetadata(data,
            PathOnType.class.getDeclaredMethod(name));

    return data;
  }

  @Test
  public void parsePathMethod() throws Exception {
    assertThat(parsePathOnTypeMethod("base").template())
        .hasUrl("/base");

    assertThat(parsePathOnTypeMethod("get").template())
        .hasUrl("/base/specific");
  }

  @Test
  public void emptyPathOnMethod() throws Exception {
    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("Path.value() was empty on emptyPath");

    parsePathOnTypeMethod("emptyPath");
  }

  @Test
  public void emptyPathParam() throws Exception {
    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("PathParam.value() was empty on parameter 0");

    MethodMetadata md = MethodMetadataUtil.buildMethodMetadata();
    contract.parseAndValidatateMetadata(md,
        PathOnType.class.getDeclaredMethod("emptyPathParam", String.class));
  }

  @Test
  public void withPathAndURIParams() throws Exception {
    MethodMetadata md = MethodMetadataUtil.buildMethodMetadata();
    contract.parseAndValidatateMetadata(md,
        WithURIParam.class.getDeclaredMethod("uriParam", String.class, URI.class, String.class));

    assertThat(md.indexToName()).containsExactly(
        entry(0, asList("1")),
        // Skips 1 as it is a url index!
        entry(2, asList("2")));

    assertThat(md.urlIndex()).isEqualTo(1);
  }

  @Test
  public void pathAndQueryParams() throws Exception {
    MethodMetadata md = MethodMetadataUtil.buildMethodMetadata();
    contract.parseAndValidatateMetadata(md, WithPathAndQueryParams.class.getDeclaredMethod
            ("recordsByNameAndType", int.class, String.class, String.class));

    assertThat(md.template())
        .hasQueries(entry("name", asList("{name}")), entry("type", asList("{type}")));

    assertThat(md.indexToName()).containsExactly(entry(0, asList("domainId")),
                                                 entry(1, asList("name")),
                                                 entry(2, asList("type")));
  }

  @Test
  public void emptyQueryParam() throws Exception {
    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("QueryParam.value() was empty on parameter 0");

    MethodMetadata md = MethodMetadataUtil.buildMethodMetadata();
    contract.parseAndValidatateMetadata(md,
        WithPathAndQueryParams.class.getDeclaredMethod("empty", String.class));
  }

  @Test
  public void formParamsParseIntoIndexToName() throws Exception {
    MethodMetadata md = MethodMetadataUtil.buildMethodMetadata();

    contract.parseAndValidatateMetadata(md, FormParams.class.getDeclaredMethod("login",
            String.class, String.class, String.class));

    assertThat(md.formParams())
        .containsExactly("customer_name", "user_name", "password");

    assertThat(md.indexToName()).containsExactly(
        entry(0, asList("customer_name")),
        entry(1, asList("user_name")),
        entry(2, asList("password"))
    );
  }

  /**
   * Body type is only for the body param.
   */
  @Test
  public void formParamsDoesNotSetBodyType() throws Exception {
    MethodMetadata md = MethodMetadataUtil.buildMethodMetadata();
    contract.parseAndValidatateMetadata(md, FormParams.class.getDeclaredMethod("login",
            String.class, String.class, String.class));

    assertThat(md.bodyType()).isNull();
  }

  @Test
  public void emptyFormParam() throws Exception {
    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("FormParam.value() was empty on parameter 0");

    MethodMetadata md = MethodMetadataUtil.buildMethodMetadata();
    contract.parseAndValidatateMetadata(md,
        FormParams.class.getDeclaredMethod("emptyFormParam", String.class));
  }

  @Test
  public void headerParamsParseIntoIndexToName() throws Exception {
    MethodMetadata md = MethodMetadataUtil.buildMethodMetadata();
    contract.parseAndValidatateMetadata(md,
            HeaderParams.class.getDeclaredMethod("logout", String.class));

    assertThat(md.template())
        .hasHeaders(entry("Auth-Token", asList("{Auth-Token}")));

    assertThat(md.indexToName())
        .containsExactly(entry(0, asList("Auth-Token")));
  }

  @Test
  public void emptyHeaderParam() throws Exception {
    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("HeaderParam.value() was empty on parameter 0");

    MethodMetadata md = MethodMetadataUtil.buildMethodMetadata();
    contract.parseAndValidatateMetadata(md,
        HeaderParams.class.getDeclaredMethod("emptyHeaderParam", String.class));
  }

  @Test
  public void pathsWithoutSlashesParseCorrectly() throws Exception {
    List<MethodMetadata> metadata =
            contract.parseAndValidatateMetadata(PathsWithoutAnySlashes.class);
    MethodMetadata md = findMethodMetadata(metadata,
            PathsWithoutAnySlashes.class.getDeclaredMethod("get"));
    assertThat(md.template())
        .hasUrl("/base/specific");
  }

  @Test
  public void pathsWithSomeSlashesParseCorrectly() throws Exception {
    List<MethodMetadata> metadata =
            contract.parseAndValidatateMetadata(PathsWithSomeSlashes.class);
    MethodMetadata md = findMethodMetadata(metadata,
            PathsWithSomeSlashes.class.getDeclaredMethod("get"));
    assertThat(md.template())
        .hasUrl("/base/specific");
  }

  @Test
  public void pathsWithSomeOtherSlashesParseCorrectly() throws Exception {
    List<MethodMetadata> metadata =
            contract.parseAndValidatateMetadata(PathsWithSomeOtherSlashes.class);
    MethodMetadata md = findMethodMetadata(metadata,
            PathsWithSomeOtherSlashes.class.getDeclaredMethod("get"));
    assertThat(md.template())
        .hasUrl("/base/specific");

  }

  interface Methods {

    @POST
    void post();

    @PUT
    void put();

    @GET
    void get();

    @DELETE
    void delete();
  }

  interface CustomMethod {

    @PATCH
    Response patch();

    @Target({ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    @HttpMethod("PATCH")
    public @interface PATCH {

    }
  }

  interface WithQueryParamsInPath {

    @GET
    @Path("/")
    Response none();

    @GET
    @Path("/?Action=GetUser")
    Response one();

    @GET
    @Path("/?Action=GetUser&Version=2010-05-08")
    Response two();

    @GET
    @Path("/?Action=GetUser&Version=2010-05-08&limit=1")
    Response three();

    @GET
    @Path("/?flag&Action=GetUser&Version=2010-05-08")
    Response empty();
  }

  interface ProducesAndConsumes {

    @GET
    @Produces("application/xml")
    Response produces();

    @GET
    @Produces({})
    Response producesNada();

    @GET
    @Produces({""})
    Response producesEmpty();

    @POST
    @Consumes("application/xml")
    Response consumes();

    @POST
    @Consumes({})
    Response consumesNada();

    @POST
    @Consumes({""})
    Response consumesEmpty();
  }

  interface BodyParams {

    @POST
    Response post(List<String> body);

    @POST
    Response tooMany(List<String> body, List<String> body2);
  }

  @Path("")
  interface EmptyPathOnType {

    @GET
    Response base();
  }

  @Path("/base")
  interface PathOnType {

    @GET
    Response base();

    @GET
    @Path("/specific")
    Response get();

    @GET
    @Path("")
    Response emptyPath();

    @GET
    @Path("/{param}")
    Response emptyPathParam(@PathParam("") String empty);
  }

  interface WithURIParam {

    @GET
    @Path("/{1}/{2}")
    Response uriParam(@PathParam("1") String one, URI endpoint, @PathParam("2") String two);
  }

  interface WithPathAndQueryParams {

    @GET
    @Path("/domains/{domainId}/records")
    Response recordsByNameAndType(@PathParam("domainId") int id,
                                  @QueryParam("name") String nameFilter,
                                  @QueryParam("type") String typeFilter);

    @GET
    Response empty(@QueryParam("") String empty);
  }

  interface FormParams {

    @POST
    void login(
        @FormParam("customer_name") String customer,
        @FormParam("user_name") String user, @FormParam("password") String password);

    @GET
    Response emptyFormParam(@FormParam("") String empty);
  }

  interface HeaderParams {

    @POST
    void logout(@HeaderParam("Auth-Token") String token);

    @GET
    Response emptyHeaderParam(@HeaderParam("") String empty);
  }

  @Path("base")
  interface PathsWithoutAnySlashes {

    @GET
    @Path("specific")
    Response get();
  }

  @Path("/base")
  interface PathsWithSomeSlashes {

    @GET
    @Path("specific")
    Response get();
  }

  @Path("base")
  interface PathsWithSomeOtherSlashes {

    @GET
    @Path("/specific")
    Response get();
  }
}
