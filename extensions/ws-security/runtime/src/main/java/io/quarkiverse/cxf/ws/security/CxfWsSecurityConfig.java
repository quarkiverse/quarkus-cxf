package io.quarkiverse.cxf.ws.security;

import static io.quarkiverse.cxf.ws.security.WssConfigurationConstant.Transformer.beanRef;
import static io.quarkiverse.cxf.ws.security.WssConfigurationConstant.Transformer.properties;

import java.util.Map;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

// The quarkus prefix is a workaround for https://github.com/quarkusio/quarkus/issues/36189
@ConfigMapping(prefix = "quarkus")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface CxfWsSecurityConfig {

    /**
     * Client configurations.
     */
    @WithName("cxf.client")
    Map<String, ClientOrEndpointConfig> clients();

    /**
     * Endpoint configurations.
     */
    @WithName("cxf.endpoint")
    Map<String, ClientOrEndpointConfig> endpoints();

    /**
     * A class that provides configurable options of a CXF client.
     */
    @ConfigGroup
    interface ClientOrEndpointConfig {
        /**
         * WS-Security related client configuration
         */
        SecurityConfig security();
    }

    /**
     * A class that provides configurable options of a CXF client.
     */
    @ConfigGroup
    public interface SecurityConfig {

        // org.apache.cxf.rt.security.SecurityConstants

        /**
         * The user's name. It is used as follows:
         * <ul>
         * <li>As the name in the UsernameToken for WS-Security
         * <li>As the alias name in the keystore to get the user's cert and private key for signature if
         * {@code signature.username} is not set
         * <li>As the alias name in the keystore to get the user's public key for encryption if
         * {@code encryption.username} is not set
         * </ul>
         */
        @WssConfigurationConstant(key = "security.username")
        Optional<String> username();

        /**
         * The user's password when a {@code callback-handler} is not defined. This is only used for the password
         * in a WS-Security UsernameToken.
         */
        @WssConfigurationConstant(key = "security.password")
        Optional<String> password();

        /**
         * The user's name for signature. It is used as the alias name in the keystore to get the user's cert
         * and private key for signature. If this is not defined, then {@link username} is used instead. If
         * that is also not specified, it uses the the default alias set in the properties file referenced by
         * {@code signature.properties}. If that's also not set, and the keystore only contains a single key,
         * that key will be used.
         * <p>
         * This option is experimental, because it is
         * <a href="https://github.com/quarkiverse/quarkus-cxf/issues/1052">not covered by tests</a> yet.
         */
        @WssConfigurationConstant(key = "security.signature.username")
        @WithName("signature.username")
        Optional<String> signatureUsername();

        /**
         * The user's password for signature when a {@code callback-handler} is not defined.
         * <p>
         * This option is experimental, because it is
         * <a href="https://github.com/quarkiverse/quarkus-cxf/issues/1052">not covered by tests</a> yet.
         */
        @WssConfigurationConstant(key = "security.signature.password")
        @WithName("signature.password")
        Optional<String> signaturePassword();

        /**
         * The user's name for encryption. It is used as the alias name in the keystore to get the user's public
         * key for encryption. If this is not defined, then {@code username} is used instead. If
         * that is also not specified, it uses the the default alias set in the properties file referenced by
         * {@code encrypt.properties}. If that's also not set, and the keystore only contains a single key,
         * that key will be used.
         * <p>
         * For the WS-Security web service provider, the {@code useReqSigCert} value can be used to accept (encrypt to)
         * any client whose public key is in the service's truststore (defined in {@code encrypt.properties}).
         * <p>
         * This option is experimental, because it is
         * <a href="https://github.com/quarkiverse/quarkus-cxf/issues/1052">not covered by tests</a> yet.
         */
        @WssConfigurationConstant(key = "security.encryption.username")
        @WithName("encryption.username")
        Optional<String> encryptionUsername();

        //
        // Callback class and Crypto properties
        //

        /**
         * A <a href="../../user-guide/configuration.html#beanRefs">reference</a> to a
         * {@code javax.security.auth.callback.CallbackHandler} bean used to obtain passwords, for both outbound and
         * inbound requests.
         */
        @WssConfigurationConstant(key = "security.callback-handler", transformer = beanRef)
        Optional<String> callbackHandler();

        /**
         * A <a href="../../user-guide/configuration.html#beanRefs">reference</a> to a
         * {@code javax.security.auth.callback.CallbackHandler} implementation used to construct SAML Assertions.
         * <p>
         * This option is experimental, because it is
         * <a href="https://github.com/quarkiverse/quarkus-cxf/issues/1052">not covered by tests</a> yet.
         */
        @WssConfigurationConstant(key = "security.saml-callback-handler", transformer = beanRef)
        Optional<String> samlCallbackHandler();

        /**
         * The Crypto property configuration to use for signing, if {@code signature.crypto} is not set.
         * <p>
         * Example
         *
         * <pre>
         * [prefix].signature.properties."org.apache.ws.security.crypto.provider" = org.apache.ws.security.components.crypto.Merlin
         * [prefix].signature.properties."org.apache.ws.security.crypto.merlin.keystore.password" = password
         * [prefix].signature.properties."org.apache.ws.security.crypto.merlin.file" = certs/alice.jks
         * </pre>
         * <p>
         * This option is experimental, because it is
         * <a href="https://github.com/quarkiverse/quarkus-cxf/issues/1052">not covered by tests</a> yet.
         */
        @WssConfigurationConstant(key = "security.signature.properties", transformer = properties)
        @WithName("signature.properties")
        Map<String, String> signatureProperties();

        /**
         * The Crypto property configuration to use for encryption, if {@code encryption.crypto} is not set.
         * <p>
         * Example
         *
         * <pre>
         * [prefix].encryption.properties."org.apache.ws.security.crypto.provider" = org.apache.ws.security.components.crypto.Merlin
         * [prefix].encryption.properties."org.apache.ws.security.crypto.merlin.keystore.password" = password
         * [prefix].encryption.properties."org.apache.ws.security.crypto.merlin.file" = certs/alice.jks
         * </pre>
         * <p>
         * This option is experimental, because it is
         * <a href="https://github.com/quarkiverse/quarkus-cxf/issues/1052">not covered by tests</a> yet.
         */
        @WssConfigurationConstant(key = "security.encryption.properties", transformer = properties)
        @WithName("encryption.properties")
        Optional<String> encryptionProperties();

        /**
         * A <a href="../../user-guide/configuration.html#beanRefs">reference</a> to a
         * {@code org.apache.wss4j.common.crypto.Crypto} bean to be used for signature. If not set,
         * {@code signature.properties} will be used to configure a {@code Crypto} instance.
         * <p>
         * This option is experimental, because it is
         * <a href="https://github.com/quarkiverse/quarkus-cxf/issues/1052">not covered by tests</a> yet.
         */
        @WssConfigurationConstant(key = "security.signature.crypto", transformer = beanRef)
        @WithName("signature.crypto")
        Optional<String> signatureCrypto();

        /**
         * A <a href="../../user-guide/configuration.html#beanRefs">reference</a> to a
         * {@code org.apache.wss4j.common.crypto.Crypto} to be used for encryption. If not set,
         * {@code encryption.properties} will be used to configure a {@code Crypto} instance.
         * <p>
         * This option is experimental, because it is
         * <a href="https://github.com/quarkiverse/quarkus-cxf/issues/1052">not covered by tests</a> yet.
         */
        @WssConfigurationConstant(key = "security.encryption.crypto", transformer = beanRef)
        @WithName("encryption.crypto")
        Optional<String> encryptionCrypto();

        /**
         * A message property for prepared X509 certificate to be used for encryption. If this is not defined, then the
         * certificate will be either loaded from the keystore {@code encryption.properties} or extracted from request
         * (when WS-Security is used and if {@code encryption.username} has value {@code useReqSigCert}.
         * <p>
         * This option is experimental, because it is
         * <a href="https://github.com/quarkiverse/quarkus-cxf/issues/1052">not covered by tests</a> yet.
         */
        @WssConfigurationConstant(key = "security.encryption.certificate")
        @WithName("encryption.certificate")
        Optional<String> encryptionCertificate();

        //
        // Boolean Security configuration tags, e.g. the value should be "true" or "false".
        //
        /**
         * If {@code true}, Certificate Revocation List (CRL) checking is enabled when verifying trust in a certificate;
         * otherwise it is not enabled.
         * <p>
         * This option is experimental, because it is
         * <a href="https://github.com/quarkiverse/quarkus-cxf/issues/1052">not covered by tests</a> yet.
         */
        @WssConfigurationConstant(key = "security.enableRevocation")
        @WithDefault("false")
        boolean enableRevocation();

        /**
         * If {@code true}, unsigned SAML assertions will be allowed as SecurityContext Principals; otherwise they won't
         * be allowed as SecurityContext Principals. Note that "unsigned" refers to an internal signature. Even if the
         * token is signed by an external signature (as per the "sender-vouches" requirement), this boolean must still
         * be configured if you want to use the token to set up the security context.
         * <p>
         * This option is experimental, because it is
         * <a href="https://github.com/quarkiverse/quarkus-cxf/issues/1052">not covered by tests</a> yet.
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
         * If {@code true}, the {@code SubjectConfirmation} requirements of a received SAML Token
         * (sender-vouches or holder-of-key) will be validated; otherwise they won't be validated.
         * <p>
         * This option is experimental, because it is
         * <a href="https://github.com/quarkiverse/quarkus-cxf/issues/1052">not covered by tests</a> yet.
         */
        @WssConfigurationConstant(key = "security.validate.saml.subject.conf")
        @WithDefault("true")
        boolean validateSamlSubjectConfirmation();

        /**
         * If {@code true}, security context can be created from JAAS Subject; otherwise it must not be created from
         * JAAS Subject.
         * <p>
         * This option is experimental, because it is
         * <a href="https://github.com/quarkiverse/quarkus-cxf/issues/1052">not covered by tests</a> yet.
         */
        @WssConfigurationConstant(key = "security.sc.jaas-subject")
        @WithDefault("true")
        boolean scFromJaasSubject();

        /**
         * If {@code}, then if the SAML Token contains Audience Restriction URIs, one of them must match one of the
         * values in {@code audience.restrictions}; otherwise the SAML AudienceRestriction validation is disabled.
         * <p>
         * This option is experimental, because it is
         * <a href="https://github.com/quarkiverse/quarkus-cxf/issues/1052">not covered by tests</a> yet.
         */
        @WssConfigurationConstant(key = "security.validate.audience-restriction")
        @WithDefault("true")
        boolean audienceRestrictionValidation();

        //
        // Non-boolean WS-Security Configuration parameters
        //

        /**
         * The attribute URI of the SAML {@code AttributeStatement} where the role information is stored.
         * <p>
         * This option is experimental, because it is
         * <a href="https://github.com/quarkiverse/quarkus-cxf/issues/1052">not covered by tests</a> yet.
         */
        @WssConfigurationConstant(key = "security.saml-role-attributename")
        @WithDefault("http://schemas.xmlsoap.org/ws/2005/05/identity/claims/role")
        String samlRoleAttributename();

        /**
         * A String of regular expressions (separated by the value specified in
         * {@code security.cert.constraints.separator}) which will be applied to the subject DN of the certificate used
         * for signature validation, after trust verification of the certificate chain associated with the certificate.
         * <p>
         * This option is experimental, because it is
         * <a href="https://github.com/quarkiverse/quarkus-cxf/issues/1052">not covered by tests</a> yet.
         */
        @WssConfigurationConstant(key = "security.subject.cert.constraints")
        Optional<String> subjectCertConstraints();

        /**
         * The separator that is used to parse certificate constraints configured in
         * {@code security.subject.cert.constraints}
         * <p>
         * This option is experimental, because it is
         * <a href="https://github.com/quarkiverse/quarkus-cxf/issues/1052">not covered by tests</a> yet.
         */
        @WssConfigurationConstant(key = "security.cert.constraints.separator")
        @WithDefault(",")
        String certConstraintsSeparator();

        // org.apache.cxf.ws.security.SecurityConstants
        // User properties
        //

        /**
         * The actor or role name of the {@code wsse:Security} header. If this parameter
         * is omitted, the actor name is not set.
         * <p>
         * This option is experimental, because it is
         * <a href="https://github.com/quarkiverse/quarkus-cxf/issues/1052">not covered by tests</a> yet.
         */
        @WssConfigurationConstant(key = "ws-security.actor")
        @WithName("actor")
        Optional<String> actor();

        //
        // Boolean WS-Security configuration tags, e.g. the value should be "true" or "false".
        //
        /**
         * If {@code true}, the password of a received {@code UsernameToken} will be validated; otherwise it won't be validated.
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
         * Whether to always encrypt {@code UsernameTokens} that are defined as a {@code SupportingToken}. This should
         * not be set to {@code false} in a production environment, as it exposes the password (or the digest of the
         * password) on the wire.
         * <p>
         * This option is experimental, because it is
         * <a href="https://github.com/quarkiverse/quarkus-cxf/issues/1052">not covered by tests</a> yet.
         */
        @WssConfigurationConstant(key = "ws-security.username-token.always.encrypted")
        @WithName("username-token.always.encrypted")
        @WithDefault("true")
        boolean alwaysEncryptUt();

        /**
         * If {@code true}, the compliance with the Basic Security Profile (BSP) 1.1 will be ensured; otherwise it will
         * not be ensured.
         * <p>
         * This option is experimental, because it is
         * <a href="https://github.com/quarkiverse/quarkus-cxf/issues/1052">not covered by tests</a> yet.
         */
        @WssConfigurationConstant(key = "ws-security.is-bsp-compliant")
        @WithName("is-bsp-compliant")
        @WithDefault("true")
        boolean isBspCompliant();

        /**
         * If {@code true}, the {@code UsernameToken} nonces will be cached for both message initiators and recipients;
         * otherwise they won't be cached for neither message initiators nor recipients. The default is {@code true} for
         * message recipients, and {@code false} for message initiators.
         * <p>
         * Note that caching only applies when either a {@code UsernameToken} WS-SecurityPolicy is in effect, or the
         * {@code UsernameToken} action has been configured for the non-security-policy case.
         */
        @WssConfigurationConstant(key = "ws-security.enable.nonce.cache")
        @WithName("enable.nonce.cache")
        Optional<Boolean> enableNonceCache();

        /**
         * If {@code true}, the {@code Timestamp} {@code Created} Strings (these are only cached in conjunction with a
         * message Signature) will be cached for both message initiators and recipients; otherwise they won't be cached
         * for neither message initiators nor recipients. The default is {@code true} for message recipients, and
         * {@code false} for message initiators.
         * <p>
         * Note that caching only applies when either a {@code IncludeTimestamp} policy is in effect, or the
         * {@code Timestamp} action has been configured for the non-security-policy case.
         * <p>
         * This option is experimental, because it is
         * <a href="https://github.com/quarkiverse/quarkus-cxf/issues/1052">not covered by tests</a> yet.
         */
        @WssConfigurationConstant(key = "ws-security.enable.timestamp.cache")
        @WithName("enable.timestamp.cache")
        Optional<Boolean> enableTimestampCache();

        /**
         * If {@code true}, the new streaming (StAX) implementation of WS-Security is used; otherwise the old DOM
         * implementation is used.
         */
        @WssConfigurationConstant(key = "ws-security.enable.streaming")
        @WithName("enable.streaming")
        @WithDefault("false")
        boolean enableStreaming();

        /**
         * If {@code true}, detailed security error messages are sent to clients; otherwise the details are omitted
         * and only a generic error message is sent.
         * <p>
         * The "real" security errors should not be returned to the client in production, as they may leak information
         * about the deployment, or otherwise provide an "oracle" for attacks.
         */
        @WssConfigurationConstant(key = "ws-security.return.security.error")
        @WithName("return.security.error")
        @WithDefault("false")
        boolean returnSecurityError();

        /**
         * If {@code true}, the SOAP {@code mustUnderstand} header is included in security headers based on
         * a WS-SecurityPolicy; otherwise the header is always omitted.
         */
        @WssConfigurationConstant(key = "ws-security.must-understand")
        @WithName("must-understand")
        @WithDefault("true")
        boolean mustUnderstand();

        /**
         * If {@code true} and in case the token contains a {@code OneTimeUse} Condition, the SAML2 Token Identifiers
         * will be cached for both message initiators and recipients; otherwise they won't be cached for neither message
         * initiators nor recipients. The default is {@code true} for message recipients, and {@code false} for message
         * initiators.
         * <p>
         * Note that caching only applies when either a {@code SamlToken} policy is in effect, or a SAML action has been
         * configured for the non-security-policy case.
         * <p>
         * This option is experimental, because it is
         * <a href="https://github.com/quarkiverse/quarkus-cxf/issues/1052">not covered by tests</a> yet.
         */
        @WssConfigurationConstant(key = "ws-security.enable.saml.cache")
        @WithName("enable.saml.cache")
        Optional<Boolean> enableSamlOneTimeUseCache();

        /**
         * Whether to store bytes (CipherData or BinarySecurityToken) in an attachment. The default is
         * true if MTOM is enabled. Set it to false to BASE-64 encode the bytes and "inlined" them in
         * the message instead. Setting this to true is more efficient, as it means that the BASE-64
         * encoding step can be skipped. This only applies to the DOM WS-Security stack.
         * <p>
         * This option is experimental, because it is
         * <a href="https://github.com/quarkiverse/quarkus-cxf/issues/1052">not covered by tests</a> yet.
         */
        @WssConfigurationConstant(key = "ws-security.store.bytes.in.attachment")
        @WithName("store.bytes.in.attachment")
        Optional<Boolean> storeBytesInAttachment();

        /**
         * If {@code true}, {@code Attachment-Content-Only} transform will be used when an Attachment is encrypted
         * via a WS-SecurityPolicy expression; otherwise {@code Attachment-Complete} transform will be used when an
         * Attachment is encrypted via a WS-SecurityPolicy expression.
         * <p>
         * This option is experimental, because it is
         * <a href="https://github.com/quarkiverse/quarkus-cxf/issues/1052">not covered by tests</a> yet.
         */
        @WssConfigurationConstant(key = "ws-security.swa.encryption.attachment.transform.content")
        @WithName("swa.encryption.attachment.transform.content")
        @WithDefault("false")
        boolean useAttachmentEncryptionContentOnlyTransform();

        /**
         * If {@code true}, the STR (Security Token Reference) Transform will be used when (externally) signing a SAML
         * Token; otherwise the STR (Security Token Reference) Transform will not be used.
         * <p>
         * Some frameworks cannot process the {@code SecurityTokenReference}. You may set this {@code false} in such
         * cases.
         * <p>
         * This option is experimental, because it is
         * <a href="https://github.com/quarkiverse/quarkus-cxf/issues/1052">not covered by tests</a> yet.
         */
        @WssConfigurationConstant(key = "ws-security.use.str.transform")
        @WithName("use.str.transform")
        @WithDefault("true")
        boolean useStrTransform();

        /**
         * If {@code true}, an {@code InclusiveNamespaces} {@code PrefixList} will be added as a
         * {@code CanonicalizationMethod} child when generating Signatures using
         * {@code WSConstants.C14N_EXCL_OMIT_COMMENTS}; otherwise the {@code PrefixList} will not be added.
         * <p>
         * This option is experimental, because it is
         * <a href="https://github.com/quarkiverse/quarkus-cxf/issues/1052">not covered by tests</a> yet.
         */
        @WssConfigurationConstant(key = "ws-security.add.inclusive.prefixes")
        @WithName("add.inclusive.prefixes")
        @WithDefault("true")
        boolean addInclusivePrefixes();

        /**
         * If {@code true}, the enforcement of the WS-SecurityPolicy {@code RequireClientCertificate} policy will be
         * disabled; otherwise the enforcement of the WS-SecurityPolicy {@code RequireClientCertificate} policy is
         * enabled.
         * <p>
         * Some servers may not do client certificate verification at the start of the SSL handshake, and therefore the
         * client certificates may not be available to the WS-Security layer for policy verification.
         * <p>
         * This option is experimental, because it is
         * <a href="https://github.com/quarkiverse/quarkus-cxf/issues/1052">not covered by tests</a> yet.
         */
        @WssConfigurationConstant(key = "ws-security.disable.require.client.cert.check")
        @WithName("disable.require.client.cert.check")
        @WithDefault("false")
        boolean disableReqClientCertCheck();

        /**
         * If {@code true}, the {@code xop:Include} elements will be searched for encryption and signature (on the
         * outbound side) or for signature verification (on the inbound side); otherwise the search won't happen.
         * This ensures that the actual bytes are signed, and not just the reference. The default is {@code true} if
         * MTOM is enabled, otherwise the default is {@code false}.
         * <p>
         * This option is experimental, because it is
         * <a href="https://github.com/quarkiverse/quarkus-cxf/issues/1052">not covered by tests</a> yet.
         */
        @WssConfigurationConstant(key = "ws-security.expand.xop.include")
        @WithName("expand.xop.include")
        Optional<Boolean> expandXopInclude();

        //
        // Non-boolean WS-Security Configuration parameters
        //

        /**
         * The time in seconds to add to the Creation value of an incoming {@code Timestamp} to determine
         * whether to accept it as valid or not.
         * <p>
         * This option is experimental, because it is
         * <a href="https://github.com/quarkiverse/quarkus-cxf/issues/1052">not covered by tests</a> yet.
         */
        @WssConfigurationConstant(key = "ws-security.timestamp.timeToLive")
        @WithName("timestamp.timeToLive")
        @WithDefault("300")
        Optional<String> timestampTtl();

        /**
         * The time in seconds in the future within which the {@code Created} time of an incoming
         * {@code Timestamp} is valid. The default is greater than zero to avoid problems where clocks are
         * slightly askew. Set this to {@code 0} to reject all future-created {@code Timestamp}s.
         * <p>
         * This option is experimental, because it is
         * <a href="https://github.com/quarkiverse/quarkus-cxf/issues/1052">not covered by tests</a> yet.
         */
        @WssConfigurationConstant(key = "ws-security.timestamp.futureTimeToLive")
        @WithName("timestamp.futureTimeToLive")
        @WithDefault("60")
        Optional<String> timestampFutureTtl();

        /**
         * The time in seconds to append to the Creation value of an incoming {@code UsernameToken} to determine
         * whether to accept it as valid or not.
         * <p>
         * This option is experimental, because it is
         * <a href="https://github.com/quarkiverse/quarkus-cxf/issues/1052">not covered by tests</a> yet.
         */
        @WssConfigurationConstant(key = "ws-security.usernametoken.timeToLive")
        @WithName("usernametoken.timeToLive")
        @WithDefault("300")
        Optional<String> usernametokenTtl();

        /**
         * The time in seconds in the future within which the {@code Created} time of an incoming
         * {@code UsernameToken} is valid. The default is greater than zero to avoid problems where clocks are
         * slightly askew. Set this to {@code 0} to reject all future-created {@code UsernameToken}s.
         * <p>
         * This option is experimental, because it is
         * <a href="https://github.com/quarkiverse/quarkus-cxf/issues/1052">not covered by tests</a> yet.
         */
        @WssConfigurationConstant(key = "ws-security.usernametoken.futureTimeToLive")
        @WithName("usernametoken.futureTimeToLive")
        @WithDefault("60")
        Optional<String> usernametokenFutureTtl();

        /**
         * A <a href="../../user-guide/configuration.html#beanRefs">reference</a> to a
         * {@code org.apache.wss4j.common.spnego.SpnegoClientAction} bean to use for SPNEGO. This allows the user to
         * plug in a different implementation to obtain a service ticket.
         * <p>
         * This option is experimental, because it is
         * <a href="https://github.com/quarkiverse/quarkus-cxf/issues/1052">not covered by tests</a> yet.
         */
        @WssConfigurationConstant(key = "ws-security.spnego.client.action", transformer = beanRef)
        @WithName("spnego.client.action")
        Optional<String> spnegoClientAction();

        /**
         * A <a href="../../user-guide/configuration.html#beanRefs">reference</a> to a
         * {@code org.apache.wss4j.common.cache.ReplayCache} bean used to cache {@code UsernameToken} nonces. A
         * {@code org.apache.wss4j.common.cache.EHCacheReplayCache} instance is used by default.
         * <p>
         * This option is experimental, because it is
         * <a href="https://github.com/quarkiverse/quarkus-cxf/issues/1052">not covered by tests</a> yet.
         */
        @WssConfigurationConstant(key = "ws-security.nonce.cache.instance", transformer = beanRef)
        @WithName("nonce.cache.instance")
        Optional<String> nonceCacheInstance();

        /**
         * A <a href="../../user-guide/configuration.html#beanRefs">reference</a> to a
         * {@code org.apache.wss4j.common.cache.ReplayCache} bean used to cache {@code Timestamp} {@code Created}
         * Strings. A {@code org.apache.wss4j.common.cache.EHCacheReplayCache} instance is used by default.
         * <p>
         * This option is experimental, because it is
         * <a href="https://github.com/quarkiverse/quarkus-cxf/issues/1052">not covered by tests</a> yet.
         */
        @WssConfigurationConstant(key = "ws-security.timestamp.cache.instance", transformer = beanRef)
        @WithName("timestamp.cache.instance")
        Optional<String> timestampCacheInstance();

        /**
         * A <a href="../../user-guide/configuration.html#beanRefs">reference</a> to a
         * {@code org.apache.wss4j.common.cache.ReplayCache} bean used to cache SAML2 Token Identifier Strings (if the
         * token contains a {@code OneTimeUse} condition). A {@code org.apache.wss4j.common.cache.EHCacheReplayCache}
         * instance is used by default.
         * <p>
         * This option is experimental, because it is
         * <a href="https://github.com/quarkiverse/quarkus-cxf/issues/1052">not covered by tests</a> yet.
         */
        @WssConfigurationConstant(key = "ws-security.saml.cache.instance", transformer = beanRef)
        @WithName("saml.cache.instance")
        Optional<String> samlOneTimeUseCacheInstance();

        /**
         * Set this property to point to a configuration file for the underlying caching implementation for the
         * {@code TokenStore}. The default configuration file that is used is {@code cxf-ehcache.xml} in
         * {@code org.apache.cxf:cxf-rt-security} JAR.
         * <p>
         * This option is experimental, because it is
         * <a href="https://github.com/quarkiverse/quarkus-cxf/issues/1052">not covered by tests</a> yet.
         */
        @WssConfigurationConstant(key = "ws-security.cache.config.file")
        @WithName("cache.config.file")
        Optional<String> cacheConfigFile();

        /**
         * A <a href="../../user-guide/configuration.html#beanRefs">reference</a> to a
         * {@code org.apache.cxf.ws.security.tokenstore.TokenStore} bean to use for caching security tokens. By default
         * this uses a {@codeorg.apache.cxf.ws.security.tokenstore.EHCacheTokenStore} instance.
         * <p>
         * This option is experimental, because it is
         * <a href="https://github.com/quarkiverse/quarkus-cxf/issues/1052">not covered by tests</a> yet.
         */
        @WssConfigurationConstant(key = "org.apache.cxf.ws.security.tokenstore.TokenStore", transformer = beanRef)
        Optional<String> tokenStoreCacheInstance();

        /**
         * The Cache Identifier to use with the TokenStore. CXF uses the following key to retrieve a
         * token store: {@code org.apache.cxf.ws.security.tokenstore.TokenStore-<identifier>}. This key can be
         * used to configure service-specific cache configuration. If the identifier does not match, then it
         * falls back to a cache configuration with key {@code org.apache.cxf.ws.security.tokenstore.TokenStore}.
         * <p>
         * The default {@code <identifier>} is the QName of the service in question. However to pick up a
         * custom cache configuration (for example, if you want to specify a TokenStore per-client proxy),
         * it can be configured with this identifier instead.
         * <p>
         * This option is experimental, because it is
         * <a href="https://github.com/quarkiverse/quarkus-cxf/issues/1052">not covered by tests</a> yet.
         */
        @WssConfigurationConstant(key = "ws-security.cache.identifier")
        @WithName("cache.identifier")
        Optional<String> cacheIdentifier();

        /**
         * The Subject Role Classifier to use. If one of the WSS4J Validators returns a JAAS Subject
         * from Validation, then the {@code WSS4JInInterceptor} will attempt to create a {@code SecurityContext}
         * based on this Subject. If this value is not specified, then it tries to get roles using
         * the {@code DefaultSecurityContext} in {@code org.apache.cxf:cxf-core}. Otherwise it uses this value in
         * combination with the {@code role.classifier.type} to get the roles from the {@code Subject}.
         * <p>
         * This option is experimental, because it is
         * <a href="https://github.com/quarkiverse/quarkus-cxf/issues/1052">not covered by tests</a> yet.
         */
        @WssConfigurationConstant(key = "ws-security.role.classifier")
        @WithName("role.classifier")
        Optional<String> subjectRoleClassifier();

        /**
         * The Subject Role Classifier Type to use. If one of the WSS4J Validators returns a JAAS Subject
         * from Validation, then the {@code WSS4JInInterceptor} will attempt to create a {@code SecurityContext}
         * based on this Subject. Currently accepted values are {@code prefix} or {@code classname}. Must be
         * used in conjunction with the {@code role.classifier}.
         * <p>
         * This option is experimental, because it is
         * <a href="https://github.com/quarkiverse/quarkus-cxf/issues/1052">not covered by tests</a> yet.
         */
        @WssConfigurationConstant(key = "ws-security.role.classifier.type")
        @WithName("role.classifier.type")
        @WithDefault("prefix")
        String subjectRoleClassifierType();

        /**
         * This configuration tag allows the user to override the default Asymmetric Signature
         * algorithm (RSA-SHA1) for use in WS-SecurityPolicy, as the WS-SecurityPolicy specification
         * does not allow the use of other algorithms at present.
         * <p>
         * This option is experimental, because it is
         * <a href="https://github.com/quarkiverse/quarkus-cxf/issues/1052">not covered by tests</a> yet.
         */
        @WssConfigurationConstant(key = "ws-security.asymmetric.signature.algorithm")
        @WithName("asymmetric.signature.algorithm")
        Optional<String> asymmetricSignatureAlgorithm();

        /**
         * This configuration tag allows the user to override the default Symmetric Signature
         * algorithm (HMAC-SHA1) for use in WS-SecurityPolicy, as the WS-SecurityPolicy specification
         * does not allow the use of other algorithms at present.
         * <p>
         * This option is experimental, because it is
         * <a href="https://github.com/quarkiverse/quarkus-cxf/issues/1052">not covered by tests</a> yet.
         */
        @WssConfigurationConstant(key = "ws-security.symmetric.signature.algorithm")
        @WithName("symmetric.signature.algorithm")
        Optional<String> symmetricSignatureAlgorithm();

        /**
         * A <a href="../../user-guide/configuration.html#beanRefs">reference</a> to a
         * {@code org.apache.wss4j.common.crypto.PasswordEncryptor} bean, which is used to encrypt or
         * decrypt passwords in the Merlin Crypto implementation (or any custom Crypto implementations).
         * <p>
         * By default, WSS4J uses the {@code org.apache.wss4j.common.crypto.JasyptPasswordEncryptor} which must be
         * instantiated with a password to use to decrypt keystore passwords in the Merlin Crypto definition.
         * This password is obtained via the CallbackHandler defined via {@code callback-handler}
         * <p>
         * The encrypted passwords must be stored in the format "ENC(encoded encrypted password)".
         * <p>
         * This option is experimental, because it is
         * <a href="https://github.com/quarkiverse/quarkus-cxf/issues/1052">not covered by tests</a> yet.
         */
        @WssConfigurationConstant(key = "ws-security.password.encryptor.instance", transformer = beanRef)
        @WithName("password.encryptor.instance")
        Optional<String> passwordEncryptorInstance();

        /**
         * A <a href="../../user-guide/configuration.html#beanRefs">reference</a> to a Kerberos
         * {@code org.ietf.jgss.GSSCredential} bean to use for WS-Security. This is used to retrieve a service ticket
         * instead of using the client credentials.
         * <p>
         * This option is experimental, because it is
         * <a href="https://github.com/quarkiverse/quarkus-cxf/issues/1052">not covered by tests</a> yet.
         */
        @WssConfigurationConstant(key = "ws-security.delegated.credential", transformer = beanRef)
        @WithName("delegated.credential")
        Optional<String> delegatedCredential();

        /**
         * A <a href="../../user-guide/configuration.html#beanRefs">reference</a> to a
         * {@code org.apache.cxf.ws.security.wss4j.WSS4JSecurityContextCreator} bean that is used to create
         * a CXF SecurityContext from the set of WSS4J processing results. The default implementation is
         * {@code org.apache.cxf.ws.security.wss4j.DefaultWSS4JSecurityContextCreator}.
         * <p>
         * This option is experimental, because it is
         * <a href="https://github.com/quarkiverse/quarkus-cxf/issues/1052">not covered by tests</a> yet.
         */
        @WssConfigurationConstant(key = "ws-security.security.context.creator", transformer = beanRef)
        @WithName("security.context.creator")
        Optional<String> securityContextCreator();

        /**
         * The security token lifetime value (in milliseconds).
         * <p>
         * This option is experimental, because it is
         * <a href="https://github.com/quarkiverse/quarkus-cxf/issues/1052">not covered by tests</a> yet.
         */
        @WssConfigurationConstant(key = "ws-security.security.token.lifetime")
        @WithName("security.token.lifetime")
        @WithDefault("300000")
        long securityTokenLifetime();

        //
        // Kerberos Configuration tags
        //

        /**
         * If {@code true}, credential delegation is requested in the KerberosClient; otherwise the credential
         * delegation is not in the KerberosClient.
         * <p>
         * This option is experimental, because it is
         * <a href="https://github.com/quarkiverse/quarkus-cxf/issues/1052">not covered by tests</a> yet.
         */
        @WssConfigurationConstant(key = "ws-security.kerberos.request.credential.delegation")
        @WithName("kerberos.request.credential.delegation")
        @WithDefault("false")
        boolean kerberosRequestCredentialDelegation();

        /**
         * If {@code true}, GSSCredential bean is retrieved from the Message Context using the
         * {@code delegated.credential} property and then it is used to obtain a service ticket.
         * <p>
         * This option is experimental, because it is
         * <a href="https://github.com/quarkiverse/quarkus-cxf/issues/1052">not covered by tests</a> yet.
         */
        @WssConfigurationConstant(key = "ws-security.kerberos.use.credential.delegation")
        @WithName("kerberos.use.credential.delegation")
        @WithDefault("false")
        boolean kerberosUseCredentialDelegation();

        /**
         * If {@code true}, the Kerberos username is in servicename form; otherwise the Kerberos username is not in servicename
         * form.
         * <p>
         * This option is experimental, because it is
         * <a href="https://github.com/quarkiverse/quarkus-cxf/issues/1052">not covered by tests</a> yet.
         */
        @WssConfigurationConstant(key = "ws-security.kerberos.is.username.in.servicename.form")
        @WithName("kerberos.is.username.in.servicename.form")
        @WithDefault("false")
        boolean kerberosIsUsernameInServicenameForm();

        /**
         * The JAAS Context name to use for Kerberos.
         * <p>
         * This option is experimental, because it is
         * <a href="https://github.com/quarkiverse/quarkus-cxf/issues/1052">not covered by tests</a> yet.
         */
        @WssConfigurationConstant(key = "ws-security.kerberos.jaas.context")
        @WithName("kerberos.jaas.context")
        Optional<String> kerberosJaasContextName();

        /**
         * The Kerberos Service Provider Name (spn) to use.
         * <p>
         * This option is experimental, because it is
         * <a href="https://github.com/quarkiverse/quarkus-cxf/issues/1052">not covered by tests</a> yet.
         */
        @WssConfigurationConstant(key = "ws-security.kerberos.spn")
        @WithName("kerberos.spn")
        Optional<String> kerberosSpn();

        /**
         * A <a href="../../user-guide/configuration.html#beanRefs">reference</a> to a
         * {@code org.apache.cxf.ws.security.kerberos.KerberosClient} bean used to obtain a service ticket.
         * <p>
         * This option is experimental, because it is
         * <a href="https://github.com/quarkiverse/quarkus-cxf/issues/1052">not covered by tests</a> yet.
         */
        @WssConfigurationConstant(key = "ws-security.kerberos.client")
        @WithName("kerberos.client")
        Optional<String> kerberosClient();

    }

    /**
     * Ready for future use
     */
    interface ValidatorConfig {

        //
        // Validator implementations for validating received security tokens
        //

        /**
         * A <a href="../../user-guide/configuration.html#beanRefs">reference</a> to a
         * {@code org.apache.wss4j.stax.validate.Validator} bean to use to validate UsernameTokens.
         * The Default is the UsernameTokenValidator.
         */
        @WssConfigurationConstant(key = "ws-security.ut.validator", transformer = beanRef)
        @WithName("ut.validator")
        Optional<String> usernameTokenValidator();

        /**
         * A <a href="../../user-guide/configuration.html#beanRefs">reference</a> to a
         * {@code org.apache.wss4j.stax.validate.Validator} bean to use to validate SAML 1.1 Tokens. The default value is the
         * SamlAssertionValidator.
         */
        @WssConfigurationConstant(key = "ws-security.saml1.validator", transformer = beanRef)
        @WithName("saml1.validator")
        Optional<String> saml1TokenValidator();

        /**
         * A <a href="../../user-guide/configuration.html#beanRefs">reference</a> to a
         * {@code org.apache.wss4j.stax.validate.Validator} bean to use to validate SAML 2.0 Tokens. The default value is the
         * SamlAssertionValidator.
         */
        @WssConfigurationConstant(key = "ws-security.saml2.validator", transformer = beanRef)
        @WithName("saml2.validator")
        Optional<String> saml2TokenValidator();

        /**
         * A <a href="../../user-guide/configuration.html#beanRefs">reference</a> to a
         * {@code org.apache.wss4j.stax.validate.Validator} bean to use to validate Timestamps. The default value is the
         * TimestampValidator.
         */
        @WssConfigurationConstant(key = "ws-security.timestamp.validator", transformer = beanRef)
        @WithName("timestamp.validator")
        Optional<String> timestampTokenValidator();

        /**
         * A <a href="../../user-guide/configuration.html#beanRefs">reference</a> to a
         * {@code org.apache.wss4j.stax.validate.Validator} bean to use to validate trust in credentials used in
         * Signature verification. The default value is the SignatureTrustValidator.
         */
        @WssConfigurationConstant(key = "ws-security.signature.validator", transformer = beanRef)
        @WithName("signature.validator")
        Optional<String> signatureTokenValidator();

        /**
         * A <a href="../../user-guide/configuration.html#beanRefs">reference</a> to a
         * {@code org.apache.wss4j.stax.validate.Validator} bean to use to validate BinarySecurityTokens. The default value
         * is the NoOpValidator.
         */
        @WssConfigurationConstant(key = "ws-security.bst.validator", transformer = beanRef)
        @WithName("bst.validator")
        Optional<String> bstTokenValidator();

        /**
         * A <a href="../../user-guide/configuration.html#beanRefs">reference</a> to a
         * {@code org.apache.wss4j.stax.validate.Validator} bean to use to validate SecurityContextTokens. The default value is
         * the NoOpValidator.
         */
        @WssConfigurationConstant(key = "ws-security.sct.validator", transformer = beanRef)
        @WithName("sct.validator")
        Optional<String> sctTokenValidator();

        /**
         * This refers to a Map of QName, SecurityPolicyValidator, which retrieves a SecurityPolicyValidator
         * implementation to validate a particular security policy, based on the QName of the policy. Any
         * SecurityPolicyValidator implementation defined in this map will override the default value
         * used internally for the corresponding QName.
         */
        @WssConfigurationConstant(key = "ws-security.policy.validator.map", transformer = beanRef)
        @WithName("policy.validator.map")
        Optional<String> policyValidatorMap();
    }

    /**
     * Ready for future use
     */
    interface StsConfig {
        //
        // STS Client Configuration tags
        //

        /**
         * A reference to the STSClient class used to communicate with the STS.
         */
        @WssConfigurationConstant(key = "security.sts.client")
        Optional<String> stsClient();

        /**
         * The "AppliesTo" address to send to the STS. The default is the endpoint address of the
         * service provider.
         */
        @WssConfigurationConstant(key = "security.sts.applies-to")
        Optional<String> stsAppliesTo();

        /**
         * Whether to write out an X509Certificate structure in UseKey/KeyInfo, or whether to write
         * out a KeyValue structure. The default value is "false".
         */
        @WssConfigurationConstant(key = "security.sts.token.usecert")
        Optional<String> stsTokenUseCertForKeyinfo();

        /**
         * Whether to cancel a token when using SecureConversation after successful invocation. The
         * default is "false".
         */
        @WssConfigurationConstant(key = "security.sts.token.do.cancel")
        Optional<String> stsTokenDoCancel();

        /**
         * Whether to fall back to calling "issue" after failing to renew an expired token. Some
         * STSs do not support the renew binding, and so we should just issue a new token after expiry.
         * The default is true.
         */
        @WssConfigurationConstant(key = "security.issue.after.failed.renew")
        Optional<String> stsIssueAfterFailedRenew();

        /**
         * Set this to "false" to not cache a SecurityToken per proxy object in the
         * IssuedTokenInterceptorProvider. This should be done if a token is being retrieved
         * from an STS in an intermediary. The default value is "true".
         */
        @WssConfigurationConstant(key = "security.cache.issued.token.in.endpoint")
        Optional<String> cacheIssuedTokenInEndpoint();

        /**
         * Whether to avoid STS client trying send WS-MetadataExchange call using
         * STS EPR WSA address when the endpoint contract contains no WS-MetadataExchange info.
         * The default value is "false".
         */
        @WssConfigurationConstant(key = "security.sts.disable-wsmex-call-using-epr-address")
        Optional<String> disableStsClientWsmexCallUsingEprAddress();

        /**
         * Whether to prefer to use WS-MEX over a STSClient's location/wsdlLocation properties
         * when making an STS RequestSecurityToken call. This can be set to true for the scenario
         * of making a WS-MEX call to an initial STS, and using the returned token to make another
         * call to an STS (which is configured using the STSClient configuration). Default is
         * "false".
         */
        @WssConfigurationConstant(key = "security.sts.prefer-wsmex")
        Optional<String> preferWsmexOverStsClientConfig();

        /**
         * Switch STS client to send Soap 1.2 messages
         */
        @WssConfigurationConstant(key = "security.sts.client-soap12-binding")
        Optional<String> stsClientSoap12Binding();

        /**
         *
         * A Crypto object to be used for the STS. If this is not defined then the
         * {@link STS_TOKEN_PROPERTIES} is used instead.
         *
         * WCF's trust server sometimes will encrypt the token in the response IN ADDITION TO
         * the full security on the message. These properties control the way the STS client
         * will decrypt the EncryptedData elements in the response.
         *
         * These are also used by the STSClient to send/process any RSA/DSAKeyValue tokens
         * used if the KeyType is "PublicKey"
         */
        @WssConfigurationConstant(key = "security.sts.token.crypto")
        Optional<String> stsTokenCrypto();

        /**
         * The Crypto property configuration to use for the STS, if {@link STS_TOKEN_CRYPTO} is not
         * set instead.
         * The value of this tag must be either:
         * a) A Java Properties object that contains the Crypto configuration.
         * b) The path of the Crypto property file that contains the Crypto configuration.
         * c) A URL that points to the Crypto property file that contains the Crypto configuration.
         */
        @WssConfigurationConstant(key = "security.sts.token.properties")
        Optional<String> stsTokenProperties();

        /**
         * The alias name in the keystore to get the user's public key to send to the STS for the
         * PublicKey KeyType case.
         */
        @WssConfigurationConstant(key = "security.sts.token.username")
        Optional<String> stsTokenUsername();

        /**
         * The token to be sent to the STS in an "ActAs" field. It can be either:
         * a) A String (which must be an XML statement like "<wst:OnBehalfOf xmlns:wst=...>...</wst:OnBehalfOf>")
         * b) A DOM Element
         * c) A CallbackHandler object to use to obtain the token
         *
         * In the case of a CallbackHandler, it must be able to handle a
         * org.apache.cxf.ws.security.trust.delegation.DelegationCallback Object, which contains a
         * reference to the current Message. The CallbackHandler implementation is required to set
         * the token Element to be sent in the request on the Callback.
         *
         * Some examples that can be reused are:
         * org.apache.cxf.ws.security.trust.delegation.ReceivedTokenCallbackHandler
         * org.apache.cxf.ws.security.trust.delegation.WSSUsernameCallbackHandler
         */
        @WssConfigurationConstant(key = "security.sts.token.act-as")
        Optional<String> stsTokenActAs();

        /**
         * The token to be sent to the STS in an "OnBehalfOf" field. It can be either:
         * a) A String (which must be an XML statement like "<wst:OnBehalfOf xmlns:wst=...>...</wst:OnBehalfOf>")
         * b) A DOM Element
         * c) A CallbackHandler object to use to obtain the token
         *
         * In the case of a CallbackHandler, it must be able to handle a
         * org.apache.cxf.ws.security.trust.delegation.DelegationCallback Object, which contains a
         * reference to the current Message. The CallbackHandler implementation is required to set
         * the token Element to be sent in the request on the Callback.
         *
         * Some examples that can be reused are:
         * org.apache.cxf.ws.security.trust.delegation.ReceivedTokenCallbackHandler
         * org.apache.cxf.ws.security.trust.delegation.WSSUsernameCallbackHandler
         */
        @WssConfigurationConstant(key = "security.sts.token.on-behalf-of")
        Optional<String> stsTokenOnBehalfOf();

        /**
         * This is the value in seconds within which a token is considered to be expired by the
         * client. When a cached token (from a STS) is retrieved by the client, it is considered
         * to be expired if it will expire in a time less than the value specified by this tag.
         * This prevents token expiry when the message is en route / being processed by the
         * service. When the token is found to be expired then it will be renewed via the STS.
         *
         * The default value is 10 (seconds). Specify 0 to avoid this check.
         */
        @WssConfigurationConstant(key = "security.sts.token.imminent-expiry-value")
        Optional<String> stsTokenImminentExpiryValue();

        /**
         * An implementation of the STSTokenCacher interface, if you want to plug in custom caching behaviour for
         * STS clients. The default value is the DefaultSTSTokenCacher.
         */
        @WssConfigurationConstant(key = "security.sts.token.cacher.impl")
        Optional<String> stsTokenCacherImpl();

        /**
         * Check that we are not invoking on the STS using its own IssuedToken policy - in which case we
         * will end up with a recursive loop. This check might be a problem in the unlikely scenario that the
         * remote endpoint has the same service / port QName as the STS, so this configuration flag allows to
         * disable this check for that scenario. The default is "true".
         */
        @WssConfigurationConstant(key = "security.sts.check.for.recursive.call")
        Optional<String> stsCheckForRecursiveCall();

        /**
         * This property contains a comma separated String corresponding to a list of audience restriction URIs.
         * The default value for this property contains the request URL and the Service QName. If the
         * AUDIENCE_RESTRICTION_VALIDATION property is "true", and if a received SAML Token contains audience
         * restriction URIs, then one of them must match one of the values specified in this property.
         */
        @WssConfigurationConstant(key = "security.audience-restrictions")
        Optional<String> audienceRestrictions();
    }

}
