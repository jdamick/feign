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
package feign;

import com.google.gson.reflect.TypeToken;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.net.URI;
import java.util.Date;
import java.util.List;

import static feign.assertj.FeignAssertions.assertThat;
import static java.util.Arrays.asList;
import static org.assertj.core.data.MapEntry.entry;

/**
 * Tests interfaces defined per {@link Contract.Default} are interpreted into expected {@link feign
 * .RequestTemplate template} instances.
 */
public class DefaultContractTest {

  @Rule
  public final ExpectedException thrown = ExpectedException.none();

  Contract.Default contract = new Contract.Default();

  @Test
  public void httpMethods() throws Exception {
    MethodMetadata data = new MethodMetadata();
    contract.parseAndValidatateMetadata(data, Methods.class.getDeclaredMethod("post"));
    assertThat(data.template()).hasMethod("POST");

    contract.parseAndValidatateMetadata(data, Methods.class.getDeclaredMethod("put"));
    assertThat(data.template()).hasMethod("PUT");

    contract.parseAndValidatateMetadata(data, Methods.class.getDeclaredMethod("get"));
    assertThat(data.template()).hasMethod("GET");

    contract.parseAndValidatateMetadata(data, Methods.class.getDeclaredMethod("delete"));
    assertThat(data.template()).hasMethod("DELETE");
  }

  @Test
  public void bodyParamIsGeneric() throws Exception {
    MethodMetadata md = new MethodMetadata();
    contract.parseAndValidatateMetadata(md,
            BodyParams.class.getDeclaredMethod("post", List.class));

    assertThat(md.bodyIndex())
        .isEqualTo(0);
    assertThat(md.bodyType())
        .isEqualTo(new TypeToken<List<String>>() {
        }.getType());
  }

  @Test
  public void tooManyBodies() throws Exception {
    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("Method has too many Body");
    MethodMetadata md = new MethodMetadata();
    contract.parseAndValidatateMetadata(
        md, BodyParams.class.getDeclaredMethod("tooMany", List.class, List.class));
  }

  @Test
  public void customMethodWithoutPath() throws Exception {
    MethodMetadata md = new MethodMetadata();
    contract.parseAndValidatateMetadata(md, CustomMethod.class.getDeclaredMethod("patch"));
    assertThat(md.template()).hasMethod("PATCH").hasUrl("");
  }

