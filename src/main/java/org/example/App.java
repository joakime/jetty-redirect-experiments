package org.example;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.alpn.server.ALPNServerConnectionFactory;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http2.HTTP2Cipher;
import org.eclipse.jetty.http2.client.HTTP2Client;
import org.eclipse.jetty.http2.client.http.HttpClientTransportOverHTTP2;
import org.eclipse.jetty.http2.server.HTTP2CServerConnectionFactory;
import org.eclipse.jetty.http2.server.HTTP2ServerConnectionFactory;
import org.eclipse.jetty.io.ClientConnector;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.util.IO;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class App
{
    private static final Logger LOG = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) throws Exception
    {
        App app = new App();
        app.startServer();
        app.waitForServerExit();
    }

    private static final int HTTP1_PORT = 8080;
    private static final int HTTP2_PORT = 8443;
    public static final URI HTTP1_URI = URI.create(String.format("http://localhost:%d/", HTTP1_PORT));
    public static final URI HTTP2_URI = URI.create(String.format("https://localhost:%d/", HTTP2_PORT));

    enum FollowRedirects
    {
        FOLLOW,
        NO_FOLLOW
    }

    private Server server;
    private HttpClient client;

    public Server getServer()
    {
        return this.server;
    }

    public HttpClient getClient()
    {
        return this.client;
    }

    public void startClient(FollowRedirects followRedirects) throws Exception
    {
        ClientConnector clientConnector = new ClientConnector();
        QueuedThreadPool clientThreads = new QueuedThreadPool();
        clientThreads.setName("client");
        clientConnector.setExecutor(clientThreads);
        SslContextFactory.Client sslClient = new SslContextFactory.Client();
        sslClient.setTrustAll(true); // disable for localhost testing
        clientConnector.setSslContextFactory(sslClient);
        HTTP2Client http2Client = new HTTP2Client(clientConnector);
        client = new HttpClient(new HttpClientTransportOverHTTP2(http2Client));
        switch (followRedirects)
        {
            case FOLLOW:
                client.setFollowRedirects(true);
                break;
            case NO_FOLLOW:
                client.setFollowRedirects(false);
                break;
        }
        client.start();
    }

    public void startServer() throws Exception
    {
        server = new Server();

        // HTTP Configuration
        HttpConfiguration httpConfig = new HttpConfiguration();
        httpConfig.setSecureScheme("https");
        httpConfig.setSendXPoweredBy(true);
        httpConfig.setSendServerVersion(true);

        // HTTP Connector
        ServerConnector http = new ServerConnector(server, new HttpConnectionFactory(httpConfig), new HTTP2CServerConnectionFactory(httpConfig));
        http.setPort(HTTP1_PORT);
        server.addConnector(http);

        // SSL Context Factory for HTTPS and HTTP/2
        Path keystorePath = Paths.get("src/main/resources/ssl/keystore").toAbsolutePath();
        if (!Files.exists(keystorePath))
            throw new FileNotFoundException(keystorePath.toString());
        SslContextFactory.Server sslContextFactory = new SslContextFactory.Server();
        sslContextFactory.setKeyStorePath(keystorePath.toString());
        sslContextFactory.setKeyStorePassword("OBF:1vny1zlo1x8e1vnw1vn61x8g1zlu1vn4");
        sslContextFactory.setKeyManagerPassword("OBF:1u2u1wml1z7s1z7a1wnl1u2g");
        sslContextFactory.setCipherComparator(HTTP2Cipher.COMPARATOR);

        // HTTPS Configuration
        HttpConfiguration httpsConfig = new HttpConfiguration(httpConfig);
        SecureRequestCustomizer secureRequestCustomizer = new SecureRequestCustomizer();
        secureRequestCustomizer.setSniRequired(false); // disable for localhost testing
        secureRequestCustomizer.setSniHostCheck(false); // disable for localhost testing
        httpsConfig.addCustomizer(secureRequestCustomizer);

        // HTTP/2 Connection Factory
        HTTP2ServerConnectionFactory h2 = new HTTP2ServerConnectionFactory(httpsConfig);

        ALPNServerConnectionFactory alpn = new ALPNServerConnectionFactory();
        alpn.setDefaultProtocol(http.getDefaultProtocol());

        // SSL Connection Factory
        SslConnectionFactory ssl = new SslConnectionFactory(sslContextFactory, alpn.getProtocol());

        // HTTP/2 Connector
        ServerConnector http2 =
            new ServerConnector(server, ssl, alpn, h2, new HttpConnectionFactory(httpsConfig));
        http2.setPort(HTTP2_PORT);
        server.addConnector(http2);

        ContextHandler postHandler = new ContextHandler();
        postHandler.setContextPath("/post");
        postHandler.setHandler(new AbstractHandler()
        {
            @Override
            public void handle(String s, Request request, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws IOException
            {
                LOG.info("handle /post");
                URI location = request.isSecure() ? HTTP2_URI.resolve("/other/") : HTTP1_URI.resolve("/other/");
                LOG.info("Redirect to {}", location);
                httpServletResponse.setStatus(HttpServletResponse.SC_TEMPORARY_REDIRECT);
                httpServletResponse.setHeader("Location", location.toASCIIString());

                if (request.getPathInfo().equals("/body"))
                    httpServletResponse.getWriter().println("This is some body content");

                request.setHandled(true);
            }
        });

        ContextHandler otherHandler = new ContextHandler();
        otherHandler.setContextPath("/other");
        otherHandler.setHandler(new AbstractHandler()
        {
            @Override
            public void handle(String s, Request request, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws IOException
            {
                LOG.info("handle /other");

                httpServletResponse.setCharacterEncoding("utf-8");
                httpServletResponse.setContentType("text/plain");

                if (HttpMethod.POST.is(request.getMethod()))
                {
                    InputStream in = request.getInputStream();
                    try (ByteArrayOutputStream out = new ByteArrayOutputStream())
                    {
                        IO.copy(in, out);
                        httpServletResponse.getWriter().printf("Read %,d bytes from request", out.size());
                    }
                }
                else
                {
                    httpServletResponse.getWriter().println("This is the other handler");
                }
                request.setHandled(true);
            }
        });

        HandlerList handlers = new HandlerList();

        ContextHandlerCollection contextHandlerCollection = new ContextHandlerCollection();
        contextHandlerCollection.addHandler(postHandler);
        contextHandlerCollection.addHandler(otherHandler);

        handlers.addHandler(contextHandlerCollection);
        handlers.addHandler(new DefaultHandler());

        server.setHandler(handlers);
        server.start();

        LOG.info("Server is listening for HTTP/1.1 on URI: {}", HTTP1_URI);
        LOG.info("Server is listening for HTTP/2 on URI: {}", HTTP2_URI);
    }

    private void waitForServerExit() throws InterruptedException
    {
        server.join();
    }
}
