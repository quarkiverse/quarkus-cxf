package io.quarkiverse.cxf.ws.security;

import static io.quarkiverse.cxf.ws.security.WssConfigurationConstant.Transformer.beanRef;
import static io.quarkiverse.cxf.ws.security.WssConfigurationConstant.Transformer.properties;
import static io.quarkiverse.cxf.ws.security.WssConfigurationConstant.Transformer.toInteger;

import java.util.Map;
import java.util.Optional;

import org.apache.cxf.ws.security.SecurityConstants;

import io.quarkus.runtime.annotations.ConfigDocFilename;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;
import io.smallrye.config.WithParentName;

@ConfigMapping(prefix = "quarkus.cxf")
@ConfigDocFilename("quarkus-cxf-rt-ws-security.adoc")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface CxfWsSecurityConfig {

    /**
     * Client configurations.
     *
     * @asciidoclet
     */
    @WithName("client")
    Map<String, ClientConfig> clients();

    /**
     * Endpoint configurations.
     *
     * @asciidoclet
     */
    @WithName("endpoint")
    Map<String, EndpointConfig> endpoints();

    /**
     * A class that provides configurable options of a CXF client.
     */
    @ConfigGroup
    interface ClientConfig {

        /**
         * WS-Security related client configuration
         *
         * @asciidoclet
         */
        ClientSecurityConfig security();
    }

    /**
     * A class that provides configurable options of a CXF client.
     */
    @ConfigGroup
    interface EndpointConfig {

        /**
         * WS-Security related client configuration
         *
         * @asciidoclet
         */
        ClientOrEndpointSecurityConfig security();
    }

    /**
     * A class that provides configurable options of a CXF client.
     */
    @ConfigGroup
    public interface ClientOrEndpointSecurityConfig {

        // org.apache.cxf.rt.security.SecurityConstants
        /**
         * The user's name. It is used as follows:
         *
         * - As the name in the UsernameToken for WS-Security
         * - As the alias name in the keystore to get the user's cert and private key for signature if `signature.username` is
         * not set
         * - As the alias name in the keystore to get the user's public key for encryption if `encryption.username` is not set
         *
         * @since 2.5.0
         * @asciidoclet
         */
        @WssConfigurationConstant(key = "security.username")
        Optional<String> username();

        /**
         * The user's password when a `callback-handler` is not defined. This is only used for the password in a WS-Security
         * UsernameToken.
         *
         * @since 2.5.0
         * @asciidoclet
         */
        @WssConfigurationConstant(key = "security.password")
        Optional<String> password();

        /**
         * The user's name for signature. It is used as the alias name in the keystore to get the user's cert and private key
         * for signature. If this is not defined, then `username` is used instead. If that is also not specified, it uses the
         * the default alias set in the properties file referenced by `signature.properties`. If that's also not set, and the
         * keystore only contains a single key, that key will be used.
         *
         * @since 2.5.0
         * @asciidoclet
         */
        @WssConfigurationConstant(key = "security.signature.username")
        @WithName("signature.username")
        Optional<String> signatureUsername();

        /**
         * The user's password for signature when a `callback-handler` is not defined.
         *
         * @since 2.5.0
         * @asciidoclet
         */
        @WssConfigurationConstant(key = "security.signature.password")
        @WithName("signature.password")
        Optional<String> signaturePassword();

        /**
         * The user's name for encryption. It is used as the alias name in the keystore to get the user's public key for
         * encryption. If this is not defined, then `username` is used instead. If that is also not specified, it uses the the
         * default alias set in the properties file referenced by `encrypt.properties`. If that's also not set, and the keystore
         * only contains a single key, that key will be used.
         *
         * For the WS-Security web service provider, the `useReqSigCert` value can be used to accept (encrypt to) any client
         * whose public key is in the service's truststore (defined in `encrypt.properties`).
         *
         * @since 2.5.0
         * @asciidoclet
         */
        @WssConfigurationConstant(key = "security.encryption.username")
        @WithName("encryption.username")
        Optional<String> encryptionUsername();

        //
        // Callback class and Crypto properties
        //
        /**
         * A xref:user-guide/configuration.adoc#beanRefs[reference] to a `javax.security.auth.callback.CallbackHandler` bean
         * used to obtain passwords, for both outbound and inbound requests.
         *
         * @since 2.5.0
         * @asciidoclet
         */
        @WssConfigurationConstant(key = "security.callback-handler", transformer = beanRef)
        Optional<String> callbackHandler();

        /**
         * A xref:user-guide/configuration.adoc#beanRefs[reference] to a `javax.security.auth.callback.CallbackHandler`
         * implementation used to construct SAML Assertions.
         *
         * @since 2.5.0
         * @asciidoclet
         */
        @WssConfigurationConstant(key = "security.saml-callback-handler", transformer = beanRef)
        Optional<String> samlCallbackHandler();

        /**
         * The Crypto property configuration to use for signing, if `signature.crypto` is not set.
         *
         * Example
         *
         * [source,properties]
         * ----
         * [prefix].signature.properties."org.apache.ws.security.crypto.provider" =
         * org.apache.ws.security.components.crypto.Merlin
         * [prefix].signature.properties."org.apache.ws.security.crypto.merlin.keystore.password" = password
         * [prefix].signature.properties."org.apache.ws.security.crypto.merlin.file" = certs/alice.jks
         * ----
         *
         * @since 2.5.0
         * @asciidoclet
         */
        @WssConfigurationConstant(key = "security.signature.properties", transformer = properties)
        @WithName("signature.properties")
        Map<String, String> signatureProperties();

        /**
         * The Crypto property configuration to use for encryption, if `encryption.crypto` is not set.
         *
         * Example
         *
         * [source,properties]
         * ----
         * [prefix].encryption.properties."org.apache.ws.security.crypto.provider" =
         * org.apache.ws.security.components.crypto.Merlin
         * [prefix].encryption.properties."org.apache.ws.security.crypto.merlin.keystore.password" = password
         * [prefix].encryption.properties."org.apache.ws.security.crypto.merlin.file" = certs/alice.jks
         * ----
         *
         * @since 2.5.0
         * @asciidoclet
         */
        @WssConfigurationConstant(key = "security.encryption.properties", transformer = properties)
        @WithName("encryption.properties")
        Map<String, String> encryptionProperties();

        /**
         * A xref:user-guide/configuration.adoc#beanRefs[reference] to a `org.apache.wss4j.common.crypto.Crypto` bean to be used
         * for signature. If not set, `signature.properties` will be used to configure a `Crypto` instance.
         *
         * @since 2.5.0
         * @asciidoclet
         */
        @WssConfigurationConstant(key = "security.signature.crypto", transformer = beanRef)
        @WithName("signature.crypto")
        Optional<String> signatureCrypto();

        /**
         * A xref:user-guide/configuration.adoc#beanRefs[reference] to a `org.apache.wss4j.common.crypto.Crypto` to be used for
         * encryption. If not set, `encryption.properties` will be used to configure a `Crypto` instance.
         *
         * @since 2.5.0
         * @asciidoclet
         */
        @WssConfigurationConstant(key = "security.encryption.crypto", transformer = beanRef)
        @WithName("encryption.crypto")
        Optional<String> encryptionCrypto();

        /**
         * A message property for prepared X509 certificate to be used for encryption. If this is not defined, then the
         * certificate will be either loaded from the keystore `encryption.properties` or extracted from request (when
         * WS-Security is used and if `encryption.username` has value `useReqSigCert`.
         *
         * This option is experimental, because it is link:https://github.com/quarkiverse/quarkus-cxf/issues/1052[not covered by
         * tests] yet.
         *
         * @since 2.5.0
         * @asciidoclet
         */
        @WssConfigurationConstant(key = "security.encryption.certificate")
        @WithName("encryption.certificate")
        Optional<String> encryptionCertificate();

        //
        // Boolean Security configuration tags, e.g. the value should be "true" or "false".
        //
        /**
         * If `true`, Certificate Revocation List (CRL) checking is enabled when verifying trust in a certificate; otherwise it
         * is not enabled.
         *
         * This option is experimental, because it is link:https://github.com/quarkiverse/quarkus-cxf/issues/1052[not covered by
         * tests] yet.
         *
         * @since 2.5.0
         * @asciidoclet
         */
        @WssConfigurationConstant(key = "security.enableRevocation")
        @WithDefault("false")
        boolean enableRevocation();

        /**
         * If `true`, unsigned SAML assertions will be allowed as SecurityContext Principals; otherwise they won't be allowed as
         * SecurityContext Principals.
         *
         * [NOTE]
         * .Signature
         * ====
         * The label "unsigned" refers to an internal signature. Even if the token is signed by an external signature (as per
         * the "sender-vouches" requirement), this boolean must still be configured if you want to use the token to set up
         * the security context.
         * ====
         *
         * This option is experimental, because it is link:https://github.com/quarkiverse/quarkus-cxf/issues/1052[not covered by
         * tests] yet.
         *
         * @since 2.5.0
         * @asciidoclet
         */
        @WssConfigurationConstant(key = "security.enable.unsigned-saml-assertion.principal")
        @WithDefault("false")
        boolean enableUnsignedSamlAssertionPrincipal();

        // does not seem to work
        //        /**
        //         * If {@code true}, {@code UsernameToken}s with no password will be used as SecurityContext Principals;
        //         * otherwise they won't be allowed to be used as SecurityContext Principals
        //         */
        //        @WssConfigurationConstant(key = "security.enable.ut-no-password.principal")
        //        @WithDefault("false")
        //        boolean enableUtNopasswordPrincipal();
        /**
         * If `true`, the `SubjectConfirmation` requirements of a received SAML Token (sender-vouches or holder-of-key) will be
         * validated; otherwise they won't be validated.
         *
         * This option is experimental, because it is link:https://github.com/quarkiverse/quarkus-cxf/issues/1052[not covered by
         * tests] yet.
         *
         * @since 2.5.0
         * @asciidoclet
         */
        @WssConfigurationConstant(key = "security.validate.saml.subject.conf")
        @WithDefault("true")
        boolean validateSamlSubjectConfirmation();

        /**
         * If `true`, security context can be created from JAAS Subject; otherwise it must not be created from JAAS Subject.
         *
         * This option is experimental, because it is link:https://github.com/quarkiverse/quarkus-cxf/issues/1052[not covered by
         * tests] yet.
         *
         * @since 2.5.0
         * @asciidoclet
         */
        @WssConfigurationConstant(key = "security.sc.jaas-subject")
        @WithDefault("true")
        boolean scFromJaasSubject();

        /**
         * If `true`, then if the SAML Token contains Audience Restriction URIs, one of them must match one of the values in
         * `audience.restrictions`; otherwise the SAML AudienceRestriction validation is disabled.
         *
         * This option is experimental, because it is link:https://github.com/quarkiverse/quarkus-cxf/issues/1052[not covered by
         * tests] yet.
         *
         * @since 2.5.0
         * @asciidoclet
         */
        @WssConfigurationConstant(key = "security.validate.audience-restriction")
        @WithDefault("true")
        boolean audienceRestrictionValidation();

        //
        // Non-boolean WS-Security Configuration parameters
        //
        /**
         * The attribute URI of the SAML `AttributeStatement` where the role information is stored.
         *
         * This option is experimental, because it is link:https://github.com/quarkiverse/quarkus-cxf/issues/1052[not covered by
         * tests] yet.
         *
         * @since 2.5.0
         * @asciidoclet
         */
        @WssConfigurationConstant(key = "security.saml-role-attributename")
        @WithDefault("http://schemas.xmlsoap.org/ws/2005/05/identity/claims/role")
        String samlRoleAttributename();

        /**
         * A String of regular expressions (separated by the value specified in `security.cert.constraints.separator`) which
         * will be applied to the subject DN of the certificate used for signature validation, after trust verification of the
         * certificate chain associated with the certificate.
         *
         * This option is experimental, because it is link:https://github.com/quarkiverse/quarkus-cxf/issues/1052[not covered by
         * tests] yet.
         *
         * @since 2.5.0
         * @asciidoclet
         */
        @WssConfigurationConstant(key = "security.subject.cert.constraints")
        Optional<String> subjectCertConstraints();

        /**
         * The separator that is used to parse certificate constraints configured in `security.subject.cert.constraints`
         *
         * This option is experimental, because it is link:https://github.com/quarkiverse/quarkus-cxf/issues/1052[not covered by
         * tests] yet.
         *
         * @since 2.5.0
         * @asciidoclet
         */
        @WssConfigurationConstant(key = "security.cert.constraints.separator")
        @WithDefault(",")
        String certConstraintsSeparator();

        // org.apache.cxf.ws.security.SecurityConstants
        // User properties
        //
        /**
         * The actor or role name of the `wsse:Security` header. If this parameter is omitted, the actor name is not set.
         *
         * This option is experimental, because it is link:https://github.com/quarkiverse/quarkus-cxf/issues/1052[not covered by
         * tests] yet.
         *
         * @since 2.5.0
         * @asciidoclet
         */
        @WssConfigurationConstant(key = "ws-security.actor")
        @WithName("actor")
        Optional<String> actor();

        //
        // Boolean WS-Security configuration tags, e.g. the value should be "true" or "false".
        //
        /**
         * If `true`, the password of a received `UsernameToken` will be validated; otherwise it won't be validated.
         *
         * @since 2.5.0
         * @asciidoclet
         */
        @WssConfigurationConstant(key = "ws-security.validate.token")
        @WithName("validate.token")
        @WithDefault("true")
        boolean validateToken();

        // WebLogic and WCF always encrypt UsernameTokens whenever possible
        //See:  http://e-docs.bea.com/wls/docs103/webserv_intro/interop.html
        //Be default, we will encrypt as well for interop reasons.  However, this
        //setting can be set to false to turn that off.
        /**
         * Whether to always encrypt `UsernameTokens` that are defined as a `SupportingToken`. This should not be set to `false`
         * in a production environment, as it exposes the password (or the digest of the password) on the wire.
         *
         * This option is experimental, because it is link:https://github.com/quarkiverse/quarkus-cxf/issues/1052[not covered by
         * tests] yet.
         *
         * @since 2.5.0
         * @asciidoclet
         */
        @WssConfigurationConstant(key = "ws-security.username-token.always.encrypted")
        @WithName("username-token.always.encrypted")
        @WithDefault("true")
        boolean alwaysEncryptUt();

        /**
         * If `true`, the compliance with the Basic Security Profile (BSP) 1.1 will be ensured; otherwise it will not be
         * ensured.
         *
         * This option is experimental, because it is link:https://github.com/quarkiverse/quarkus-cxf/issues/1052[not covered by
         * tests] yet.
         *
         * @since 2.5.0
         * @asciidoclet
         */
        @WssConfigurationConstant(key = "ws-security.is-bsp-compliant")
        @WithName("is-bsp-compliant")
        @WithDefault("true")
        boolean isBspCompliant();

        /**
         * If `true`, the `UsernameToken` nonces will be cached for both message initiators and recipients; otherwise they won't
         * be cached for neither message initiators nor recipients. The default is `true` for message recipients, and `false`
         * for message initiators.
         *
         * [NOTE]
         * .Caching
         * ====
         * Caching only applies when either a `UsernameToken` WS-SecurityPolicy is in effect, or the `UsernameToken` action has
         * been configured for the non-security-policy case.
         * ====
         *
         * @since 2.5.0
         * @asciidoclet
         */
        @WssConfigurationConstant(key = "ws-security.enable.nonce.cache")
        @WithName("enable.nonce.cache")
        Optional<Boolean> enableNonceCache();

        /**
         * If `true`, the `Timestamp` `Created` Strings (these are only cached in conjunction with a message Signature) will be
         * cached for both message initiators and recipients; otherwise they won't be cached for neither message initiators nor
         * recipients. The default is `true` for message recipients, and `false` for message initiators.
         *
         * [NOTE]
         * .Caching
         * ====
         * Caching only applies when either a `IncludeTimestamp` policy is in effect, or the `Timestamp` action has been
         * configured for the non-security-policy case.
         * ====
         *
         * This option is experimental, because it is link:https://github.com/quarkiverse/quarkus-cxf/issues/1052[not covered by
         * tests] yet.
         *
         * @since 2.5.0
         * @asciidoclet
         */
        @WssConfigurationConstant(key = "ws-security.enable.timestamp.cache")
        @WithName("enable.timestamp.cache")
        Optional<Boolean> enableTimestampCache();

        /**
         * If `true`, the new streaming (StAX) implementation of WS-Security is used; otherwise the old DOM implementation is
         * used.
         *
         * @since 2.5.0
         * @asciidoclet
         */
        @WssConfigurationConstant(key = "ws-security.enable.streaming")
        @WithName("enable.streaming")
        @WithDefault("false")
        boolean enableStreaming();

        /**
         * If `true`, detailed security error messages are sent to clients; otherwise the details are omitted and only a generic
         * error message is sent.
         *
         * The "real" security errors should not be returned to the client in production, as they may leak information about the
         * deployment, or otherwise provide an "oracle" for attacks.
         *
         * @since 2.5.0
         * @asciidoclet
         */
        @WssConfigurationConstant(key = "ws-security.return.security.error")
        @WithName("return.security.error")
        @WithDefault("false")
        boolean returnSecurityError();

        /**
         * If `true`, the SOAP `mustUnderstand` header is included in security headers based on a WS-SecurityPolicy; otherwise
         * the header is always omitted.
         *
         * Works only with `enable.streaming = true` - see link:https://issues.apache.org/jira/browse/CXF-8940[CXF-8940]
         *
         * @since 2.5.0
         * @asciidoclet
         */
        @WssConfigurationConstant(key = "ws-security.must-understand")
        @WithName("must-understand")
        @WithDefault("true")
        boolean mustUnderstand();

        /**
         * If `true` and in case the token contains a `OneTimeUse` Condition, the SAML2 Token Identifiers will be cached for
         * both message initiators and recipients; otherwise they won't be cached for neither message initiators nor recipients.
         * The default is `true` for message recipients, and `false` for message initiators.
         *
         * Caching only applies when either a `SamlToken` policy is in effect, or a SAML action has been configured for the
         * non-security-policy case.
         *
         * This option is experimental, because it is link:https://github.com/quarkiverse/quarkus-cxf/issues/1052[not covered by
         * tests] yet.
         *
         * @since 2.5.0
         * @asciidoclet
         */
        @WssConfigurationConstant(key = "ws-security.enable.saml.cache")
        @WithName("enable.saml.cache")
        Optional<Boolean> enableSamlOneTimeUseCache();

        /**
         * Whether to store bytes (CipherData or BinarySecurityToken) in an attachment. The default is true if MTOM is enabled.
         * Set it to false to BASE-64 encode the bytes and "inlined" them in the message instead. Setting this to true is more
         * efficient, as it means that the BASE-64 encoding step can be skipped. This only applies to the DOM WS-Security stack.
         *
         * This option is experimental, because it is link:https://github.com/quarkiverse/quarkus-cxf/issues/1052[not covered by
         * tests] yet.
         *
         * @since 2.5.0
         * @asciidoclet
         */
        @WssConfigurationConstant(key = "ws-security.store.bytes.in.attachment")
        @WithName("store.bytes.in.attachment")
        Optional<Boolean> storeBytesInAttachment();

        /**
         * If `true`, `Attachment-Content-Only` transform will be used when an Attachment is encrypted via a WS-SecurityPolicy
         * expression; otherwise `Attachment-Complete` transform will be used when an Attachment is encrypted via a
         * WS-SecurityPolicy expression.
         *
         * This option is experimental, because it is link:https://github.com/quarkiverse/quarkus-cxf/issues/1052[not covered by
         * tests] yet.
         *
         * @since 2.5.0
         * @asciidoclet
         */
        @WssConfigurationConstant(key = "ws-security.swa.encryption.attachment.transform.content")
        @WithName("swa.encryption.attachment.transform.content")
        @WithDefault("false")
        boolean useAttachmentEncryptionContentOnlyTransform();

        /**
         * If `true`, the STR (Security Token Reference) Transform will be used when (externally) signing a SAML Token;
         * otherwise the STR (Security Token Reference) Transform will not be used.
         *
         * Some frameworks cannot process the `SecurityTokenReference`. You may set this `false` in such cases.
         *
         * This option is experimental, because it is link:https://github.com/quarkiverse/quarkus-cxf/issues/1052[not covered by
         * tests] yet.
         *
         * @since 2.5.0
         * @asciidoclet
         */
        @WssConfigurationConstant(key = "ws-security.use.str.transform")
        @WithName("use.str.transform")
        @WithDefault("true")
        boolean useStrTransform();

        /**
         * If `true`, an `InclusiveNamespaces` `PrefixList` will be added as a `CanonicalizationMethod` child when generating
         * Signatures using `WSConstants.C14N_EXCL_OMIT_COMMENTS`; otherwise the `PrefixList` will not be added.
         *
         * This option is experimental, because it is link:https://github.com/quarkiverse/quarkus-cxf/issues/1052[not covered by
         * tests] yet.
         *
         * @since 2.5.0
         * @asciidoclet
         */
        @WssConfigurationConstant(key = "ws-security.add.inclusive.prefixes")
        @WithName("add.inclusive.prefixes")
        @WithDefault("true")
        boolean addInclusivePrefixes();

        /**
         * If `true`, the enforcement of the WS-SecurityPolicy `RequireClientCertificate` policy will be disabled; otherwise the
         * enforcement of the WS-SecurityPolicy `RequireClientCertificate` policy is enabled.
         *
         * Some servers may not do client certificate verification at the start of the SSL handshake, and therefore the client
         * certificates may not be available to the WS-Security layer for policy verification.
         *
         * This option is experimental, because it is link:https://github.com/quarkiverse/quarkus-cxf/issues/1052[not covered by
         * tests] yet.
         *
         * @since 2.5.0
         * @asciidoclet
         */
        @WssConfigurationConstant(key = "ws-security.disable.require.client.cert.check")
        @WithName("disable.require.client.cert.check")
        @WithDefault("false")
        boolean disableReqClientCertCheck();

        /**
         * If `true`, the `xop:Include` elements will be searched for encryption and signature (on the outbound side) or for
         * signature verification (on the inbound side); otherwise the search won't happen. This ensures that the actual bytes
         * are signed, and not just the reference. The default is `true` if MTOM is enabled, otherwise the default is `false`.
         *
         * This option is experimental, because it is link:https://github.com/quarkiverse/quarkus-cxf/issues/1052[not covered by
         * tests] yet.
         *
         * @since 2.5.0
         * @asciidoclet
         */
        @WssConfigurationConstant(key = "ws-security.expand.xop.include")
        @WithName("expand.xop.include")
        Optional<Boolean> expandXopInclude();

        //
        // Non-boolean WS-Security Configuration parameters
        //
        /**
         * The time in seconds to add to the Creation value of an incoming `Timestamp` to determine whether to accept it as
         * valid or not.
         *
         * This option is experimental, because it is link:https://github.com/quarkiverse/quarkus-cxf/issues/1052[not covered by
         * tests] yet.
         *
         * @since 2.5.0
         * @asciidoclet
         */
        @WssConfigurationConstant(key = "ws-security.timestamp.timeToLive")
        @WithName("timestamp.timeToLive")
        @WithDefault("300")
        Optional<String> timestampTtl();

        /**
         * The time in seconds in the future within which the `Created` time of an incoming `Timestamp` is valid. The default is
         * greater than zero to avoid problems where clocks are slightly askew. Set this to `0` to reject all future-created
         * `Timestamp`s.
         *
         * This option is experimental, because it is link:https://github.com/quarkiverse/quarkus-cxf/issues/1052[not covered by
         * tests] yet.
         *
         * @since 2.5.0
         * @asciidoclet
         */
        @WssConfigurationConstant(key = "ws-security.timestamp.futureTimeToLive")
        @WithName("timestamp.futureTimeToLive")
        @WithDefault("60")
        Optional<String> timestampFutureTtl();

        /**
         * The time in seconds to append to the Creation value of an incoming `UsernameToken` to determine whether to accept it
         * as valid or not.
         *
         * This option is experimental, because it is link:https://github.com/quarkiverse/quarkus-cxf/issues/1052[not covered by
         * tests] yet.
         *
         * @since 2.5.0
         * @asciidoclet
         */
        @WssConfigurationConstant(key = "ws-security.usernametoken.timeToLive")
        @WithName("usernametoken.timeToLive")
        @WithDefault("300")
        Optional<String> usernametokenTtl();

        /**
         * The time in seconds in the future within which the `Created` time of an incoming `UsernameToken` is valid. The
         * default is greater than zero to avoid problems where clocks are slightly askew. Set this to `0` to reject all
         * future-created `UsernameToken`s.
         *
         * This option is experimental, because it is link:https://github.com/quarkiverse/quarkus-cxf/issues/1052[not covered by
         * tests] yet.
         *
         * @since 2.5.0
         * @asciidoclet
         */
        @WssConfigurationConstant(key = "ws-security.usernametoken.futureTimeToLive")
        @WithName("usernametoken.futureTimeToLive")
        @WithDefault("60")
        Optional<String> usernametokenFutureTtl();

        /**
         * A xref:user-guide/configuration.adoc#beanRefs[reference] to a `org.apache.wss4j.common.spnego.SpnegoClientAction`
         * bean to use for SPNEGO. This allows the user to plug in a different implementation to obtain a service ticket.
         *
         * This option is experimental, because it is link:https://github.com/quarkiverse/quarkus-cxf/issues/1052[not covered by
         * tests] yet.
         *
         * @since 2.5.0
         * @asciidoclet
         */
        @WssConfigurationConstant(key = "ws-security.spnego.client.action", transformer = beanRef)
        @WithName("spnego.client.action")
        Optional<String> spnegoClientAction();

        /**
         * A xref:user-guide/configuration.adoc#beanRefs[reference] to a `org.apache.wss4j.common.cache.ReplayCache` bean used
         * to cache `UsernameToken` nonces. A `org.apache.wss4j.common.cache.EHCacheReplayCache` instance is used by default.
         *
         * This option is experimental, because it is link:https://github.com/quarkiverse/quarkus-cxf/issues/1052[not covered by
         * tests] yet.
         *
         * @since 2.5.0
         * @asciidoclet
         */
        @WssConfigurationConstant(key = "ws-security.nonce.cache.instance", transformer = beanRef)
        @WithName("nonce.cache.instance")
        Optional<String> nonceCacheInstance();

        /**
         * A xref:user-guide/configuration.adoc#beanRefs[reference] to a `org.apache.wss4j.common.cache.ReplayCache` bean used
         * to cache `Timestamp` `Created` Strings. A `org.apache.wss4j.common.cache.EHCacheReplayCache` instance is used by
         * default.
         *
         * This option is experimental, because it is link:https://github.com/quarkiverse/quarkus-cxf/issues/1052[not covered by
         * tests] yet.
         *
         * @since 2.5.0
         * @asciidoclet
         */
        @WssConfigurationConstant(key = "ws-security.timestamp.cache.instance", transformer = beanRef)
        @WithName("timestamp.cache.instance")
        Optional<String> timestampCacheInstance();

        /**
         * A xref:user-guide/configuration.adoc#beanRefs[reference] to a `org.apache.wss4j.common.cache.ReplayCache` bean used
         * to cache SAML2 Token Identifier Strings (if the token contains a `OneTimeUse` condition). A
         * `org.apache.wss4j.common.cache.EHCacheReplayCache` instance is used by default.
         *
         * This option is experimental, because it is link:https://github.com/quarkiverse/quarkus-cxf/issues/1052[not covered by
         * tests] yet.
         *
         * @since 2.5.0
         * @asciidoclet
         */
        @WssConfigurationConstant(key = "ws-security.saml.cache.instance", transformer = beanRef)
        @WithName("saml.cache.instance")
        Optional<String> samlOneTimeUseCacheInstance();

        /**
         * Set this property to point to a configuration file for the underlying caching implementation for the `TokenStore`.
         * The default configuration file that is used is `cxf-ehcache.xml` in `org.apache.cxf:cxf-rt-security` JAR.
         *
         * This option is experimental, because it is link:https://github.com/quarkiverse/quarkus-cxf/issues/1052[not covered by
         * tests] yet.
         *
         * @since 2.5.0
         * @asciidoclet
         */
        @WssConfigurationConstant(key = "ws-security.cache.config.file")
        @WithName("cache.config.file")
        Optional<String> cacheConfigFile();

        /**
         * A xref:user-guide/configuration.adoc#beanRefs[reference] to a `org.apache.cxf.ws.security.tokenstore.TokenStore` bean
         * to use for caching security tokens. By default this uses a instance.
         *
         * This option is experimental, because it is link:https://github.com/quarkiverse/quarkus-cxf/issues/1052[not covered by
         * tests] yet.
         *
         * @since 2.5.0
         * @asciidoclet
         */
        @WssConfigurationConstant(key = "org.apache.cxf.ws.security.tokenstore.TokenStore", transformer = beanRef)
        Optional<String> tokenStoreCacheInstance();

        /**
         * The Cache Identifier to use with the TokenStore. CXF uses the following key to retrieve a token store:
         * `org.apache.cxf.ws.security.tokenstore.TokenStore-<identifier>`. This key can be used to configure service-specific
         * cache configuration. If the identifier does not match, then it falls back to a cache configuration with key
         * `org.apache.cxf.ws.security.tokenstore.TokenStore`.
         *
         * The default `<identifier>` is the QName of the service in question. However to pick up a custom cache configuration
         * (for example, if you want to specify a TokenStore per-client proxy), it can be configured with this identifier
         * instead.
         *
         * This option is experimental, because it is link:https://github.com/quarkiverse/quarkus-cxf/issues/1052[not covered by
         * tests] yet.
         *
         * @since 2.5.0
         * @asciidoclet
         */
        @WssConfigurationConstant(key = "ws-security.cache.identifier")
        @WithName("cache.identifier")
        Optional<String> cacheIdentifier();

        /**
         * The Subject Role Classifier to use. If one of the WSS4J Validators returns a JAAS Subject from Validation, then the
         * `WSS4JInInterceptor` will attempt to create a `SecurityContext` based on this Subject. If this value is not
         * specified, then it tries to get roles using the `DefaultSecurityContext` in `org.apache.cxf:cxf-core`. Otherwise it
         * uses this value in combination with the `role.classifier.type` to get the roles from the `Subject`.
         *
         * This option is experimental, because it is link:https://github.com/quarkiverse/quarkus-cxf/issues/1052[not covered by
         * tests] yet.
         *
         * @since 2.5.0
         * @asciidoclet
         */
        @WssConfigurationConstant(key = "ws-security.role.classifier")
        @WithName("role.classifier")
        Optional<String> subjectRoleClassifier();

        /**
         * The Subject Role Classifier Type to use. If one of the WSS4J Validators returns a JAAS Subject from Validation, then
         * the `WSS4JInInterceptor` will attempt to create a `SecurityContext` based on this Subject. Currently accepted values
         * are `prefix` or `classname`. Must be used in conjunction with the `role.classifier`.
         *
         * This option is experimental, because it is link:https://github.com/quarkiverse/quarkus-cxf/issues/1052[not covered by
         * tests] yet.
         *
         * @since 2.5.0
         * @asciidoclet
         */
        @WssConfigurationConstant(key = "ws-security.role.classifier.type")
        @WithName("role.classifier.type")
        @WithDefault("prefix")
        String subjectRoleClassifierType();

        /**
         * This configuration tag allows the user to override the default Asymmetric Signature algorithm (RSA-SHA1) for use in
         * WS-SecurityPolicy, as the WS-SecurityPolicy specification does not allow the use of other algorithms at present.
         *
         * This option is experimental, because it is link:https://github.com/quarkiverse/quarkus-cxf/issues/1052[not covered by
         * tests] yet.
         *
         * @since 2.5.0
         * @asciidoclet
         */
        @WssConfigurationConstant(key = "ws-security.asymmetric.signature.algorithm")
        @WithName("asymmetric.signature.algorithm")
        Optional<String> asymmetricSignatureAlgorithm();

        /**
         * This configuration tag allows the user to override the default Symmetric Signature algorithm (HMAC-SHA1) for use in
         * WS-SecurityPolicy, as the WS-SecurityPolicy specification does not allow the use of other algorithms at present.
         *
         * This option is experimental, because it is link:https://github.com/quarkiverse/quarkus-cxf/issues/1052[not covered by
         * tests] yet.
         *
         * @since 2.5.0
         * @asciidoclet
         */
        @WssConfigurationConstant(key = "ws-security.symmetric.signature.algorithm")
        @WithName("symmetric.signature.algorithm")
        Optional<String> symmetricSignatureAlgorithm();

        /**
         * A xref:user-guide/configuration.adoc#beanRefs[reference] to a `org.apache.wss4j.common.crypto.PasswordEncryptor`
         * bean, which is used to encrypt or decrypt passwords in the Merlin Crypto implementation (or any custom Crypto
         * implementations).
         *
         * By default, WSS4J uses the `org.apache.wss4j.common.crypto.JasyptPasswordEncryptor` which must be instantiated with a
         * password to use to decrypt keystore passwords in the Merlin Crypto definition. This password is obtained via the
         * CallbackHandler defined via `callback-handler`
         *
         * The encrypted passwords must be stored in the format "ENC(encoded encrypted password)".
         *
         * This option is experimental, because it is link:https://github.com/quarkiverse/quarkus-cxf/issues/1052[not covered by
         * tests] yet.
         *
         * @since 2.5.0
         * @asciidoclet
         */
        @WssConfigurationConstant(key = "ws-security.password.encryptor.instance", transformer = beanRef)
        @WithName("password.encryptor.instance")
        Optional<String> passwordEncryptorInstance();

        /**
         * A xref:user-guide/configuration.adoc#beanRefs[reference] to a Kerberos `org.ietf.jgss.GSSCredential` bean to use for
         * WS-Security. This is used to retrieve a service ticket instead of using the client credentials.
         *
         * This option is experimental, because it is link:https://github.com/quarkiverse/quarkus-cxf/issues/1052[not covered by
         * tests] yet.
         *
         * @since 2.5.0
         * @asciidoclet
         */
        @WssConfigurationConstant(key = "ws-security.delegated.credential", transformer = beanRef)
        @WithName("delegated.credential")
        Optional<String> delegatedCredential();

        /**
         * A xref:user-guide/configuration.adoc#beanRefs[reference] to a
         * `org.apache.cxf.ws.security.wss4j.WSS4JSecurityContextCreator` bean that is used to create a CXF SecurityContext from
         * the set of WSS4J processing results. The default implementation is
         * `org.apache.cxf.ws.security.wss4j.DefaultWSS4JSecurityContextCreator`.
         *
         * This option is experimental, because it is link:https://github.com/quarkiverse/quarkus-cxf/issues/1052[not covered by
         * tests] yet.
         *
         * @since 2.5.0
         * @asciidoclet
         */
        @WssConfigurationConstant(key = "ws-security.security.context.creator", transformer = beanRef)
        @WithName("security.context.creator")
        Optional<String> securityContextCreator();

        /**
         * The security token lifetime value (in milliseconds).
         *
         * This option is experimental, because it is link:https://github.com/quarkiverse/quarkus-cxf/issues/1052[not covered by
         * tests] yet.
         *
         * @since 2.5.0
         * @asciidoclet
         */
        @WssConfigurationConstant(key = "ws-security.security.token.lifetime")
        @WithName("security.token.lifetime")
        @WithDefault("300000")
        long securityTokenLifetime();

        //
        // Kerberos Configuration tags
        //
        /**
         * If `true`, credential delegation is requested in the KerberosClient; otherwise the credential delegation is not in
         * the KerberosClient.
         *
         * This option is experimental, because it is link:https://github.com/quarkiverse/quarkus-cxf/issues/1052[not covered by
         * tests] yet.
         *
         * @since 2.5.0
         * @asciidoclet
         */
        @WssConfigurationConstant(key = "ws-security.kerberos.request.credential.delegation")
        @WithName("kerberos.request.credential.delegation")
        @WithDefault("false")
        boolean kerberosRequestCredentialDelegation();

        /**
         * If `true`, GSSCredential bean is retrieved from the Message Context using the `delegated.credential` property and
         * then it is used to obtain a service ticket.
         *
         * This option is experimental, because it is link:https://github.com/quarkiverse/quarkus-cxf/issues/1052[not covered by
         * tests] yet.
         *
         * @since 2.5.0
         * @asciidoclet
         */
        @WssConfigurationConstant(key = "ws-security.kerberos.use.credential.delegation")
        @WithName("kerberos.use.credential.delegation")
        @WithDefault("false")
        boolean kerberosUseCredentialDelegation();

        /**
         * If `true`, the Kerberos username is in servicename form; otherwise the Kerberos username is not in servicename form.
         *
         * This option is experimental, because it is link:https://github.com/quarkiverse/quarkus-cxf/issues/1052[not covered by
         * tests] yet.
         *
         * @since 2.5.0
         * @asciidoclet
         */
        @WssConfigurationConstant(key = "ws-security.kerberos.is.username.in.servicename.form")
        @WithName("kerberos.is.username.in.servicename.form")
        @WithDefault("false")
        boolean kerberosIsUsernameInServicenameForm();

        /**
         * The JAAS Context name to use for Kerberos.
         *
         * This option is experimental, because it is link:https://github.com/quarkiverse/quarkus-cxf/issues/1052[not covered by
         * tests] yet.
         *
         * @since 2.5.0
         * @asciidoclet
         */
        @WssConfigurationConstant(key = "ws-security.kerberos.jaas.context")
        @WithName("kerberos.jaas.context")
        Optional<String> kerberosJaasContextName();

        /**
         * The Kerberos Service Provider Name (spn) to use.
         *
         * This option is experimental, because it is link:https://github.com/quarkiverse/quarkus-cxf/issues/1052[not covered by
         * tests] yet.
         *
         * @since 2.5.0
         * @asciidoclet
         */
        @WssConfigurationConstant(key = "ws-security.kerberos.spn")
        @WithName("kerberos.spn")
        Optional<String> kerberosSpn();

        /**
         * A xref:user-guide/configuration.adoc#beanRefs[reference] to a `org.apache.cxf.ws.security.kerberos.KerberosClient`
         * bean used to obtain a service ticket.
         *
         * This option is experimental, because it is link:https://github.com/quarkiverse/quarkus-cxf/issues/1052[not covered by
         * tests] yet.
         *
         * @since 2.5.0
         * @asciidoclet
         */
        @WssConfigurationConstant(key = "ws-security.kerberos.client")
        @WithName("kerberos.client")
        Optional<String> kerberosClient();

        //
        // Custom Algorithm Suite
        //

        /**
         * If algorithm suite with the identifier <i>CustomizedAlgorithmSuite</i> is used, it can be fully customized.
         * Suggested usage is for scenarios for the non-standard security requirements (like FIPS).
         *
         * <p>
         * Default values are derived from the algorithm suite <i>Basic256Sha256Rsa15</i> and are FIPS compliant.
         * <p>
         * </p>
         * For more information about algorithms, see
         * <a:href="http://docs.oasis-open.org/ws-sx/ws-securitypolicy/v1.2/errata01/os/ws-securitypolicy-1.2-errata01-os-complete.html#_Toc325572744">WS-SecurityPolicy
         * 1.2</a:href="http://docs.oasis-open.org/ws-sx/ws-securitypolicy/v1.2/errata01/os/ws-securitypolicy-1.2-errata01-os-complete.html#_Toc325572744">
         * and <a:href="https://www.w3.org/TR/xmlenc-core1/#sec-Algorithms">security
         * algorithms</a></a:href="https://www.w3.org/TR/xmlenc-core1/#sec-Algorithms">
         * </p>
         *
         * <p>
         * Default values:
         * </p>
         * <ul>
         * <li>Asymmetric Signature: http://www.w3.org/2001/04/xmldsig-more#rsa-sha256</li>
         * <li>Symmetric Signature: http://www.w3.org/2000/09/xmldsig#hmac-sha1</li>
         * <li>Digest Algorithm: http://www.w3.org/2001/04/xmlenc#sha256</li>
         * <li>Encryption Algorithm: http://www.w3.org/2009/xmlenc11#aes256-gcm (differs from <i>Basic256Sha256Rsa15</i>)</li>
         * <li>Symmetric Key Encryption Algorithm: http://www.w3.org/2001/04/xmlenc#kw-aes256</li>
         * <li>Asymmetric Key Encryption Algorithm: http://www.w3.org/2001/04/xmlenc#rsa-1_5</li>
         * <li>Encryption Key Derivation: http://schemas.xmlsoap.org/ws/2005/02/sc/dk/p_sha1</li>
         * <li>Signature Key Derivation: http://schemas.xmlsoap.org/ws/2005/02/sc/dk/p_sha1</li>
         * <li>Encryption Derived Key Length: 256</li>
         * <li>Signature Derived Key Length: 192</li>
         * <li>Minimum Symmetric Key Length: 256</li>
         * <li>Maximum Symmetric Key Length: 1024</li>
         * <li>Minimum Asymmetric Key Length: 256</li>
         * <li>Maximum Asymmetric Key Length: 4096</li>
         * </ul>
         * </p>
         */
        public static final String CUSTOM_ALGORITHM_SUITE_NAME = "CustomAlgorithmSuite";

        /**
         * Digest Algorithm.
         * <p>
         * For more information about algorithms, see
         * <a:href="http://docs.oasis-open.org/ws-sx/ws-securitypolicy/v1.2/errata01/os/ws-securitypolicy-1.2-errata01-os-complete.html#_Toc325572744">WS-SecurityPolicy
         * 1.2</a:href="http://docs.oasis-open.org/ws-sx/ws-securitypolicy/v1.2/errata01/os/ws-securitypolicy-1.2-errata01-os-complete.html#_Toc325572744">
         * and <a:href="https://www.w3.org/TR/xmlenc-core1/#sec-Algorithms">security
         * algorithms</a></a:href="https://www.w3.org/TR/xmlenc-core1/#sec-Algorithms">
         * </p>
         */
        @WssConfigurationConstant(key = SecurityConstants.CUSTOM_ALG_SUITE_DIGEST_ALGORITHM)
        @WithName("custom.digest.algorithm")
        @WithDefault("http://www.w3.org/2001/04/xmlenc#sha256")
        public String digestAlgorithm();

        /**
         * Encryption Algorithm.
         * <p>
         * For more information about algorithms, see
         * <a:href="http://docs.oasis-open.org/ws-sx/ws-securitypolicy/v1.2/errata01/os/ws-securitypolicy-1.2-errata01-os-complete.html#_Toc325572744">WS-SecurityPolicy
         * 1.2</a:href="http://docs.oasis-open.org/ws-sx/ws-securitypolicy/v1.2/errata01/os/ws-securitypolicy-1.2-errata01-os-complete.html#_Toc325572744">
         * and <a:href="https://www.w3.org/TR/xmlenc-core1/#sec-Algorithms">security
         * algorithms</a></a:href="https://www.w3.org/TR/xmlenc-core1/#sec-Algorithms">
         * </p>
         */
        @WssConfigurationConstant(key = SecurityConstants.CUSTOM_ALG_SUITE_ENCRYPTION_ALGORITHM)
        @WithName("custom.encryption.algorithm")
        @WithDefault("http://www.w3.org/2009/xmlenc11#aes256-gcm")
        public String encryptionAlgorithm();

        /**
         * Symmetric Key Encryption Algorithm.
         * <p>
         * For more information about algorithms, see
         * <a:href="http://docs.oasis-open.org/ws-sx/ws-securitypolicy/v1.2/errata01/os/ws-securitypolicy-1.2-errata01-os-complete.html#_Toc325572744">WS-SecurityPolicy
         * 1.2</a:href="http://docs.oasis-open.org/ws-sx/ws-securitypolicy/v1.2/errata01/os/ws-securitypolicy-1.2-errata01-os-complete.html#_Toc325572744">
         * and <a:href="https://www.w3.org/TR/xmlenc-core1/#sec-Algorithms">security
         * algorithms</a></a:href="https://www.w3.org/TR/xmlenc-core1/#sec-Algorithms">
         * </p>
         */
        @WssConfigurationConstant(key = SecurityConstants.CUSTOM_ALG_SUITE_SYMMETRIC_KEY_ENCRYPTION_ALGORITHM)
        @WithName("custom.symmetric.key.encryption.algorithm")
        @WithDefault("http://www.w3.org/2001/04/xmlenc#kw-aes256")
        public String symmetricKeyEncryptionAlgorithm();

        /**
         * Asymmetric Key Encryption Algorithm.
         * <p>
         * For more information about algorithms, see
         * <a:href="http://docs.oasis-open.org/ws-sx/ws-securitypolicy/v1.2/errata01/os/ws-securitypolicy-1.2-errata01-os-complete.html#_Toc325572744">WS-SecurityPolicy
         * 1.2</a:href="http://docs.oasis-open.org/ws-sx/ws-securitypolicy/v1.2/errata01/os/ws-securitypolicy-1.2-errata01-os-complete.html#_Toc325572744">
         * and <a:href="https://www.w3.org/TR/xmlenc-core1/#sec-Algorithms">security
         * algorithms</a></a:href="https://www.w3.org/TR/xmlenc-core1/#sec-Algorithms">
         * </p>
         */
        @WssConfigurationConstant(key = SecurityConstants.CUSTOM_ALG_SUITE_ASYMMETRIC_KEY_ENCRYPTION_ALGORITHM)
        @WithName("custom.asymmetric.key.encryption.algorithm")
        @WithDefault("http://www.w3.org/2001/04/xmlenc#rsa-1_5")
        public String asymmetricKeyEncryptionAlgorithm();

        /**
         * Encryption Key Derivation. For more information about algorithms, see WS-SecurityPolicy 1.2 and security algorithms
         * <p>
         * For more information about algorithms, see
         * <a:href="http://docs.oasis-open.org/ws-sx/ws-securitypolicy/v1.2/errata01/os/ws-securitypolicy-1.2-errata01-os-complete.html#_Toc325572744">WS-SecurityPolicy
         * 1.2</a:href="http://docs.oasis-open.org/ws-sx/ws-securitypolicy/v1.2/errata01/os/ws-securitypolicy-1.2-errata01-os-complete.html#_Toc325572744">
         * and <a:href="https://www.w3.org/TR/xmlenc-core1/#sec-Algorithms">security
         * algorithms</a></a:href="https://www.w3.org/TR/xmlenc-core1/#sec-Algorithms">
         * </p>
         */
        @WssConfigurationConstant(key = SecurityConstants.CUSTOM_ALG_SUITE_ENCRYPTION_KEY_DERIVATION)
        @WithName("custom.encryption.key.derivation")
        @WithDefault("http://schemas.xmlsoap.org/ws/2005/02/sc/dk/p_sha1")
        public String encryptionKeyDerivation();

        /**
         * Signature Key Derivation.
         * <p>
         * For more information about algorithms, see
         * <a:href="http://docs.oasis-open.org/ws-sx/ws-securitypolicy/v1.2/errata01/os/ws-securitypolicy-1.2-errata01-os-complete.html#_Toc325572744">WS-SecurityPolicy
         * 1.2</a:href="http://docs.oasis-open.org/ws-sx/ws-securitypolicy/v1.2/errata01/os/ws-securitypolicy-1.2-errata01-os-complete.html#_Toc325572744">
         * and <a:href="https://www.w3.org/TR/xmlenc-core1/#sec-Algorithms">security
         * algorithms</a></a:href="https://www.w3.org/TR/xmlenc-core1/#sec-Algorithms">
         * </p>
         */
        @WssConfigurationConstant(key = SecurityConstants.CUSTOM_ALG_SUITE_SIGNATURE_KEY_DERIVATION)
        @WithName("custom.signature.key.derivation")
        @WithDefault("http://schemas.xmlsoap.org/ws/2005/02/sc/dk/p_sha1")
        public String signatureKeyDerivation();

        /**
         * Encryption Derived Key Length (number of bits).
         * <p>
         * For more information about algorithms, see
         * <a:href="http://docs.oasis-open.org/ws-sx/ws-securitypolicy/v1.2/errata01/os/ws-securitypolicy-1.2-errata01-os-complete.html#_Toc325572744">WS-SecurityPolicy
         * 1.2</a:href="http://docs.oasis-open.org/ws-sx/ws-securitypolicy/v1.2/errata01/os/ws-securitypolicy-1.2-errata01-os-complete.html#_Toc325572744">
         * and <a:href="https://www.w3.org/TR/xmlenc-core1/#sec-Algorithms">security
         * algorithms</a></a:href="https://www.w3.org/TR/xmlenc-core1/#sec-Algorithms">
         * </p>
         */
        @WssConfigurationConstant(key = SecurityConstants.CUSTOM_ALG_SUITE_ENCRYPTION_DERIVED_KEY_LENGTH, transformer = toInteger)
        @WithName("custom.encryption.derived.key.length")
        @WithDefault("256")
        public Integer encryptionDerivedKeyLength();

        /**
         * Signature Derived Key Length (number of bits).
         * <p>
         * For more information about algorithms, see
         * <a:href="http://docs.oasis-open.org/ws-sx/ws-securitypolicy/v1.2/errata01/os/ws-securitypolicy-1.2-errata01-os-complete.html#_Toc325572744">WS-SecurityPolicy
         * 1.2</a:href="http://docs.oasis-open.org/ws-sx/ws-securitypolicy/v1.2/errata01/os/ws-securitypolicy-1.2-errata01-os-complete.html#_Toc325572744">
         * and <a:href="https://www.w3.org/TR/xmlenc-core1/#sec-Algorithms">security
         * algorithms</a></a:href="https://www.w3.org/TR/xmlenc-core1/#sec-Algorithms">
         * </p>
         */
        @WssConfigurationConstant(key = SecurityConstants.CUSTOM_ALG_SUITE_SIGNATURE_DERIVED_KEY_LENGTH, transformer = toInteger)
        @WithName("custom.signature.derived.key.length")
        @WithDefault("192")
        public Integer signatureDerivedKeyLength();

        /**
         * Minimum Symmetric Key Length (number of bits).
         * <p>
         * For more information about algorithms, see
         * <a:href="http://docs.oasis-open.org/ws-sx/ws-securitypolicy/v1.2/errata01/os/ws-securitypolicy-1.2-errata01-os-complete.html#_Toc325572744">WS-SecurityPolicy
         * 1.2</a:href="http://docs.oasis-open.org/ws-sx/ws-securitypolicy/v1.2/errata01/os/ws-securitypolicy-1.2-errata01-os-complete.html#_Toc325572744">
         * and <a:href="https://www.w3.org/TR/xmlenc-core1/#sec-Algorithms">security
         * algorithms</a></a:href="https://www.w3.org/TR/xmlenc-core1/#sec-Algorithms">
         * </p>
         */
        @WssConfigurationConstant(key = SecurityConstants.CUSTOM_ALG_SUITE_MINIMUM_SYMMETRIC_KEY_LENGTH, transformer = toInteger)
        @WithName("custom.minimum.symmetric.key.length")
        @WithDefault("256")
        public Integer minimumSymmetricKeyLength();

        /**
         * Maximum Symmetric Key Length (number of bits).
         * <p>
         * For more information about algorithms, see
         * <a:href="http://docs.oasis-open.org/ws-sx/ws-securitypolicy/v1.2/errata01/os/ws-securitypolicy-1.2-errata01-os-complete.html#_Toc325572744">WS-SecurityPolicy
         * 1.2</a:href="http://docs.oasis-open.org/ws-sx/ws-securitypolicy/v1.2/errata01/os/ws-securitypolicy-1.2-errata01-os-complete.html#_Toc325572744">
         * and <a:href="https://www.w3.org/TR/xmlenc-core1/#sec-Algorithms">security
         * algorithms</a></a:href="https://www.w3.org/TR/xmlenc-core1/#sec-Algorithms">
         * </p>
         */
        @WssConfigurationConstant(key = SecurityConstants.CUSTOM_ALG_SUITE_MAXIMUM_SYMMETRIC_KEY_LENGTH, transformer = toInteger)
        @WithName("custom.maximum.symmetric.key.length")
        @WithDefault("256")
        public Integer maximumSymmetricKeyLength();

        /**
         * Minimum Symmetric Key Length (number of bits).
         * <p>
         * For more information about algorithms, see
         * <a:href="http://docs.oasis-open.org/ws-sx/ws-securitypolicy/v1.2/errata01/os/ws-securitypolicy-1.2-errata01-os-complete.html#_Toc325572744">WS-SecurityPolicy
         * 1.2</a:href="http://docs.oasis-open.org/ws-sx/ws-securitypolicy/v1.2/errata01/os/ws-securitypolicy-1.2-errata01-os-complete.html#_Toc325572744">
         * and <a:href="https://www.w3.org/TR/xmlenc-core1/#sec-Algorithms">security
         * algorithms</a></a:href="https://www.w3.org/TR/xmlenc-core1/#sec-Algorithms">
         * </p>
         */
        @WssConfigurationConstant(key = SecurityConstants.CUSTOM_ALG_SUITE_MINIMUM_ASYMMETRIC_KEY_LENGTH, transformer = toInteger)
        @WithName("custom.minimum.asymmetric.key.length")
        @WithDefault("1024")
        public Integer minimumAsymmetricKeyLength();

        /**
         * Maximum Symmetric Key Length (number of bits).
         * <p>
         * For more information about algorithms, see
         * <a:href="http://docs.oasis-open.org/ws-sx/ws-securitypolicy/v1.2/errata01/os/ws-securitypolicy-1.2-errata01-os-complete.html#_Toc325572744">WS-SecurityPolicy
         * 1.2</a:href="http://docs.oasis-open.org/ws-sx/ws-securitypolicy/v1.2/errata01/os/ws-securitypolicy-1.2-errata01-os-complete.html#_Toc325572744">
         * and <a:href="https://www.w3.org/TR/xmlenc-core1/#sec-Algorithms">security
         * algorithms</a></a:href="https://www.w3.org/TR/xmlenc-core1/#sec-Algorithms">
         * </p>
         */
        @WssConfigurationConstant(key = SecurityConstants.CUSTOM_ALG_SUITE_MAXIMUM_ASYMMETRIC_KEY_LENGTH, transformer = toInteger)
        @WithName("custom.maximum.asymmetric.key.length")
        @WithDefault("4096")
        public Integer maximumAsymmetricKeyLength();
    }

    /**
     * A class that provides configurable options of a CXF client.
     */
    @ConfigGroup
    public interface ClientSecurityConfig extends ClientOrEndpointSecurityConfig {

        /**
         * STS configuration.
         *
         * @since 3.8.0
         * @asciidoclet
         */
        @WithName("sts.client")
        StsClientConfig sts();
    }

    /**
     * Ready for future use
     */
    interface ValidatorConfig {

        //
        // Validator implementations for validating received security tokens
        //
        /**
         * A xref:user-guide/configuration.adoc#beanRefs[reference] to a `org.apache.wss4j.stax.validate.Validator` bean to use
         * to validate UsernameTokens. The Default is the UsernameTokenValidator.
         *
         * @since 2.5.0
         * @asciidoclet
         */
        @WssConfigurationConstant(key = "ws-security.ut.validator", transformer = beanRef)
        @WithName("ut.validator")
        Optional<String> usernameTokenValidator();

        /**
         * A xref:user-guide/configuration.adoc#beanRefs[reference] to a `org.apache.wss4j.stax.validate.Validator` bean to use
         * to validate SAML 1.1 Tokens. The default value is the SamlAssertionValidator.
         *
         * @since 2.5.0
         * @asciidoclet
         */
        @WssConfigurationConstant(key = "ws-security.saml1.validator", transformer = beanRef)
        @WithName("saml1.validator")
        Optional<String> saml1TokenValidator();

        /**
         * A xref:user-guide/configuration.adoc#beanRefs[reference] to a `org.apache.wss4j.stax.validate.Validator` bean to use
         * to validate SAML 2.0 Tokens. The default value is the SamlAssertionValidator.
         *
         * @since 2.5.0
         * @asciidoclet
         */
        @WssConfigurationConstant(key = "ws-security.saml2.validator", transformer = beanRef)
        @WithName("saml2.validator")
        Optional<String> saml2TokenValidator();

        /**
         * A xref:user-guide/configuration.adoc#beanRefs[reference] to a `org.apache.wss4j.stax.validate.Validator` bean to use
         * to validate Timestamps. The default value is the TimestampValidator.
         *
         * @since 2.5.0
         * @asciidoclet
         */
        @WssConfigurationConstant(key = "ws-security.timestamp.validator", transformer = beanRef)
        @WithName("timestamp.validator")
        Optional<String> timestampTokenValidator();

        /**
         * A xref:user-guide/configuration.adoc#beanRefs[reference] to a `org.apache.wss4j.stax.validate.Validator` bean to use
         * to validate trust in credentials used in Signature verification. The default value is the SignatureTrustValidator.
         *
         * @since 2.5.0
         * @asciidoclet
         */
        @WssConfigurationConstant(key = "ws-security.signature.validator", transformer = beanRef)
        @WithName("signature.validator")
        Optional<String> signatureTokenValidator();

        /**
         * A xref:user-guide/configuration.adoc#beanRefs[reference] to a `org.apache.wss4j.stax.validate.Validator` bean to use
         * to validate BinarySecurityTokens. The default value is the NoOpValidator.
         *
         * @since 2.5.0
         * @asciidoclet
         */
        @WssConfigurationConstant(key = "ws-security.bst.validator", transformer = beanRef)
        @WithName("bst.validator")
        Optional<String> bstTokenValidator();

        /**
         * A xref:user-guide/configuration.adoc#beanRefs[reference] to a `org.apache.wss4j.stax.validate.Validator` bean to use
         * to validate SecurityContextTokens. The default value is the NoOpValidator.
         *
         * @since 2.5.0
         * @asciidoclet
         */
        @WssConfigurationConstant(key = "ws-security.sct.validator", transformer = beanRef)
        @WithName("sct.validator")
        Optional<String> sctTokenValidator();

        /**
         * This refers to a Map of QName, SecurityPolicyValidator, which retrieves a SecurityPolicyValidator implementation to
         * validate a particular security policy, based on the QName of the policy. Any SecurityPolicyValidator implementation
         * defined in this map will override the default value used internally for the corresponding QName.
         *
         * @since 2.5.0
         * @asciidoclet
         */
        @WssConfigurationConstant(key = "ws-security.policy.validator.map", transformer = beanRef)
        @WithName("policy.validator.map")
        Optional<String> policyValidatorMap();
    }

    interface StsClientConfig {

        /**
         * A xref:user-guide/configuration.adoc#beanRefs[reference] to a fully configured
         * `org.apache.cxf.ws.security.trust.STSClient` bean to communicate with the STS. If not set, the STS client will be
         * created and configured based on other `++[++prefix++]++.security.sts.client.++*++` properties as long as they are
         * available.
         *
         * To work around the fact that `org.apache.cxf.ws.security.trust.STSClient` does not have a no-args constructor and
         * cannot thus be used as a CDI bean type, you can use the wrapper class
         * `io.quarkiverse.cxf.ws.security.sts.client.STSClientBean` instead.
         *
         * Tip: Check the xref:reference/extensions/quarkus-cxf-services-sts.adoc[Security Token Service (STS)] extension page
         * for more information about WS-Trust.
         *
         * @since 3.8.0
         * @asciidoclet
         */
        @WithParentName
        @WssConfigurationConstant(key = "security.sts.client", transformer = beanRef)
        Optional<String> client();

        /**
         * A URL, resource path or local filesystem path pointing to a WSDL document to use when generating the service proxy of
         * the STS client.
         *
         * @since 3.8.0
         * @asciidoclet
         */
        Optional<String> wsdl();

        /**
         * A fully qualified name of the STS service. Common values include:
         *
         * - WS-Trust 1.0: `++{++http://schemas.xmlsoap.org/ws/2005/02/trust/++}++SecurityTokenService`
         * - WS-Trust 1.3: `++{++http://docs.oasis-open.org/ws-sx/ws-trust/200512/++}++SecurityTokenService`
         * - WS-Trust 1.4: `++{++http://docs.oasis-open.org/ws-sx/ws-trust/200802/++}++SecurityTokenService`
         *
         * @since 3.8.0
         * @asciidoclet
         */
        Optional<String> serviceName();

        /**
         * A fully qualified name of the STS endpoint name. Common values include:
         *
         * - `++{++http://docs.oasis-open.org/ws-sx/ws-trust/200512/++}++X509_Port`
         * - `++{++http://docs.oasis-open.org/ws-sx/ws-trust/200512/++}++Transport_Port`
         * - `++{++http://docs.oasis-open.org/ws-sx/ws-trust/200512/++}++UT_Port`
         *
         * @since 3.8.0
         * @asciidoclet
         */
        Optional<String> endpointName();

        /**
         * The user name to use when authenticating against the STS. It is used as follows:
         *
         * - As the name in the UsernameToken for WS-Security
         * - As the alias name in the keystore to get the user's cert and private key for signature if `signature.username` is
         * not set
         * - As the alias name in the keystore to get the user's public key for encryption if `encryption.username` is not set
         *
         * @since 3.8.0
         * @asciidoclet
         */
        @WssConfigurationConstant(key = "security.username")
        Optional<String> username();

        /**
         * The password associated with the `username`.
         *
         * @since 3.8.0
         * @asciidoclet
         */
        @WssConfigurationConstant(key = "security.password")
        Optional<String> password();

        /**
         * The user's name for encryption. It is used as the alias name in the keystore to get the user's public key for
         * encryption. If this is not defined, then `username` is used instead. If that is also not specified, it uses the the
         * default alias set in the properties file referenced by `encrypt.properties`. If that's also not set, and the keystore
         * only contains a single key, that key will be used.
         *
         * For the WS-Security web service provider, the `useReqSigCert` value can be used to accept (encrypt to) any client
         * whose public key is in the service's truststore (defined in `encrypt.properties`).
         *
         * @since 3.8.0
         * @asciidoclet
         */
        @WssConfigurationConstant(key = "security.encryption.username")
        @WithName("encryption.username")
        Optional<String> encryptionUsername();

        /**
         * The Crypto property configuration to use for encryption, if `encryption.crypto` is not set.
         *
         * Example
         *
         * [source,properties]
         * ----
         * [prefix].encryption.properties."org.apache.ws.security.crypto.provider" =
         * org.apache.ws.security.components.crypto.Merlin
         * [prefix].encryption.properties."org.apache.ws.security.crypto.merlin.keystore.password" = password
         * [prefix].encryption.properties."org.apache.ws.security.crypto.merlin.file" = certs/alice.jks
         * ----
         *
         * @since 3.8.0
         * @asciidoclet
         */
        @WssConfigurationConstant(key = "security.encryption.properties", transformer = properties)
        @WithName("encryption.properties")
        Map<String, String> encryptionProperties();

        /**
         * A xref:user-guide/configuration.adoc#beanRefs[reference] to a `org.apache.wss4j.common.crypto.Crypto` to be used for
         * encryption. If not set, `encryption.properties` will be used to configure a `Crypto` instance.
         *
         * @since 3.8.0
         * @asciidoclet
         */
        @WssConfigurationConstant(key = "security.encryption.crypto", transformer = beanRef)
        @WithName("encryption.crypto")
        Optional<String> encryptionCrypto();

        /**
         * A xref:user-guide/configuration.adoc#beanRefs[reference] to a `org.apache.wss4j.common.crypto.Crypto` to be used for
         * the STS. If not set, `token.properties` will be used to configure a `Crypto` instance.
         *
         * WCF's trust server sometimes will encrypt the token in the response IN ADDITION TO the full security on the message.
         * These properties control the way the STS client will decrypt the EncryptedData elements in the response.
         *
         * These are also used by the `token.properties` to send/process any RSA/DSAKeyValue tokens used if the KeyType is
         * `PublicKey`
         *
         * @since 3.8.0
         * @asciidoclet
         */
        @WssConfigurationConstant(key = "security.sts.token.crypto", transformer = beanRef)
        @WithName("token.crypto")
        Optional<String> tokenCrypto();

        /**
         * The Crypto property configuration to use for encryption, if `encryption.crypto` is not set.
         *
         * Example
         *
         * [source,properties]
         * ----
         * [prefix].token.properties."org.apache.ws.security.crypto.provider" = org.apache.ws.security.components.crypto.Merlin
         * [prefix].token.properties."org.apache.ws.security.crypto.merlin.keystore.password" = password
         * [prefix].token.properties."org.apache.ws.security.crypto.merlin.file" = certs/alice.jks
         * ----
         *
         * @since 3.8.0
         * @asciidoclet
         */
        @WssConfigurationConstant(key = "security.sts.token.properties", transformer = properties)
        @WithName("token.properties")
        Map<String, String> tokenProperties();

        /**
         * The alias name in the keystore to get the user's public key to send to the STS for the PublicKey KeyType case.
         *
         * @since 3.8.0
         * @asciidoclet
         */
        @WssConfigurationConstant(key = "security.sts.token.username")
        @WithName("token.username")
        Optional<String> tokenUsername();

        /**
         * Whether to write out an X509Certificate structure in UseKey/KeyInfo, or whether to write out a KeyValue structure.
         *
         * @since 3.8.0
         * @asciidoclet
         */
        @WssConfigurationConstant(key = "security.sts.token.usecert")
        @WithName("token.usecert")
        @WithDefault("false")
        boolean tokenUsecert();

        /**
         * If `true` the STS client will be set to send Soap 1.2 messages; otherwise it will send SOAP 1.1 messages.
         *
         * @since 3.8.0
         * @asciidoclet
         */
        @WssConfigurationConstant(key = "security.sts.client-soap12-binding")
        @WithName("soap12-binding")
        @WithDefault("false")
        boolean soap12Binding();
    }

    /**
     * Ready for future use
     */
    interface FutureStsConfig {

        //
        // STS Client Configuration tags
        //
        /**
         * The "AppliesTo" address to send to the STS. The default is the endpoint address of the service provider.
         *
         * @since 2.5.0
         * @asciidoclet
         */
        @WssConfigurationConstant(key = "security.sts.applies-to")
        Optional<String> stsAppliesTo();

        /**
         * Whether to cancel a token when using SecureConversation after successful invocation. The default is "false".
         *
         * @since 2.5.0
         * @asciidoclet
         */
        @WssConfigurationConstant(key = "security.sts.token.do.cancel")
        Optional<String> stsTokenDoCancel();

        /**
         * Whether to fall back to calling "issue" after failing to renew an expired token. Some STSs do not support the renew
         * binding, and so we should just issue a new token after expiry. The default is true.
         *
         * @since 2.5.0
         * @asciidoclet
         */
        @WssConfigurationConstant(key = "security.issue.after.failed.renew")
        Optional<String> stsIssueAfterFailedRenew();

        /**
         * Set this to "false" to not cache a SecurityToken per proxy object in the IssuedTokenInterceptorProvider. This should
         * be done if a token is being retrieved from an STS in an intermediary. The default value is "true".
         *
         * @since 2.5.0
         * @asciidoclet
         */
        @WssConfigurationConstant(key = "security.cache.issued.token.in.endpoint")
        Optional<String> cacheIssuedTokenInEndpoint();

        /**
         * Whether to avoid STS client trying send WS-MetadataExchange call using STS EPR WSA address when the endpoint contract
         * contains no WS-MetadataExchange info. The default value is "false".
         *
         * @since 2.5.0
         * @asciidoclet
         */
        @WssConfigurationConstant(key = "security.sts.disable-wsmex-call-using-epr-address")
        Optional<String> disableStsClientWsmexCallUsingEprAddress();

        /**
         * Whether to prefer to use WS-MEX over a STSClient's location/wsdlLocation properties when making an STS
         * RequestSecurityToken call. This can be set to true for the scenario of making a WS-MEX call to an initial STS, and
         * using the returned token to make another call to an STS (which is configured using the STSClient configuration).
         * Default is "false".
         *
         * @since 2.5.0
         * @asciidoclet
         */
        @WssConfigurationConstant(key = "security.sts.prefer-wsmex")
        Optional<String> preferWsmexOverStsClientConfig();

        /**
         * The token to be sent to the STS in an "ActAs" field. It can be either: a) A String (which must be an XML statement
         * like "...") b) A DOM Element c) A CallbackHandler object to use to obtain the token In the case of a CallbackHandler,
         * it must be able to handle a org.apache.cxf.ws.security.trust.delegation.DelegationCallback Object, which contains a
         * reference to the current Message. The CallbackHandler implementation is required to set the token Element to be sent
         * in the request on the Callback. Some examples that can be reused are:
         * org.apache.cxf.ws.security.trust.delegation.ReceivedTokenCallbackHandler
         * org.apache.cxf.ws.security.trust.delegation.WSSUsernameCallbackHandler
         *
         * @since 2.5.0
         * @asciidoclet
         */
        @WssConfigurationConstant(key = "security.sts.token.act-as")
        Optional<String> stsTokenActAs();

        /**
         * The token to be sent to the STS in an "OnBehalfOf" field. It can be either: a) A String (which must be an XML
         * statement like "...") b) A DOM Element c) A CallbackHandler object to use to obtain the token In the case of a
         * CallbackHandler, it must be able to handle a org.apache.cxf.ws.security.trust.delegation.DelegationCallback Object,
         * which contains a reference to the current Message. The CallbackHandler implementation is required to set the token
         * Element to be sent in the request on the Callback. Some examples that can be reused are:
         * org.apache.cxf.ws.security.trust.delegation.ReceivedTokenCallbackHandler
         * org.apache.cxf.ws.security.trust.delegation.WSSUsernameCallbackHandler
         *
         * @since 2.5.0
         * @asciidoclet
         */
        @WssConfigurationConstant(key = "security.sts.token.on-behalf-of")
        Optional<String> stsTokenOnBehalfOf();

        /**
         * This is the value in seconds within which a token is considered to be expired by the client. When a cached token
         * (from a STS) is retrieved by the client, it is considered to be expired if it will expire in a time less than the
         * value specified by this tag. This prevents token expiry when the message is en route / being processed by the
         * service. When the token is found to be expired then it will be renewed via the STS. The default value is 10
         * (seconds). Specify 0 to avoid this check.
         *
         * @since 2.5.0
         * @asciidoclet
         */
        @WssConfigurationConstant(key = "security.sts.token.imminent-expiry-value")
        Optional<String> stsTokenImminentExpiryValue();

        /**
         * An implementation of the STSTokenCacher interface, if you want to plug in custom caching behaviour for STS clients.
         * The default value is the DefaultSTSTokenCacher.
         *
         * @since 2.5.0
         * @asciidoclet
         */
        @WssConfigurationConstant(key = "security.sts.token.cacher.impl")
        Optional<String> stsTokenCacherImpl();

        /**
         * Check that we are not invoking on the STS using its own IssuedToken policy - in which case we will end up with a
         * recursive loop. This check might be a problem in the unlikely scenario that the remote endpoint has the same service
         * / port QName as the STS, so this configuration flag allows to disable this check for that scenario. The default is
         * "true".
         *
         * @since 2.5.0
         * @asciidoclet
         */
        @WssConfigurationConstant(key = "security.sts.check.for.recursive.call")
        Optional<String> stsCheckForRecursiveCall();

        /**
         * This property contains a comma separated String corresponding to a list of audience restriction URIs. The default
         * value for this property contains the request URL and the Service QName. If the AUDIENCE_RESTRICTION_VALIDATION
         * property is "true", and if a received SAML Token contains audience restriction URIs, then one of them must match one
         * of the values specified in this property.
         *
         * @since 2.5.0
         * @asciidoclet
         */
        @WssConfigurationConstant(key = "security.audience-restrictions")
        Optional<String> audienceRestrictions();
    }
}
