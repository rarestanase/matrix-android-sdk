/*
 * Copyright 2016 OpenMarket Ltd
 * Copyright 2018 New Vector Ltd
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

package org.matrix.androidsdk;

import android.net.Uri;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.matrix.androidsdk.rest.model.login.Credentials;
import org.matrix.androidsdk.ssl.Fingerprint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import okhttp3.CipherSuite;
import okhttp3.TlsVersion;

/**
 * Represents how to connect to a specific Homeserver, may include credentials to use.
 */
public class HomeServerConnectionConfig {
    private static final String CERTIFICATE_PINS_JSON_KEY = "certificate_pins";


    // the home server URI
    private Uri mHsUri;
    // the identity server URI
    private Uri mIdentityServerUri;
    // the credentials
    private Credentials mCredentials;
    private final List<CertificatePin> mCertificatePins = new ArrayList<>();

    // the anti-virus server URI
    private Uri mAntiVirusServerUri;
    // allowed fingerprints
    private List<Fingerprint> mAllowedFingerprints = Collections.emptyList();

    // tell whether we should reject X509 certs that were issued by trusts CAs and only trustcerts with matching fingerprints.
    private boolean mPin = false;
    // the accepted TLS versions
    private List<TlsVersion> mTlsVersions;
    // the accepted TLS cipher suites
    private List<CipherSuite> mTlsCipherSuites;
    // should accept TLS extensions
    private boolean mShouldAcceptTlsExtensions;
    // allow Http connection
    private boolean mAllowHttpExtension;

    /**
     * @param hsUri The URI to use to connect to the homeserver
     */
    public HomeServerConnectionConfig(Uri hsUri) {
        this(hsUri, null);
    }

    /**
     * @param hsUri       The URI to use to connect to the homeserver
     * @param credentials The credentials to use, if needed. Can be null.
     */
    public HomeServerConnectionConfig(Uri hsUri, Credentials credentials) {
        this(hsUri, null, credentials, Collections.EMPTY_LIST);
    }

    /**
     * @param hsUri The URI to use to connect to the homeserver
     * @param identityServerUri The URI to use to manage identity
     * @param credentials The credentials to use, if needed. Can be null.
     * @param certificatePins See https://square.github.io/okhttp/3.x/okhttp/okhttp3/CertificatePinner.html.
     */
    public HomeServerConnectionConfig(Uri hsUri, Uri identityServerUri, Credentials credentials, List<CertificatePin> certificatePins) {
        if (hsUri == null || (!"http".equals(hsUri.getScheme()) && !"https".equals(hsUri.getScheme()))) {
            throw new RuntimeException("Invalid home server URI: " + hsUri);
        }

        if ((null != identityServerUri) && (!"http".equals(hsUri.getScheme()) && !"https".equals(hsUri.getScheme()))) {
            throw new RuntimeException("Invalid identity server URI: " + identityServerUri);
        }

        // remove trailing /
        if (hsUri.toString().endsWith("/")) {
            try {
                String url = hsUri.toString();
                hsUri = Uri.parse(url.substring(0, url.length() - 1));
            } catch (Exception e) {
                throw new RuntimeException("Invalid home server URI: " + hsUri);
            }
        }

        // remove trailing /
        if ((null != identityServerUri) && identityServerUri.toString().endsWith("/")) {
            try {
                String url = identityServerUri.toString();
                identityServerUri = Uri.parse(url.substring(0, url.length() - 1));
            } catch (Exception e) {
                throw new RuntimeException("Invalid identity server URI: " + identityServerUri);
            }
        }

        mHsUri = hsUri;
        mIdentityServerUri = identityServerUri;
        mAntiVirusServerUri = null;

        this.mCredentials = credentials;
        if (certificatePins != null) {
            this.mCertificatePins.addAll(certificatePins);
        }
        mCredentials = credentials;

        mShouldAcceptTlsExtensions = true;
    }

    /**
     * Update the home server URI.
     *
     * @param uri the new HS uri
     */
    public void setHomeserverUri(Uri uri) {
        mHsUri = uri;
    }

    /**
     * @return the home server uri
     */
    public Uri getHomeserverUri() {
        return mHsUri;
    }

    /**
     * Update the identity server uri.
     *
     * @param uri the new identity server uri
     */
    public void setIdentityServerUri(Uri uri) {
        mIdentityServerUri = uri;
    }

    public List<CertificatePin> getCertificatePins() {
        return mCertificatePins;
    }
    /**
     * @return the identity server uri
     */
    public Uri getIdentityServerUri() {
        if (null != mIdentityServerUri) {
            return mIdentityServerUri;
        }
        // Else consider the HS uri by default.
        return mHsUri;
    }

    /**
     * Update the anti-virus server URI.
     *
     * @param uri the new anti-virus uri
     */
    public void setAntiVirusServerUri(Uri uri) {
        mAntiVirusServerUri = uri;
    }