  @Test
  public void queryParamsInPathExtract() throws Exception {
    MethodMetadata md = new MethodMetadata();
    contract.parseAndValidatateMetadata(md, WithQueryParamsInPath.class.getDeclaredMethod("none"));
    assertThat(md.template())
        .hasUrl("/")
        .hasQueries();

    md = new MethodMetadata();
    contract.parseAndValidatateMetadata(md, WithQueryParamsInPath.class.getDeclaredMethod("one"));
    assertThat(md.template())
        .hasUrl("/")
        .hasQueries(
            entry("Action", asList("GetUser"))
        );

    md = new MethodMetadata();
    contract.parseAndValidatateMetadata(md, WithQueryParamsInPath.class.getDeclaredMethod("two"));
    assertThat(md.template())
        .hasUrl("/")
        .hasQueries(
            entry("Action", asList("GetUser")),
            entry("Version", asList("2010-05-08"))
        );

    md = new MethodMetadata();
    contract.parseAndValidatateMetadata(md, WithQueryParamsInPath.class.getDeclaredMethod("three"));
    assertThat(md.template())
        .hasUrl("/")
        .hasQueries(
            entry("Action", asList("GetUser")),
            entry("Version", asList("2010-05-08")),
            entry("limit", asList("1"))
        );

    md = new MethodMetadata();
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
  public void bodyWithoutParameters() throws Exception {
    MethodMetadata md = new MethodMetadata();
    contract.parseAndValidatateMetadata(md, BodyWithoutParameters.class.getDeclaredMethod("post"));

    assertThat(md.template())
        .hasBody("<v01:getAccountsListOfUser/>");
  }

  @Test
  public void producesAddsContentTypeHeader() throws Exception {
    MethodMetadata md = new MethodMetadata();
    contract.parseAndValidatateMetadata(md, BodyWithoutParameters.class.getDeclaredMethod("post"));

    assertThat(md.template())
        .hasHeaders(
            entry("Content-Type", asList("application/xml")),
            entry("Content-Length", asList(String.valueOf(md.template().body().length)))
        );
  }

  @Test
  public void withPathAndURIParam() throws Exception {
    MethodMetadata md = new MethodMetadata();
    contract.parseAndValidatateMetadata(md,
        WithURIParam.class.getDeclaredMethod("uriParam", String.class, URI.class, String.class));

    assertThat(md.indexToName())
        .containsExactly(
            entry(0, asList("1")),
            // Skips 1 as it is a url index!
            entry(2, asList("2"))
        );

    assertThat(md.urlIndex()).isEqualTo(1);
  }

  @Test
  public void pathAndQueryParams() throws Exception {
    MethodMetadata md = new MethodMetadata();
    contract.parseAndValidatateMetadata(md, WithPathAndQueryParams.class.getDeclaredMethod
            ("recordsByNameAndType", int.class, String.class, String.class));

    assertThat(md.template())
        .hasQueries(entry("name", asList("{name}")), entry("type", asList("{type}")));

    assertThat(md.indexToName()).containsExactly(
        entry(0, asList("domainId")),
        entry(1, asList("name")),
        entry(2, asList("type"))
    );
  }

  @Test
  public void bodyWithTemplate() throws Exception {
    MethodMetadata md = new MethodMetadata();
    contract.parseAndValidatateMetadata(md, FormParams.class.getDeclaredMethod("login",
            String.class, String.class, String.class));

    assertThat(md.template())
        .hasBodyTemplate(
            "%7B\"customer_name\": \"{customer_name}\", \"user_name\": \"{user_name}\", \"password\": \"{password}\"%7D");
  }

  @Test
  public void formParamsParseIntoIndexToName() throws Exception {
    MethodMetadata md = new MethodMetadata();
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
    MethodMetadata md = new MethodMetadata();
    contract.parseAndValidatateMetadata(md, FormParams.class.getDeclaredMethod("login",
            String.class, String.class, String.class));

    assertThat(md.bodyType()).isNull();
  }

  @Test
  public void headerParamsParseIntoIndexToName() throws Exception {
    MethodMetadata md = new MethodMetadata();
    contract.parseAndValidatateMetadata(md,
            HeaderParams.class.getDeclaredMethod("logout", String.class));

    assertThat(md.template())
        .hasHeaders(entry("Auth-Token", asList("{authToken}", "Foo")));

    assertThat(md.indexToName())
        .containsExactly(entry(0, asList("authToken")));
  }

  @Test
  public void customExpander() throws Exception {
    MethodMetadata md = new MethodMetadata();
    contract.parseAndValidatateMetadata(md,
            CustomExpander.class.getDeclaredMethod("date", Date.class));

    assertThat(md.indexToExpanderClass())
        .containsExactly(entry(0, DateToMillis.class));
  }

  interface Methods {

    @RequestLine("POST /")
    void post();

    @RequestLine("PUT /")
    void put();

    @RequestLine("GET /")
    void get();

    @RequestLine("DELETE /")
    void delete();
  }

  interface BodyParams {

    @RequestLine("POST")
    Response post(List<String> body);

    @RequestLine("POST")
    Response tooMany(List<String> body, List<String> body2);
  }

  interface CustomMethod {

    @RequestLine("PATCH")
    Response patch();
  }

  interface WithQueryParamsInPath {

    @RequestLine("GET /")
    Response none();

    @RequestLine("GET /?Action=GetUser")
    Response one();

    @RequestLine("GET /?Action=GetUser&Version=2010-05-08")
    Response two();

    @RequestLine("GET /?Action=GetUser&Version=2010-05-08&limit=1")
    Response three();

    @RequestLine("GET /?flag&Action=GetUser&Version=2010-05-08")
    Response empty();
  }

  interface BodyWithoutParameters {

    @RequestLine("POST /")
    @Headers("Content-Type: application/xml")
    @Body("<v01:getAccountsListOfUser/>")
    Response post();
  }

  interface WithURIParam {

    @RequestLine("GET /{1}/{2}")
    Response uriParam(@Param("1") String one, URI endpoint, @Param("2") String two);
  }

  interface WithPathAndQueryParams {

    @RequestLine("GET /domains/{domainId}/records?name={name}&type={type}")
    Response recordsByNameAndType(@Param("domainId") int id, @Param("name") String nameFilter,
                                  @Param("type") String typeFilter);
  }

  interface FormParams {

    @RequestLine("POST /")
    @Body("%7B\"customer_name\": \"{customer_name}\", \"user_name\": \"{user_name}\", \"password\": \"{password}\"%7D")
    void login(
        @Param("customer_name") String customer,
        @Param("user_name") String user, @Param("password") String password);
  }

  interface HeaderParams {

    @RequestLine("POST /")
    @Headers({"Auth-Token: {authToken}", "Auth-Token: Foo"})
    void logout(@Param("authToken") String token);
  }

  interface CustomExpander {

    @RequestLine("POST /?date={date}")
    void date(@Param(value = "date", expander = DateToMillis.class) Date date);
  }

  class DateToMillis implements Param.Expander {

    @Override
    public String expand(Object value) {
      return String.valueOf(((Date) value).getTime());
    }
  }
}
