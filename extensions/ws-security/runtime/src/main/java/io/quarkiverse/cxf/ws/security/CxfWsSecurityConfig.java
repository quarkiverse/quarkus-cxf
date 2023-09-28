package io.quarkiverse.cxf.ws.security;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.quarkiverse.cxf.ws.security.WssConfigurationConstant.Transformer;
import io.quarkus.runtime.annotations.ConfigDocEnumValue;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithConverter;
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
        WsSecurityConfig wsSecurity();
    }

    /**
     * A class that provides configurable options of a CXF client.
     */
    @ConfigGroup
    public interface WsSecurityConfig {
        /**
         * A comma separated list of WS-Security actions to perform. The following actions are supported:
         * <ul>
         * <li>{@code UsernameToken} - Perform a UsernameToken action
         * <li>{@code UsernameTokenSignature} - Perform a UsernameTokenSignature action
         * <li>{@code UsernameTokenNoPassword} - Perform a UsernameToken action with no password.
         * <li>{@code SAMLTokenUnsigned} - Perform an unsigned SAML Token action.
         * <li>{@code SAMLTokenSigned} - Perform a signed SAML Token action.
         * <li>{@code Signature} - Perform a Signature action. The signature specific parameters define how to sign,
         * which keys
         * to use, and so on.
         * <li>{@code Encryption} - Perform an Encryption action. The encryption specific parameters define how to
         * encrypt,
         * which keys to use, and so on.
         * <li>{@code Timestamp} - Add a timestamp to the security header.
         * <li>{@code SignatureDerived} - Perform a Signature action with derived keys. The signature specific
         * parameters define
         * how to sign, which keys to use, and so on.
         * <li>{@code EncryptionDerived} - Perform an Encryption action with derived keys. The encryption specific
         * parameters
         * define how to encrypt, which keys to use, and so on.
         * <li>{@code SignatureWithKerberosToken} - Perform a Signature action with a kerberos token. The signature
         * specific
         * parameters define how to sign, which keys to use, and so on.
         * <li>{@code EncryptionWithKerberosToken} - Perform a Encryption action with a kerberos token. The signature
         * specific
         * parameters define how to encrypt, which keys to use, and so on.
         * <li>{@code KerberosToken} - Add a kerberos token.
         * <li>{@code CustomToken} - Add a "Custom" token. This token will be retrieved from a CallbackHandler via
         * {@code WSPasswordCallback.Usage.CUSTOM_TOKEN} and written out as is in the security header.
         * </ul>
         */
        @WithConverter(WsSecurityActionConverter.class)
        List<WsSecurityAction> actions();

        /**
         * The actor or role name of the <code>wsse:Security</code> header. If this parameter
         * is omitted, the actor name is not set.
         * <p>
         * The value of the actor or role has to match the receiver's setting
         * or may contain standard values.
         * <p>
         * This option is experimental, because it is <a href="https://github.com/quarkiverse/quarkus-cxf/issues/1052">not
         * covered by tests</a> yet.
         */
        @WssConfigurationConstant
        Optional<String> actor();

        /**
         * The user's name. It is used differently by each of the WS-Security functions.
         * <ul>
         * <li>The <i>UsernameToken</i> function sets this name in the
         * <code>UsernameToken</code>.
         * </li>
         * <li>The <i>Signing</i> function uses this name as the alias name
         * in the keystore to get user's certificate and private key to
         * perform signing if {@code signature-user} is not used.
         * </li>
         * <li>The <i>encryption</i>
         * functions uses this parameter as fallback if {@code encryption-user}
         * is not used.
         * </li>
         * </ul>
         */
        @WssConfigurationConstant
        Optional<String> user();

        /**
         * The user's name for encryption. The encryption functions use the public key of
         * this user's certificate to encrypt the generated symmetric key.
         * <p>
         * If this parameter is not set, then the encryption
         * function falls back to the {@code user} parameter to get the
         * certificate.
         * <p>
         * If <b>only</b> encryption of the SOAP body data is requested,
         * it is recommended to use this parameter to define the username.
         * <p>
         * This option is experimental, because it is <a href="https://github.com/quarkiverse/quarkus-cxf/issues/1052">not
         * covered by tests</a> yet.
         */
        @WssConfigurationConstant
        Optional<String> encryptionUser();

        /**
         * The user's name for signature. This name is used as the alias name in the keystore
         * to get user's certificate and private key to perform signing.
         * <p>
         * If this parameter is not set, then the signature
         * function falls back to the {@code user} parameter.
         * <p>
         * This option is experimental, because it is <a href="https://github.com/quarkiverse/quarkus-cxf/issues/1052">not
         * covered by tests</a> yet.
         */
        @WssConfigurationConstant
        Optional<String> signatureUser();

        /**
         * Specifying this name as {@code encryption-user} triggers a special action to get the public key to use for
         * encryption.
         * <p>
         * The handler uses the public key of the sender's certificate. Using this way to define an encryption key
         * simplifies certificate management to a large extent.
         * <p>
         * This option is experimental, because it is <a href="https://github.com/quarkiverse/quarkus-cxf/issues/1052">not
         * covered by tests</a> yet.
         */
        @WssConfigurationConstant
        Optional<String> useReqSigCert();

        /**
         * A {@code javax.security.auth.callback.CallbackHandler} implementation used to obtain passwords.
         * Can be one of the following:
         * <ul>
         * <li>A fully qualified class name implementing {@code javax.security.auth.callback.CallbackHandler} to look up
         * in the
         * CDI
         * container.
         * <li>A bean name prefixed with {@code #} that will be looked up in the CDI container; example:
         * {@code #myCallbackHandler}
         * </ul>
         * The callback function
         * {@code CallbackHandler.handle(Callback[])} gets an array of
         * {@code org.apache.wss4j.common.ext.WSPasswordCallback} objects. Only the first entry of the array is used.
         * This object contains the username/keyname as identifier. The callback handler must set the password or key
         * associated with this identifier before it returns.
         */
        @WssConfigurationConstant(transformer = Transformer.beanRef, key = "passwordCallbackRef")
        Optional<String> passwordCallback();

        /**
         * A {@code javax.security.auth.callback.CallbackHandler} implementation used to construct SAML Assertions.
         * Can be one of the following:
         * <ul>
         * <li>A fully qualified class name implementing {@code javax.security.auth.callback.CallbackHandler} to look up
         * in the
         * CDI
         * container.
         * <li>A bean name prefixed with {@code #} that will be looked up in the CDI container; example:
         * {@code #myCallbackHandler}
         * </ul>
         * <p>
         * This option is experimental, because it is <a href="https://github.com/quarkiverse/quarkus-cxf/issues/1052">not
         * covered by tests</a> yet.
         */
        @WssConfigurationConstant(transformer = Transformer.beanRef, key = "samlCallbackRef")
        Optional<String> samlCallback();

        /**
         * Signing configuration
         */
        @WssConfigurationConstant(transformer = Transformer.crypto)
        CryptoConfig signature();

        /**
         * Signature verification configuration
         */
        @WssConfigurationConstant(transformer = Transformer.crypto)
        CryptoConfig signatureVerification();

        /**
         * Decryption configuration
         */
        @WssConfigurationConstant(transformer = Transformer.crypto)
        CryptoConfig decryption();

        /**
         * Encryption configuration
         */
        @WssConfigurationConstant(transformer = Transformer.crypto)
        CryptoConfig encryption();

        /**
         * If {@code true}, the signatureConfirmation will be enabled; otherwise the signatureConfirmation will not be
         * enabled.
         * <p>
         * This option is experimental, because it is <a href="https://github.com/quarkiverse/quarkus-cxf/issues/1052">not
         * covered by tests</a> yet.
         */
        @WssConfigurationConstant
        @WithDefault("false")
        boolean enableSignatureConfirmation();

        /**
         * If {@code true}, the mustUnderstand flag will be set on an outbound message; otherwise the flag will not be
         * set.
         */
        @WssConfigurationConstant
        @WithDefault("true")
        boolean mustUnderstand();

        /**
         * If {@code true}, the compliance with the Basic Security Profile (BSP) 1.1 will be ensured; otherwise it will
         * not be ensured.
         */
        @WssConfigurationConstant
        @WithDefault("true")
        boolean isBSPCompliant();

        /**
         * If {@code true}, an InclusiveNamespaces PrefixList will be added as a CanonicalizationMethod child when
         * generating Signatures using WSConstants.C14N_EXCL_OMIT_COMMENTS; otherwise the PrefixList will not be added.
         * <p>
         * This option is experimental, because it is <a href="https://github.com/quarkiverse/quarkus-cxf/issues/1052">not
         * covered by tests</a> yet.
         */
        @WssConfigurationConstant
        @WithDefault("true")
        boolean addInclusivePrefixes();

        /**
         * If {@code true}, and the password type is of type {@code text} then a Nonce Element is added to a
         * UsernameToken; otherwise a Nonce Element is not be added. Note that a Nonce is automatically added if the
         * password type is {@code digest}.
         */
        @WssConfigurationConstant
        @WithDefault("false")
        boolean addUsernameTokenNonce();

        /**
         * If {@code true}, and the password type is of type {@code text} then a Created Element is added to a
         * UsernameToken; otherwise a Created Element is not be added. Note that a Created is automatically added if the
         * password type is {@code digest}.
         */
        @WssConfigurationConstant
        @WithDefault("false")
        boolean addUsernameTokenCreated();

        /**
         * If {@code true}, password types other than PasswordDigest or PasswordText are allowed when processing
         * UsernameTokens; otherwise other password types are not allowed.
         * <p>
         * This option is experimental, because it is <a href="https://github.com/quarkiverse/quarkus-cxf/issues/1052">not
         * covered by tests</a> yet.
         */
        @WssConfigurationConstant
        @WithDefault("false")
        boolean handleCustomPasswordTypes();

        /**
         * If {@code true}, a UsernameToken with no password element is allowed; otherwise a UsernameToken with no
         * password element is not allowed. Set it to {@code true} to allow deriving keys from UsernameTokens or to
         * support UsernameTokens for purposes other than authentication.
         * <p>
         * This option is experimental, because it is <a href="https://github.com/quarkiverse/quarkus-cxf/issues/1052">not
         * covered by tests</a> yet.
         */
        @WssConfigurationConstant
        @WithDefault("false")
        boolean allowUsernameTokenNoPassword();

        /**
         * If {@code true}, (wsse) namespace qualified password types are accepted when processing UsernameTokens;
         * otherwise they are not allowed.
         * <p>
         * This option is experimental, because it is <a href="https://github.com/quarkiverse/quarkus-cxf/issues/1052">not
         * covered by tests</a> yet.
         */
        @WssConfigurationConstant
        @WithDefault("false")
        boolean allowNamespaceQualifiedPasswordTypes();

        /**
         * If {@code true}, Certificate Revocation List (CRL) checking is enabled when verifying trust in a certificate;
         * otherwise it is not allowed.
         * <p>
         * This option is experimental, because it is <a href="https://github.com/quarkiverse/quarkus-cxf/issues/1052">not
         * covered by tests</a> yet.
         */
        @WssConfigurationConstant
        @WithDefault("false")
        boolean enableRevocation();

        /**
         * If {@code true}, a single certificate is used when constructing a BinarySecurityToken used for direct
         * reference in signature; otherwise a whole certificate chain is used.
         * <p>
         * This option is experimental, because it is <a href="https://github.com/quarkiverse/quarkus-cxf/issues/1052">not
         * covered by tests</a> yet.
         */
        @WssConfigurationConstant
        @WithDefault("true")
        boolean useSingleCertificate();

        /**
         * If {@code true}, the Username Token derived key for a MAC is used; otherwise the Username Token derived key
         * is not used.
         * <p>
         * This option is experimental, because it is <a href="https://github.com/quarkiverse/quarkus-cxf/issues/1052">not
         * covered by tests</a> yet.
         */
        @WssConfigurationConstant
        @WithDefault("true")
        boolean useDerivedKeyForMAC();

        /**
         * If {@code true}, Timestamps will have precision in milliseconds; otherwise Timestamps will have precision in
         * seconds.
         */
        @WssConfigurationConstant
        @WithDefault("true")
        boolean precisionInMilliseconds();

        /**
         * If {@code true}, an exception will be thrown if a Timestamp contains
         * an <code>Expires</code> element and the current time at the receiver is past the expires time; otherwise the
         * the <code>Expires</code> time won't be validated.
         */
        @WssConfigurationConstant
        @WithDefault("true")
        boolean timestampStrict();

        /**
         * If {@code true}, {@code Timestamp} elements must have {@code Expires} Element; otherwise {@code Timestamp}
         * elements do not need to have {@code Expires} Element.
         */
        @WssConfigurationConstant
        @WithDefault("false")
        boolean requireTimestampExpires();

        /**
         * If {@code true}, the symmetric key used for encryption is encrypted in turn,
         * and inserted into the security header in an "EncryptedKey" structure; otherwise no EncryptedKey structure
         * is constructed.
         * <p>
         * This option is experimental, because it is <a href="https://github.com/quarkiverse/quarkus-cxf/issues/1052">not
         * covered by tests</a> yet.
         */
        @WssConfigurationConstant
        @WithDefault("true")
        boolean encryptSymmetricEncryptionKey();

        /**
         * If {@code true}, the engine will enforce EncryptedData elements to be in a signed subtree of the document;
         * otherwise no such enforcement will happen. This can be used to prevent some wrapping based attacks when
         * encrypt-before-sign token protection is selected.
         * <p>
         * This option is experimental, because it is <a href="https://github.com/quarkiverse/quarkus-cxf/issues/1052">not
         * covered by tests</a> yet.
         */
        @WssConfigurationConstant
        @WithDefault("false")
        boolean requireSignedEncryptedDataElements();

        /**
         * If {@code true}, the use of the discouraged RSA v1.5 Key Transport Algorithm will be allowed; otherwise the
         * use of that algorithm Key Transport Algorithm will not be allowed.
         * <p>
         * This option is experimental, because it is <a href="https://github.com/quarkiverse/quarkus-cxf/issues/1052">not
         * covered by tests</a> yet.
         */
        @WssConfigurationConstant
        @WithDefault("false")
        boolean allowRSA15KeyTransportAlgorithm();

        /**
         * If {@code true}, the SubjectConfirmation requirements of a received SAML Token
         * (sender-vouches or holder-of-key) will be validated; otherwise they won't be validated.
         * <p>
         * This option is experimental, because it is <a href="https://github.com/quarkiverse/quarkus-cxf/issues/1052">not
         * covered by tests</a> yet.
         */
        @WssConfigurationConstant
        @WithDefault("true")
        boolean validateSamlSubjectConfirmation();

        /**
         * If {@code true}, the Signature Token will be included in the security header as well; otherwise it will not
         * be added. This is only applicable to the IssuerSerial, Thumbprint and SKI Key Identifier cases.
         * <p>
         * This option is experimental, because it is <a href="https://github.com/quarkiverse/quarkus-cxf/issues/1052">not
         * covered by tests</a> yet.
         */
        @WssConfigurationConstant
        @WithDefault("false")
        boolean includeSignatureToken();

        /**
         * If {@code true}, the Encryption token (BinarySecurityToken) will be included in the security header as well;
         * otherwise it won't be included. This is only applicable to the IssuerSerial, Thumbprint and SKI Key
         * Identifier cases.
         * <p>
         * This option is experimental, because it is <a href="https://github.com/quarkiverse/quarkus-cxf/issues/1052">not
         * covered by tests</a> yet.
         */
        @WssConfigurationConstant
        @WithDefault("false")
        boolean includeEncryptionToken();

        /**
         * If {@code true}, the "http://docs.oasis-open.org/ws-sx/ws-secureconversation/200512"
         * namespace will be used for SecureConversation + Derived Keys; otherwise it will use the namespace
         * "http://schemas.xmlsoap.org/ws/2005/02/sc".
         * <p>
         * This option is experimental, because it is <a href="https://github.com/quarkiverse/quarkus-cxf/issues/1052">not
         * covered by tests</a> yet.
         */
        @WssConfigurationConstant
        @WithDefault("true")
        boolean use200512Namespace();

        /**
         * If {@code true}, WSS4J attempts to get the secret key from the CallbackHandler otherwise the random key will
         * be generated internally. This allows the user more control over the symmetric key if required.
         * <p>
         * This option is experimental, because it is <a href="https://github.com/quarkiverse/quarkus-cxf/issues/1052">not
         * covered by tests</a> yet.
         */
        @WssConfigurationConstant
        @WithDefault("false")
        boolean getSecretKeyFromCallbackHandler();

        /**
         * If {@code true}, the bytes (CipherData or BinarySecurityToken) will be stored in an attachment; otherwise
         * the bytes are BASE-64 encoded and "inlined" in the message. Setting this to {@code true} is more
         * efficient, as it means that the BASE-64 encoding step can be skipped. For this to work, a
         * CallbackHandler must be set on RequestData that can handle attachments.
         * <p>
         * This option is experimental, because it is <a href="https://github.com/quarkiverse/quarkus-cxf/issues/1052">not
         * covered by tests</a> yet.
         */
        @WssConfigurationConstant
        @WithDefault("false")
        boolean storeBytesInAttachment();

        /**
         * If {@code true}, the xop:Include Elements will be searched for encryption and signature (on the outbound
         * side) or for signature verification (on the inbound side); otherwise the search won't happen. The default
         * is {@code false} on the outbound side and {@code true} on the inbound side. What this means on the inbound
         * side is that the relevant attachment bytes are BASE-64 encoded and inserted into the Element. This ensures
         * that the actual bytes are signed, and not just the reference.
         * <p>
         * This option is experimental, because it is <a href="https://github.com/quarkiverse/quarkus-cxf/issues/1052">not
         * covered by tests</a> yet.
         */
        @WssConfigurationConstant
        Optional<Boolean> expandXOPInclude();

        /**
         * Specific parameter for UsernameTokens to define the encoding of the password. It can
         * be used on either the outbound or inbound side. The valid values are:
         * <ul>
         * <li>{@code PasswordDigest}
         * <li>{@code PasswordText}
         * <li>{@code PasswordNone}
         * </ul>
         * On the Outbound side, the default value is {@code PasswordDigest}. There is no default value on
         * the inbound side. If a value is specified on the inbound side, the password type of
         * the received UsernameToken must match the specified type, or an exception will be
         * thrown.
         */
        @WssConfigurationConstant
        Optional<String> passwordType();

        /**
         * Defines which key identifier type to use for signature. The WS-Security specifications
         * recommends to use the identifier type <code>IssuerSerial</code>.
         *
         * For signature <code>IssuerSerial</code>, <code>DirectReference</code>,
         * <code>X509KeyIdentifier</code>, <code>Thumbprint</code>, <code>SKIKeyIdentifier</code>
         * and <code>KeyValue</code> are valid only.
         * <p>
         * This option is experimental, because it is <a href="https://github.com/quarkiverse/quarkus-cxf/issues/1052">not
         * covered by tests</a> yet.
         */
        @WithDefault("IssuerSerial")
        @WssConfigurationConstant
        String signatureKeyIdentifier();

        /**
         * Defines which signature algorithm to use. The default is set by the data in the
         * certificate, i.e. one of the following:
         * <ul>
         * <li>{@code http://www.w3.org/2000/09/xmldsig#rsa-sha1}
         * <li>{@code http://www.w3.org/2000/09/xmldsig#dsa-sha1}
         * </ul>
         * <p>
         * This option is experimental, because it is <a href="https://github.com/quarkiverse/quarkus-cxf/issues/1052">not
         * covered by tests</a> yet.
         */
        @WssConfigurationConstant
        Optional<String> signatureAlgorithm();

        /**
         * Defines which signature digest algorithm to use.
         * <p>
         * This option is experimental, because it is <a href="https://github.com/quarkiverse/quarkus-cxf/issues/1052">not
         * covered by tests</a> yet.
         */
        @WssConfigurationConstant
        @WithDefault("http://www.w3.org/2000/09/xmldsig#sha1")
        String signatureDigestAlgorithm();

        /**
         * Defines which signature c14n (canonicalization) algorithm to use.
         * <p>
         * This option is experimental, because it is <a href="https://github.com/quarkiverse/quarkus-cxf/issues/1052">not
         * covered by tests</a> yet.
         */
        @WssConfigurationConstant
        @WithDefault("http://www.w3.org/2001/10/xml-exc-c14n#")
        Optional<String> signatureC14nAlgorithm();

        /**
         * Parameter to define which parts of the request shall be signed.
         * <p>
         * Refer to {@code encryption-parts} for a detailed description of
         * the format of the value string.
         * <p>
         * If this parameter is not specified the handler signs the SOAP Body
         * by default, i.e.:
         *
         * <pre>
         * {}{http://schemas.xmlsoap.org/soap/envelope/}Body
         * </pre>
         *
         * To specify an element without a namespace use the string
         * <code>Null</code> as the namespace name (this is a case sensitive
         * string)
         * <p>
         * If there is no other element in the request with a local name of
         * <code>Body</code> then the SOAP namespace identifier can be empty
         * (<code>{}</code>).
         * <p>
         * This option is experimental, because it is <a href="https://github.com/quarkiverse/quarkus-cxf/issues/1052">not
         * covered by tests</a> yet.
         */
        @WssConfigurationConstant
        Optional<String> signatureParts();

        /**
         * Parameter to define which parts of the request shall be signed, if they
         * exist in the request. If they do not, then no error is thrown. This contrasts
         * with the {@code signature-parts} Identifier, which specifies elements that must be
         * signed in the request.
         * <p>
         * Refer to {@code encryption-parts} for a detailed description of
         * the format of the value string.
         * <p>
         * This option is experimental, because it is <a href="https://github.com/quarkiverse/quarkus-cxf/issues/1052">not
         * covered by tests</a> yet.
         */
        @WssConfigurationConstant
        Optional<String> optionalSignatureParts();

        /**
         * This parameter sets the number of iterations to use when deriving a key
         * from a Username Token.
         * <p>
         * This option is experimental, because it is <a href="https://github.com/quarkiverse/quarkus-cxf/issues/1052">not
         * covered by tests</a> yet.
         */
        @WssConfigurationConstant
        @WithDefault("100")
        int derivedKeyIterations();

        /**
         * Defines which key identifier type to use for encryption. The WS-Security specifications
         * recommends to use the identifier type <code>IssuerSerial</code>. For encryption
         * <code>IssuerSerial</code>, <code>DirectReference</code>, <code>X509KeyIdentifier</code>,
         * <code>Thumbprint</code>, <code>SKIKeyIdentifier</code>, <code>EncryptedKeySHA1</code>
         * and <code>EmbeddedKeyName</code> are valid only.
         * <p>
         * This option is experimental, because it is <a href="https://github.com/quarkiverse/quarkus-cxf/issues/1052">not
         * covered by tests</a> yet.
         */
        @WssConfigurationConstant
        @WithDefault("IssuerSerial")
        String encryptionKeyIdentifier();

        /**
         * Defines which symmetric encryption algorithm to use. WSS4J supports the
         * following algorithms:
         * <ul>
         * <li>{@code http://www.w3.org/2001/04/xmlenc#tripledes-cbc}
         * <li>{@code http://www.w3.org/2001/04/xmlenc#aes128-cbc}
         * <li>{@code http://www.w3.org/2001/04/xmlenc#aes256-cbc}
         * <li>{@code http://www.w3.org/2001/04/xmlenc#aes192-cbc}
         * </ul>
         * Except for AES 192 all of these algorithms are required by the XML Encryption
         * specification.
         * <p>
         * This option is experimental, because it is <a href="https://github.com/quarkiverse/quarkus-cxf/issues/1052">not
         * covered by tests</a> yet.
         */
        @WssConfigurationConstant
        @WithDefault("http://www.w3.org/2001/04/xmlenc#aes128-cbc")
        String encryptionSymAlgorithm();

        /**
         * Defines which algorithm to use to encrypt the generated symmetric key.
         * <p>
         * This option is experimental, because it is <a href="https://github.com/quarkiverse/quarkus-cxf/issues/1052">not
         * covered by tests</a> yet.
         */
        @WssConfigurationConstant
        @WithDefault("http://www.w3.org/2001/04/xmlenc#rsa-oaep-mgf1p")
        String encryptionKeyTransportAlgorithm();

        /**
         * Parameter to define which parts of the request shall be encrypted.
         * <p>
         * The value of this parameter is a list of semi-colon separated
         * element names that identify the elements to encrypt. An encryption mode
         * specifier and a namespace identification, each inside a pair of curly
         * brackets, may preceed each element name.
         * <p>
         * The encryption mode specifier is either <code>{Content}</code> or
         * <code>{Element}</code>. Please refer to the W3C XML Encryption
         * specification about the differences between Element and Content
         * encryption. The encryption mode defaults to <code>Content</code>
         * if it is omitted. An example:
         *
         * <pre>
         * {Content}{http://example.org/paymentv2}CreditCard;{Element}{}UserName
         * </pre>
         *
         * The the first entry of the list identifies the element
         * <code>CreditCard</code> in the namespace
         * <code>http://example.org/paymentv2</code>, and will encrypt its content.
         * Be aware that the element name, the namespace identifier, and the
         * encryption modifier are case sensitive.
         * <p>
         * The encryption modifier and the namespace identifier can be ommited.
         * In this case the encryption mode defaults to <code>Content</code> and
         * the namespace is set to the SOAP namespace.
         * <p>
         * An empty encryption mode defaults to <code>Content</code>, an empty
         * namespace identifier defaults to the SOAP namespace.
         * The second line of the example defines <code>Element</code> as
         * encryption mode for an <code>UserName</code> element in the SOAP
         * namespace.
         * <p>
         * Note that the special value "{}cid:Attachments;" means that all of the message
         * attachments should be encrypted.
         * <p>
         * To specify an element without a namespace use the string
         * <code>Null</code> as the namespace name (this is a case sensitive
         * string)
         * <p>
         * If no list is specified, the handler encrypts the SOAP Body in
         * <code>Content</code> mode by default.
         * <p>
         * This option is experimental, because it is <a href="https://github.com/quarkiverse/quarkus-cxf/issues/1052">not
         * covered by tests</a> yet.
         */
        @WssConfigurationConstant
        Optional<String> encryptionParts();

        /**
         * Parameter to define which parts of the request shall be encrypted, if they
         * exist in the request. If they do not, then no error is thrown. This contrasts
         * with the {@code encryption-parts} Identifier, which specifies elements that must be
         * encrypted in the request.
         * <p>
         * Refer to {@code encryption-parts} for a detailed description of
         * the format of the value string.
         * <p>
         * This option is experimental, because it is <a href="https://github.com/quarkiverse/quarkus-cxf/issues/1052">not
         * covered by tests</a> yet.
         */
        @WssConfigurationConstant
        Optional<String> optionalEncryptionParts();

        /**
         * Defines which encryption digest algorithm to use with the RSA OAEP Key Transport
         * algorithm for encryption.
         * <p>
         * This option is experimental, because it is <a href="https://github.com/quarkiverse/quarkus-cxf/issues/1052">not
         * covered by tests</a> yet.
         */
        @WssConfigurationConstant
        @WithDefault("SHA-1")
        String encryptionDigestAlgorithm();

        /**
         * Defines which encryption mgf algorithm to use with the RSA OAEP Key Transport
         * algorithm for encryption.
         * <p>
         * This option is experimental, because it is <a href="https://github.com/quarkiverse/quarkus-cxf/issues/1052">not
         * covered by tests</a> yet.
         */
        @WssConfigurationConstant
        @WithDefault("mgfsha1")
        String encryptionMGFAlgorithm();

        /**
         * Time-To-Live is the time difference between creation and expiry time in
         * seconds of the UsernameToken Created value. After this time the SOAP request
         * is invalid (at least the security data shall be treated this way).
         * <p>
         * If this parameter is not defined, contains a value less or equal
         * zero, or an illegal format the handlers use a default TTL of
         * 300 seconds (5 minutes).
         */
        @WssConfigurationConstant
        Optional<Integer> utTimeToLive();

        /**
         * This configuration tag specifies the time in seconds in the future within which
         * the Created time of an incoming UsernameToken is valid. The default value is "60",
         * to avoid problems where clocks are slightly askew. To reject all future-created
         * UsernameTokens, set this value to "0".
         */
        @WssConfigurationConstant
        Optional<Integer> utFutureTimeToLive();

        /**
         * A comma separated list of regular expressions which will be applied to the subject DN of the
         * certificate used for signature validation, after trust verification of the certificate chain associated with
         * the certificate.
         * <p>
         * This option is experimental, because it is <a href="https://github.com/quarkiverse/quarkus-cxf/issues/1052">not
         * covered by tests</a> yet.
         */
        @WssConfigurationConstant
        Optional<List<String>> sigSubjectCertConstraints();

        /**
         * A comma separated list of regular expressions which will be applied to the issuer DN of the certificate used
         * for signature validation, after trust verification of the certificate chain associated with the certificate.
         * <p>
         * This option is experimental, because it is <a href="https://github.com/quarkiverse/quarkus-cxf/issues/1052">not
         * covered by tests</a> yet.
         */
        @WssConfigurationConstant
        Optional<List<String>> sigIssuerCertConstraints();

        /**
         * Time-To-Live is the time difference between creation and expiry time in
         * seconds in the WSS Timestamp. After this time the SOAP request is
         * invalid (at least the security data shall be treated this way).
         * <p>
         * If this parameter is not defined, contains a value less or equal
         * zero, or an illegal format the handlers use a default TTL of
         * 300 seconds (5 minutes).
         */
        @WssConfigurationConstant
        Optional<Integer> timeToLive();

        /**
         * This configuration tag specifies the time in seconds in the future within which
         * the Created time of an incoming Timestamp is valid. The default value is "60",
         * to avoid problems where clocks are slightly askew. To reject all future-created
         * Timestamps, set this value to "0".
         */
        @WssConfigurationConstant
        Optional<Integer> futureTimeToLive();

        /**
         * A reference to a Map from {@code QName}s to Validators be used to validate tokens identified by their QName.
         * For the DOM layer, the Validators have to implement {@code org.apache.wss4j.dom.validate.Validator}
         * For the StAX layer, the Validators have to implement {@code org.apache.wss4j.stax.validate.Validator}.
         * <p>
         * Can be one of the following:
         * <ul>
         * <li>A fully qualified class name implementing {@code java.util.Map} to look up in the CDI
         * container.
         * <li>A bean name prefixed with {@code #} that will be looked up in the CDI container; example:
         * {@code #myValidatorMap}
         * </ul>
         * <p>
         * This option is experimental, because it is <a href="https://github.com/quarkiverse/quarkus-cxf/issues/1052">not
         * covered by tests</a> yet.
         */
        @WssConfigurationConstant(transformer = Transformer.beanRef)
        Optional<String> validatorMap();
        //
        //        /**
        //         * A reference to a {@code org.apache.wss4j.common.cache.ReplayCache} instance used to cache UsernameToken
        //         * nonces. A {@code org.apache.wss4j.common.cache.EHCacheReplayCache} is used by default.
        //         * <p>
        //         * Can be one of the following:
        //         * <ul>
        //         * <li>A fully qualified class name implementing {@code org.apache.wss4j.common.cache.ReplayCache} to look up in
        //         * the CDI container.
        //         * <li>A bean name prefixed with {@code #} that will be looked up in the CDI container; example:
        //         * {@code #myReplayCache}
        //         * </ul>
        //         */
        //        @WssConfigurationConstant(transformer = Transformer.beanRef)
        //        Optional<String> nonceCacheInstance();
        //
        //        /**
        //         * A reference to a {@code org.apache.wss4j.common.cache.ReplayCache} instance used to cache Timestamp Created
        //         * Strings. A {@code org.apache.wss4j.common.cache.EHCacheReplayCache} is used by default.
        //         * <p>
        //         * Can be one of the following:
        //         * <ul>
        //         * <li>A fully qualified class name implementing {@code org.apache.wss4j.common.cache.ReplayCache} to look up in
        //         * the CDI container.
        //         * <li>A bean name prefixed with {@code #} that will be looked up in the CDI container; example:
        //         * {@code #myReplayCache}
        //         * </ul>
        //         */
        //        @WssConfigurationConstant(transformer = Transformer.beanRef)
        //        Optional<String> timestampCacheInstance();
        //
        //        /**
        //         * A reference to a {@code org.apache.wss4j.common.cache.ReplayCache} instance used to cache SAML2
        //         * Token Identifier Strings (if the token contains a OneTimeUse Condition).
        //         * A {@code org.apache.wss4j.common.cache.EHCacheReplayCache} is used by default.
        //         * <p>
        //         * Can be one of the following:
        //         * <ul>
        //         * <li>A fully qualified class name implementing {@code org.apache.wss4j.common.cache.ReplayCache} to look up in
        //         * the CDI container.
        //         * <li>A bean name prefixed with {@code #} that will be looked up in the CDI container; example:
        //         * {@code #myReplayCache}
        //         * </ul>
        //         */
        //        @WssConfigurationConstant(transformer = Transformer.beanRef)
        //        Optional<String> samlOneTimeUseCacheInstance();

        /**
         * A reference to a {@code org.apache.wss4j.common.crypto.PasswordEncryptor} bean, which is used to encrypt or
         * decrypt passwords in the Merlin Crypto implementation (or any custom Crypto implementations).
         * <p>
         * By default, WSS4J uses the {@code org.apache.wss4j.common.crypto.JasyptPasswordEncryptor} which must be
         * instantiated with a password to use to decrypt keystore passwords in the Merlin Crypto definition.
         * This password is obtained via the CallbackHandler defined via {@code password-callback}
         * <p>
         * The encrypted passwords must be stored in the format "ENC(encoded encrypted password)".
         * <p>
         * Can be one of the following:
         * <ul>
         * <li>A fully qualified class name implementing {@code org.apache.wss4j.common.cache.ReplayCache} to look up in
         * the CDI container.
         * <li>A bean name prefixed with {@code #} that will be looked up in the CDI container; example:
         * {@code #myReplayCache}
         * </ul>
         * <p>
         * This option is experimental, because it is <a href="https://github.com/quarkiverse/quarkus-cxf/issues/1052">not
         * covered by tests</a> yet.
         */
        @WssConfigurationConstant(transformer = Transformer.beanRef)
        Optional<String> passwordEncryptorInstance();

        /**
         * Controls the deriving token from which DerivedKeyTokens derive keys from.
         * Possible values:
         * <ul>
         * <li>{@code DirectReference} - A reference to a BinarySecurityToken
         * <li>{@code EncryptedKey} - A reference to an EncryptedKey
         * <li>{@code SecurityContextToken} - A reference to a SecurityContextToken
         * </ul>
         * <p>
         * This option is experimental, because it is <a href="https://github.com/quarkiverse/quarkus-cxf/issues/1052">not
         * covered by tests</a> yet.
         */
        @WssConfigurationConstant
        Optional<String> derivedTokenReference();

        /**
         * Controls the key identifier of Derived Tokens, i.e. how they reference the deriving key.
         * <p>
         * This option is experimental, because it is <a href="https://github.com/quarkiverse/quarkus-cxf/issues/1052">not
         * covered by tests</a> yet.
         */
        @WssConfigurationConstant
        Optional<String> derivedTokenKeyIdentifier();

        /**
         * The length to use (in bytes) when deriving a key for Signature. If not specified, defaults to a value based
         * on the signature algorithm.
         * <p>
         * This option is experimental, because it is <a href="https://github.com/quarkiverse/quarkus-cxf/issues/1052">not
         * covered by tests</a> yet.
         */
        @WssConfigurationConstant
        Optional<Integer> derivedSignatureKeyLength();

        /**
         * The length to use (in bytes) when deriving a key for Encryption. If not specified, defaults to a value based
         * on the encryption algorithm.
         * <p>
         * This option is experimental, because it is <a href="https://github.com/quarkiverse/quarkus-cxf/issues/1052">not
         * covered by tests</a> yet.
         */
        @WssConfigurationConstant
        Optional<Integer> derivedEncryptionKeyLength();
    }

    @ConfigGroup
    interface CryptoConfig {
        String MERLIN_PROVIDER = "org.apache.wss4j.common.crypto.Merlin";

        /**
         * WSS4J specific provider used to create Crypto instances
         * <p>
         * This option is experimental, because it is <a href="https://github.com/quarkiverse/quarkus-cxf/issues/1052">not
         * covered by tests</a> yet.
         */
        @WithDefault(MERLIN_PROVIDER)
        String provider();

        /**
         * Configuration of Merlin WSS4J Crypto provider
         */
        MerlinConfig merlin();

        /**
         * Free-form properties to pass to a Crypto implementation other than Merlin.
         * <p>
         * Example
         *
         * <pre>
         * [prefix].provider = org.acme.MyProvider
         * [prefix].property."org.acme.key1" = value1
         * [prefix].property."org.acme.key2" = value2
         * </pre>
         * <p>
         * This option is experimental, because it is <a href="https://github.com/quarkiverse/quarkus-cxf/issues/1052">not
         * covered by tests</a> yet.
         */
        @WithName("property")
        Map<String, String> properties();

    }

    @ConfigGroup
    interface MerlinConfig {

        /**
         * The location of an (X509) CRL file to use.
         * <p>
         * This option is experimental, because it is <a href="https://github.com/quarkiverse/quarkus-cxf/issues/1052">not
         * covered by tests</a> yet.
         */
        @WssConfigurationConstant(key = "x509crl.file")
        Optional<String> x509crlFile();

        /**
         * The provider used to load keystores. Defaults to installed provider.
         * <p>
         * This option is experimental, because it is <a href="https://github.com/quarkiverse/quarkus-cxf/issues/1052">not
         * covered by tests</a> yet.
         */
        @WssConfigurationConstant(key = "keystore.provider")
        Optional<String> keystoreProvider();

        /**
         * The provider used to load certificates. Defaults to keystore provider.
         * <p>
         * This option is experimental, because it is <a href="https://github.com/quarkiverse/quarkus-cxf/issues/1052">not
         * covered by tests</a> yet.
         */
        @WssConfigurationConstant(key = "cert.provider")
        Optional<String> certProvider();

        /**
         * The location of the keystore
         * <p>
         * This option is experimental, because it is <a href="https://github.com/quarkiverse/quarkus-cxf/issues/1052">not
         * covered by tests</a> yet.
         */
        @WssConfigurationConstant(key = "keystore.file")
        Optional<String> keystoreFile();

        /**
         * The password used to load the keystore. Default value is "security".
         * <p>
         * This option is experimental, because it is <a href="https://github.com/quarkiverse/quarkus-cxf/issues/1052">not
         * covered by tests</a> yet.
         */
        @WssConfigurationConstant(key = "keystore.password")
        Optional<String> keystorePassword();

        /**
         * Type of keystore. Defaults to: java.security.KeyStore.getDefaultType())
         * <p>
         * This option is experimental, because it is <a href="https://github.com/quarkiverse/quarkus-cxf/issues/1052">not
         * covered by tests</a> yet.
         */
        @WssConfigurationConstant(key = "keystore.type")
        Optional<String> keystoreType();

        /**
         * The default keystore alias to use, if none is specified.
         * <p>
         * This option is experimental, because it is <a href="https://github.com/quarkiverse/quarkus-cxf/issues/1052">not
         * covered by tests</a> yet.
         */
        @WssConfigurationConstant(key = "keystore.alias")
        Optional<String> keystoreAlias();

        /**
         * The default password used to load the private key.
         * <p>
         * This option is experimental, because it is <a href="https://github.com/quarkiverse/quarkus-cxf/issues/1052">not
         * covered by tests</a> yet.
         */
        @WssConfigurationConstant(key = "keystore.private.password")
        Optional<String> keystorePrivatePassword();

        /**
         * Whether to enable caching when loading private keys or not. There is a significant performance gain for
         * PKCS12 keys
         * when caching is enabled.
         * <p>
         * This option is experimental, because it is <a href="https://github.com/quarkiverse/quarkus-cxf/issues/1052">not
         * covered by tests</a> yet.
         */
        @WssConfigurationConstant(key = "keystore.private.caching")
        @WithDefault("true")
        boolean keystorePrivateCaching();

        /**
         * Whether or not to load the CA certs in <code>${java.home}/lib/security/cacerts</code>
         * <p>
         * This option is experimental, because it is <a href="https://github.com/quarkiverse/quarkus-cxf/issues/1052">not
         * covered by tests</a> yet.
         */
        @WssConfigurationConstant(key = "load.cacerts")
        @WithDefault("false")
        boolean loadCacerts();

        /**
         * The location of the truststore.
         * <p>
         * This option is experimental, because it is <a href="https://github.com/quarkiverse/quarkus-cxf/issues/1052">not
         * covered by tests</a> yet.
         */
        @WssConfigurationConstant(key = "truststore.file")
        Optional<String> truststoreFile();

        /**
         * The truststore password. Defaults to "changeit".
         * <p>
         * This option is experimental, because it is <a href="https://github.com/quarkiverse/quarkus-cxf/issues/1052">not
         * covered by tests</a> yet.
         */
        @WssConfigurationConstant(key = "truststore.password")
        Optional<String> truststorePassword();

        /**
         * The truststore type. Defaults to: java.security.KeyStore.getDefaultType().
         * <p>
         * This option is experimental, because it is <a href="https://github.com/quarkiverse/quarkus-cxf/issues/1052">not
         * covered by tests</a> yet.
         */
        @WssConfigurationConstant(key = "truststore.type")
        Optional<String> truststoreType();

        /**
         * WSS4J 2.1.5 The provider used to load truststores. By default it’s the same as the keystore provider. Set to
         * an empty
         * value to force use of the JRE’s default provider.
         * <p>
         * This option is experimental, because it is <a href="https://github.com/quarkiverse/quarkus-cxf/issues/1052">not
         * covered by tests</a> yet.
         *
         */
        @WssConfigurationConstant(key = "truststore.provider")
        Optional<String> truststoreProvider();

    }

    public enum WsSecurityAction {
        @ConfigDocEnumValue("UsernameToken")
        UsernameToken,
        @ConfigDocEnumValue("UsernameTokenSignature")
        UsernameTokenSignature,
        @ConfigDocEnumValue("UsernameTokenNoPassword")
        UsernameTokenNoPassword,
        @ConfigDocEnumValue("SAMLTokenUnsigned")
        SAMLTokenUnsigned,
        @ConfigDocEnumValue("SAMLTokenSigned")
        SAMLTokenSigned,
        @ConfigDocEnumValue("Signature")
        Signature,
        @ConfigDocEnumValue("Encryption")
        Encryption,
        @ConfigDocEnumValue("Timestamp")
        Timestamp,
        @ConfigDocEnumValue("SignatureDerived")
        SignatureDerived,
        @ConfigDocEnumValue("EncryptionDerived")
        EncryptionDerived,
        @ConfigDocEnumValue("SignatureWithKerberosToken")
        SignatureWithKerberosToken,
        @ConfigDocEnumValue("EncryptionWithKerberosToken")
        EncryptionWithKerberosToken,
        @ConfigDocEnumValue("KerberosToken")
        KerberosToken,
        @ConfigDocEnumValue("CustomToken")
        CustomToken
    }

}