    /**
     * @return the anti-virus server uri
     */
    public Uri getAntiVirusServerUri() {
        if (null != mAntiVirusServerUri) {
            return mAntiVirusServerUri;
        }
        // Else consider the HS uri by default.
        return mHsUri;
    }

    /**
     * @return the credentials
     */
    public Credentials getCredentials() {
        return mCredentials;
    }

    /**
     * Update the credentials.
     *
     * @param credentials the new credentials
     */
    public void setCredentials(Credentials credentials) {
        mCredentials = credentials;
    }

    /**
     * @return whether we should reject X509 certs that were issued by trusts CAs and only trust
     *         certs with matching fingerprints.
     */
    public boolean shouldPin() {
        return mPin;
    }

    /**
     * Update the set of TLS versions accepted for TLS connections with the home server.
     *
     * @param tlsVersions the set of TLS versions accepted.
     */
    public void setAcceptedTlsVersions(@Nullable List<TlsVersion> tlsVersions) {
        if (tlsVersions == null) {
            mTlsVersions = null;
        } else {
            mTlsVersions = Collections.unmodifiableList(tlsVersions);
        }
    }

    /**
     * TLS versions accepted for TLS connections with the home server.
     */
    @Nullable
    public List<TlsVersion> getAcceptedTlsVersions() {
        return mTlsVersions;
    }

    /**
     * Update the set of TLS cipher suites accepted for TLS connections with the home server.
     *
     * @param tlsCipherSuites the set of TLS cipher suites accepted.
     */
    public void setAcceptedTlsCipherSuites(@Nullable List<CipherSuite> tlsCipherSuites) {
        if (tlsCipherSuites == null) {
            mTlsCipherSuites = null;
        } else {
            mTlsCipherSuites = Collections.unmodifiableList(tlsCipherSuites);
        }
    }

    /**
     * TLS cipher suites accepted for TLS connections with the home server.
     */
    @Nullable
    public List<CipherSuite> getAcceptedTlsCipherSuites() {
        return mTlsCipherSuites;
    }

    /**
     * @param shouldAcceptTlsExtensions if true TLS extensions will be accepted for TLS
     *                                  connections with the home server.
     */
    public void setShouldAcceptTlsExtensions(boolean shouldAcceptTlsExtensions) {
        mShouldAcceptTlsExtensions = shouldAcceptTlsExtensions;
    }

    /**
     * @return whether we should accept TLS extensions.
     */
    public boolean shouldAcceptTlsExtensions() {
        return mShouldAcceptTlsExtensions;
    }

    /**
     * For test only: allow Http connection
     */
    @VisibleForTesting()
    public void allowHttpConnection() {
        mAllowHttpExtension = true;
    }

    /**
     * @return true if Http connection is allowed (false by default).
     */
    public boolean isHttpConnectionAllowed() {
        return mAllowHttpExtension;
    }

    @Override
    public String toString() {
        return "HomeserverConnectionConfig{" +
                "mHsUri=" + mHsUri +
                "mIdentityServerUri=" + mIdentityServerUri +
                ", mIdentityServerUri=" + mIdentityServerUri +
                ", mCredentials=" + mCredentials +
                ", certificatePins= " + mCertificatePins +
                ", mAntiVirusServerUri=" + mAntiVirusServerUri +
                ", mAllowedFingerprints size=" + mAllowedFingerprints.size() +
                ", mCredentials=" + mCredentials +
                ", mPin=" + mPin +
                ", mShouldAcceptTlsExtensions=" + mShouldAcceptTlsExtensions +
                ", mTlsVersions=" + (null == mTlsVersions ? "" : mTlsVersions.size()) +
                ", mTlsCipherSuitess=" + (null == mTlsCipherSuites ? "" : mTlsCipherSuites.size()) +
                '}';
    }

    /**
     * Convert the object instance into a JSon object
     *
     * @return the JSon representation
     * @throws JSONException the JSON conversion failure reason
     */
    public JSONObject toJson() throws JSONException {
        JSONObject json = new JSONObject();

        json.put("home_server_url", mHsUri.toString());
        json.put("identity_server_url", getIdentityServerUri().toString());
        if (mAntiVirusServerUri != null) {
            json.put("antivirus_server_url", mAntiVirusServerUri.toString());
        }

        if (mCredentials != null) json.put("credentials", mCredentials.toJson());
        List<JSONObject> jsonCertificatePins = new ArrayList<>();
        for (CertificatePin certificatePin : mCertificatePins) {
            jsonCertificatePins.add(certificatePin.toJson());
        }
        if (mAllowedFingerprints != null) {
            List<JSONObject> fingerprints = new ArrayList<>(mAllowedFingerprints.size());

            for (Fingerprint fingerprint : mAllowedFingerprints) {
                fingerprints.add(fingerprint.toJson());
            }

            json.put("fingerprints", new JSONArray(fingerprints));
        }
        json.put(CERTIFICATE_PINS_JSON_KEY, new JSONArray(jsonCertificatePins));

        json.put("tls_extensions", mShouldAcceptTlsExtensions);

        if (mTlsVersions != null) {
            List<String> tlsVersions = new ArrayList<>(mTlsVersions.size());

            for (TlsVersion tlsVersion : mTlsVersions) {
                tlsVersions.add(tlsVersion.javaName());
            }

            json.put("tls_versions", new JSONArray(tlsVersions));
        }

        if (mTlsCipherSuites != null) {
            List<String> tlsCipherSuites = new ArrayList<>(mTlsCipherSuites.size());

            for (CipherSuite tlsCipherSuite : mTlsCipherSuites) {
                tlsCipherSuites.add(tlsCipherSuite.javaName());
            }

            json.put("tls_cipher_suites", new JSONArray(tlsCipherSuites));
        }

        return json;
    }

