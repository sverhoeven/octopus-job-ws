package nl.esciencecenter.octopus.webservice.mac;

/*
 * #%L
 * Octopus Job Webservice
 * %%
 * Copyright (C) 2013 Nederlands eScience Center
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import static org.fest.assertions.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.Random;

import nl.esciencecenter.octopus.webservice.mac.MacCredential;
import nl.esciencecenter.octopus.webservice.mac.MacScheme;

import org.apache.http.Header;
import org.apache.http.ProtocolException;
import org.apache.http.auth.AuthenticationException;
import org.apache.http.auth.MalformedChallengeException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.RequestWrapper;
import org.apache.http.message.BasicHeader;
import org.junit.Before;
import org.junit.Test;

public class MacSchemeTest {
    private MacScheme scheme;

    @Before
    public void setUp() {
        scheme = new MacScheme();
    }

    @Test
    public void testProcessChallenge() throws MalformedChallengeException {
        Header header = new BasicHeader("WWW-Authenticate", "NTLM");
        scheme.processChallenge(header);
    }

    @Test
    public void testGetSchemeName() {
        assertEquals("MAC", scheme.getSchemeName());
    }

    @Test
    public void testGetParameter() {
        assertNull(scheme.getParameter("something"));
    }

    @Test
    public void testGetRealm() {
        assertNull(scheme.getRealm());
    }

    @Test
    public void testIsConnectionBased() {
        assertFalse(scheme.isConnectionBased());
    }

    @Test
    public void testIsComplete() {
        assertTrue(scheme.isComplete());
    }

    @Test
    public void testSetGetRandom() {
        Random rand = new Random();
        scheme.setRandom(rand);
        assertEquals(rand, scheme.getRandom());
    }

    @Test
    public void testAuthenticate() throws URISyntaxException,
            AuthenticationException {
        // set date to known value
        scheme.setDate(new Date(0));
        // make nonce creation predictable
        // by mocking random generator
        Random rand = mock(Random.class);
        scheme.setRandom(rand);

        String id = "eyJzYWx0IjogIjU3MjY0NCIsICJleHBpcmVzIjogMTM4Njc3MjEwOC4yOTIyNTUsICJ1c2VyaWQiOiAiam9ibWFuYWdlciJ9KBJRMeTW2G9I6jlYwRj6j8koAek=";
        String key = "_B1YfcqEYpZxyTx_-411-QdBOSI=";
        MacCredential credentials = new MacCredential(id, key, new URI(
                "http://localhost"));
        HttpGet request = new HttpGet("http://localhost");
        Header header = scheme.authenticate(credentials, request, null);

        String headerValue = "MAC id=\"eyJzYWx0IjogIjU3MjY0NCIsICJleHBpcmVzIjogMTM4Njc3MjEwOC4yOTIyNTUsICJ1c2VyaWQiOiAiam9ibWFuYWdlciJ9KBJRMeTW2G9I6jlYwRj6j8koAek=\",";
        headerValue += "ts=\"0\",";
        headerValue += "nonce=\"0\",";
        headerValue += "mac=\"cDuU6wfugKOnoAaEEEUz22J/kTc=\"";
        assertEquals("Authorization", header.getName());
        assertEquals(headerValue, header.getValue());
    }

    @Test
    public void testAuthenticateWrapped() throws URISyntaxException,
            ProtocolException {
        // set date to known value
        scheme.setDate(new Date(0));
        // make nonce creation predictable
        // by mocking random generator
        Random rand = mock(Random.class);
        scheme.setRandom(rand);

        String id = "eyJzYWx0IjogIjU3MjY0NCIsICJleHBpcmVzIjogMTM4Njc3MjEwOC4yOTIyNTUsICJ1c2VyaWQiOiAiam9ibWFuYWdlciJ9KBJRMeTW2G9I6jlYwRj6j8koAek=";
        String key = "_B1YfcqEYpZxyTx_-411-QdBOSI=";
        MacCredential credentials = new MacCredential(id, key, new URI(
                "http://localhost"));
        HttpGet request = new HttpGet("http://localhost");
        RequestWrapper wrappedRequest = new RequestWrapper(request);
        Header header = scheme.authenticate(credentials, wrappedRequest, null);

        String headerValue = "MAC id=\"eyJzYWx0IjogIjU3MjY0NCIsICJleHBpcmVzIjogMTM4Njc3MjEwOC4yOTIyNTUsICJ1c2VyaWQiOiAiam9ibWFuYWdlciJ9KBJRMeTW2G9I6jlYwRj6j8koAek=\",";
        headerValue += "ts=\"0\",";
        headerValue += "nonce=\"0\",";
        headerValue += "mac=\"cDuU6wfugKOnoAaEEEUz22J/kTc=\"";
        assertEquals("Authorization", header.getName());
        assertEquals(headerValue, header.getValue());
    }

    @Test
    public void testAuthenticateInvalidAlgorithm() throws URISyntaxException {
        String id = "eyJzYWx0IjogIjU3MjY0NCIsICJleHBpcmVzIjogMTM4Njc3MjEwOC4yOTIyNTUsICJ1c2VyaWQiOiAiam9ibWFuYWdlciJ9KBJRMeTW2G9I6jlYwRj6j8koAek=";
        String key = "_B1YfcqEYpZxyTx_-411-QdBOSI=";
        MacCredential credentials = new MacCredential(id, key, new URI(
                "http://localhost"));
        credentials.setAlgorithm("blowfish");
        HttpGet request = new HttpGet("http://localhost");
        try {
            scheme.authenticate(credentials, request, null);
            fail();
        } catch (AuthenticationException e) {
            assertEquals("Algorithm is not supported", e.getMessage());
        }
    }

    @Test
    public void testSetGetDate() {
        Date date = new Date(0);
        scheme.setDate(date);
        assertEquals(date, scheme.getDate());
    }

    @Test
    public void testGetPort() throws URISyntaxException {
        assertEquals(80, MacScheme.getPort(new URI("http://localhost")));

        assertEquals(443, MacScheme.getPort(new URI("https://localhost")));

        assertEquals(8080, MacScheme.getPort(new URI("http://localhost:8080")));
    }

    @Test
    public void testAlgorithmMapper() {
        assertEquals("HmacSHA1", MacScheme.algorithmMapper("hmac-sha-1"));

        assertEquals("HmacSHA256", MacScheme.algorithmMapper("hmac-sha-256"));

        assertEquals("bla", MacScheme.algorithmMapper("bla"));
    }

    @Test
    public void testEquals() {
        assertThat(scheme.equals(scheme)).isTrue();

        assertThat(scheme.equals(null)).isFalse();

        assertThat(scheme.equals("string")).isFalse();
    }

}
