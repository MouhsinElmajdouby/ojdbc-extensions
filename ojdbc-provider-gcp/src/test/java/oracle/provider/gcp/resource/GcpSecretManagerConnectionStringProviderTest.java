/*
 ** Copyright (c) 2024 Oracle and/or its affiliates.
 **
 ** The Universal Permissive License (UPL), Version 1.0
 **
 ** Subject to the condition set forth below, permission is hereby granted to any
 ** person obtaining a copy of this software, associated documentation and/or data
 ** (collectively the "Software"), free of charge and under any and all copyright
 ** rights in the Software, and any and all patent rights owned or freely
 ** licensable by each licensor hereunder covering either (i) the unmodified
 ** Software as contributed to or provided by such licensor, or (ii) the Larger
 ** Works (as defined below), to deal in both
 **
 ** (a) the Software, and
 ** (b) any piece of software and/or hardware listed in the lrgrwrks.txt file if
 ** one is included with the Software (each a "Larger Work" to which the Software
 ** is contributed by such licensors),
 **
 ** without restriction, including without limitation the rights to copy, create
 ** derivative works of, display, perform, and distribute the Software and make,
 ** use, sell, offer for sale, import, export, have made, and have sold the
 ** Software and the Larger Work(s), and to sublicense the foregoing rights on
 ** either these or other terms.
 **
 ** This license is subject to the following condition:
 ** The above copyright notice and either this complete permission notice or at
 ** a minimum a reference to the UPL must be included in all copies or
 ** substantial portions of the Software.
 **
 ** THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 ** IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 ** FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 ** AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 ** LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 ** OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 ** SOFTWARE.
 */

package oracle.provider.gcp.resource;

import oracle.jdbc.provider.TestProperties;
import oracle.jdbc.spi.ConnectionStringProvider;
import oracle.jdbc.spi.OracleResourceProvider;
import oracle.jdbc.spi.OracleResourceProvider.Parameter;
import oracle.provider.gcp.GcpTestProperty;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static oracle.jdbc.provider.resource.ResourceProviderTestUtil.createParameterValues;
import static oracle.jdbc.provider.resource.ResourceProviderTestUtil.findProvider;
import static org.junit.jupiter.api.Assertions.*;

public class GcpSecretManagerConnectionStringProviderTest {

  private static final ConnectionStringProvider PROVIDER =
    findProvider(
      ConnectionStringProvider.class,
        "ojdbc-provider-gcp-secretmanager-tnsnames");

  /**
   * Verifies that {@link ConnectionStringProvider#getParameters()} includes parameters
   * to configure a vault URL and secret name.
   */
  @Test
  public void testGetParameters() {
    Collection<? extends Parameter> parameters = PROVIDER.getParameters();
    assertNotNull(parameters);

    OracleResourceProvider.Parameter vaultUrlParameter =
            parameters.stream()
                    .filter(parameter -> "secretVersionName".equals(parameter.name()))
                    .findFirst()
                    .orElseThrow(AssertionError::new);
    assertFalse(vaultUrlParameter.isSensitive());
    assertTrue(vaultUrlParameter.isRequired());
    assertNull(vaultUrlParameter.defaultValue());

    Parameter aliasParameter =
      parameters.stream()
        .filter(parameter -> "tns-alias".equals(parameter.name()))
        .findFirst()
        .orElseThrow(AssertionError::new);
    assertTrue(aliasParameter.isSensitive());
    assertTrue(aliasParameter.isRequired());
    assertNull(aliasParameter.defaultValue());
  }

  @Test
  public void testValidAlias() {
    Map<String, String> testParameters = new HashMap<>();
    testParameters.put("secretVersionName",
      TestProperties.getOrAbort(
              GcpTestProperty
                .GCP_SECRET_MANAGER_TNS_NAMES_SECRET_VERSION)
    );

    testParameters.put("tns-alias",
      TestProperties.getOrAbort(GcpTestProperty.GCP_SECRET_MANAGER_TNS_ALIAS_SECRET_NAME));

    Map<Parameter, CharSequence> parameterValues =
      createParameterValues(PROVIDER, testParameters);

    String connectionString = PROVIDER.getConnectionString(parameterValues);
    assertNotNull(connectionString);
  }

  @Test
  public void testInvalidOrNonExistentAlias() {
    Map<String, String> testParameters = new HashMap<>();
    testParameters.put("secretVersionName",
            TestProperties.getOrAbort(
                    GcpTestProperty
                      .GCP_SECRET_MANAGER_TNS_NAMES_SECRET_VERSION)
    );

    testParameters.put("tns-alias", "INVALID_ALIAS");

    Map<Parameter, CharSequence> parameterValues =
            createParameterValues(PROVIDER, testParameters);

    assertThrows(IllegalArgumentException.class,
            () -> PROVIDER.getConnectionString(parameterValues),
            "Expected IllegalArgumentException for invalid alias"
    );
  }

  @Test
  public void testMissingAliasParameter() {
    Map<String, String> testParameters = new HashMap<>();
    testParameters.put("secretVersionName",
            TestProperties.getOrAbort(GcpTestProperty.GCP_SECRET_MANAGER_TNS_NAMES_SECRET_VERSION));

    Map<Parameter, CharSequence> parameterValues =
            createParameterValues(PROVIDER, testParameters);

    assertThrows(IllegalArgumentException.class,
            () -> PROVIDER.getConnectionString(parameterValues),
            "Expected IllegalArgumentException when tns-alias parameter is missing"
    );
  }

  @Test
  public void testNonBase64EncodedTnsnamesContent() {
    Map<String, String> testParameters = new HashMap<>();
    testParameters.put("secretVersionName",
            TestProperties.getOrAbort(GcpTestProperty.GCP_NON_BASE64_TNS_NAMES_SECRET_VERSION));

    testParameters.put("tns-alias",
            TestProperties.getOrAbort(GcpTestProperty.GCP_SECRET_MANAGER_TNS_ALIAS_SECRET_NAME));

    Map<Parameter, CharSequence> parameterValues = createParameterValues(PROVIDER, testParameters);

    String connectionString = PROVIDER.getConnectionString(parameterValues);
    assertNotNull(connectionString);
  }

}