    /**
     * Create an object instance from the json object.
     *
     * @param obj the json object
     * @return a HomeServerConnectionConfig instance
     * @throws JSONException the conversion failure reason
     */
    public static HomeServerConnectionConfig fromJson(JSONObject obj) throws JSONException {
        List<CertificatePin> certificatePins = new ArrayList<>();
        if (obj.has(CERTIFICATE_PINS_JSON_KEY)) {
            JSONArray jsonCertificatePins = obj.getJSONArray(CERTIFICATE_PINS_JSON_KEY);
            for (int i = 0; i < jsonCertificatePins.length(); i++) {
                certificatePins.add(CertificatePin.fromJson(jsonCertificatePins.getJSONObject(i)));
            }
        }

        JSONArray fingerprintArray = obj.optJSONArray("fingerprints");
        List<Fingerprint> fingerprints = new ArrayList<>();
        if (fingerprintArray != null) {
            for (int i = 0; i < fingerprintArray.length(); i++) {
                fingerprints.add(Fingerprint.fromJson(fingerprintArray.getJSONObject(i)));
            }
        }
        JSONObject credentialsObj = obj.optJSONObject("credentials");
        Credentials creds = credentialsObj != null ? Credentials.fromJson(credentialsObj) : null;
        HomeServerConnectionConfig config = new HomeServerConnectionConfig(
            Uri.parse(obj.getString("home_server_url")),
            obj.has("identity_server_url") ? Uri.parse(obj.getString("identity_server_url")) : null,
            creds,
            certificatePins
        );

        // Set the anti-virus server uri if any
        if (obj.has("antivirus_server_url")) {
            config.setAntiVirusServerUri(Uri.parse(obj.getString("antivirus_server_url")));
        }

        config.setShouldAcceptTlsExtensions(obj.optBoolean("tls_extensions", true));

        // Set the TLS versions if any
        if (obj.has("tls_versions")) {
            List<TlsVersion> tlsVersions = new ArrayList<>();
            JSONArray tlsVersionsArray = obj.optJSONArray("tls_versions");
            if (tlsVersionsArray != null) {
                for (int i = 0; i < tlsVersionsArray.length(); i++) {
                    tlsVersions.add(TlsVersion.forJavaName(tlsVersionsArray.getString(i)));
                }
            }
            config.setAcceptedTlsVersions(tlsVersions);
        } else {
            config.setAcceptedTlsVersions(null);
        }

        // Set the TLS cipher suites if any

        if (obj.has("tls_cipher_suites")) {
            List<CipherSuite> tlsCipherSuites = new ArrayList<>();
            JSONArray tlsCipherSuitesArray = obj.optJSONArray("tls_cipher_suites");
            if (tlsCipherSuitesArray != null) {
                for (int i = 0; i < tlsCipherSuitesArray.length(); i++) {
                    tlsCipherSuites.add(CipherSuite.forJavaName(tlsCipherSuitesArray.getString(i)));
                }
            }
            config.setAcceptedTlsCipherSuites(tlsCipherSuites);
        } else {
            config.setAcceptedTlsCipherSuites(null);
        }

        return config;
    }

    public static final class CertificatePin {
        private static final String HOSTNAME_JSON_KEY = "hostname";
        private static final String PUBLIC_HASH_KEY_JSON_KEY = "publicHashKey";
        private final String hostname;
        private final String publicKeyHash;

        public CertificatePin(String hostname, String publicKeyHash) {
            this.hostname = hostname;
            this.publicKeyHash = publicKeyHash;
        }

        public String getHostname() {
            return hostname;
        }

        public String getPublicKeyHash() {
            return publicKeyHash;
        }

        public JSONObject toJson() throws JSONException {
            JSONObject json = new JSONObject();
            json.put(HOSTNAME_JSON_KEY, hostname);
            json.put(PUBLIC_HASH_KEY_JSON_KEY, publicKeyHash);
            return json;
        }

        public static CertificatePin fromJson(JSONObject json) throws JSONException {
            return new CertificatePin(
                json.getString(HOSTNAME_JSON_KEY),
                json.getString(PUBLIC_HASH_KEY_JSON_KEY)
            );
        }
    }

    // API compatibility with matrix SDK
    public List<Fingerprint> getAllowedFingerprints() {
        return mAllowedFingerprints;
    }
}
