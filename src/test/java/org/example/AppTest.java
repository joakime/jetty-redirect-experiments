package org.example;

import java.net.URI;

import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.util.FormRequestContent;
import org.eclipse.jetty.util.Fields;
import org.eclipse.jetty.util.component.LifeCycle;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class AppTest
{
    private static final Logger LOG = LoggerFactory.getLogger(AppTest.class);
    private App app;

    @BeforeEach
    public void setup() throws Exception
    {
        app = new App();
        app.startServer();
    }

    @AfterEach
    public void tearDown()
    {
        LifeCycle.stop(app.getClient());
        LifeCycle.stop(app.getServer());
    }

    @ParameterizedTest
    @EnumSource(App.FollowRedirects.class)
    public void testHttp11PostThatResultsIn307(App.FollowRedirects followRedirects) throws Exception
    {
        app.startClient(followRedirects);
        URI destUri = App.HTTP1_URI.resolve("/post/info");
        LOG.info("Client request to: {}", destUri);
        Fields postForm = new Fields();
        postForm.put("greetings", "Hello Mr Anderson");
        ContentResponse response = app.getClient().POST(destUri)
            .body(new FormRequestContent(postForm))
            .send();
        System.err.println("Response: " + response.getHeaders());
        System.err.println(response.getContentAsString());
        int expectedStatus = followRedirects == App.FollowRedirects.NO_FOLLOW ? 307 : 200;
        assertThat(response.getStatus(), is(expectedStatus));
    }

    @ParameterizedTest
    @EnumSource(App.FollowRedirects.class)
    public void testHttp2PostThatResultsIn307(App.FollowRedirects followRedirects) throws Exception
    {
        app.startClient(followRedirects);
        URI destUri = App.HTTP2_URI.resolve("/post/info");
        LOG.info("Client request to: {}", destUri);
        Fields postForm = new Fields();
        postForm.put("greetings", "Hello Mr Anderson");
        ContentResponse response = app.getClient().POST(destUri)
            .body(new FormRequestContent(postForm))
            .send();
        System.err.println("Response: " + response.getHeaders());
        System.err.println(response.getContentAsString());
        int expectedStatus = followRedirects == App.FollowRedirects.NO_FOLLOW ? 307 : 200;
        assertThat(response.getStatus(), is(expectedStatus));
    }

    @ParameterizedTest
    @EnumSource(App.FollowRedirects.class)
    public void testHttp11PostThatResultsIn307WithBody(App.FollowRedirects followRedirects) throws Exception
    {
        app.startClient(followRedirects);
        URI destUri = App.HTTP1_URI.resolve("/post/body");
        LOG.info("Client request to: {}", destUri);
        Fields postForm = new Fields();
        postForm.put("greetings", "Hello Mr Anderson");
        ContentResponse response = app.getClient().POST(destUri)
            .body(new FormRequestContent(postForm))
            .send();
        System.err.println("Response: " + response.getHeaders());
        System.err.println(response.getContentAsString());
        int expectedStatus = followRedirects == App.FollowRedirects.NO_FOLLOW ? 307 : 200;
        assertThat(response.getStatus(), is(expectedStatus));
    }

    @ParameterizedTest
    @EnumSource(App.FollowRedirects.class)
    public void testHttp2PostThatResultsIn307WithBody(App.FollowRedirects followRedirects) throws Exception
    {
        app.startClient(followRedirects);
        URI destUri = App.HTTP2_URI.resolve("/post/body");
        LOG.info("Client request to: {}", destUri);
        Fields postForm = new Fields();
        postForm.put("greetings", "Hello Mr Anderson");
        ContentResponse response = app.getClient().POST(destUri)
            .body(new FormRequestContent(postForm))
            .send();
        System.err.println("Response: " + response.getHeaders());
        System.err.println(response.getContentAsString());
        int expectedStatus = followRedirects == App.FollowRedirects.NO_FOLLOW ? 307 : 200;
        assertThat(response.getStatus(), is(expectedStatus));
    }
}
