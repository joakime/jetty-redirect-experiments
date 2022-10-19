# Experiments with Jetty and Redirect on HTTP/2

## To compile & test.

Use OpenJDK 11 (or newer)

``` shell
mvn clean install
```

## To run server (you should compile & test first)

``` shell
mvn exec:java
```

You should see something like ...

``` plain
[INFO] --- exec-maven-plugin:3.1.0:java (default-cli) @ jetty-redirect-experiments ---
2022-10-19 14:27:44.833:INFO :oejs.Server:org.example.App.main(): jetty-10.0.12; built: 2022-09-14T01:54:40.076Z; git: 408d0139887e27a57b54ed52e2d92a36731a7e88; jvm 17.0.3+7
2022-10-19 14:27:44.879:INFO :oejsh.ContextHandler:org.example.App.main(): Started o.e.j.s.h.ContextHandler@11915c38{/post,null,AVAILABLE}
2022-10-19 14:27:44.879:INFO :oejsh.ContextHandler:org.example.App.main(): Started o.e.j.s.h.ContextHandler@6c28b878{/other,null,AVAILABLE}
2022-10-19 14:27:44.890:INFO :oejs.AbstractConnector:org.example.App.main(): Started ServerConnector@2dbbf781{HTTP/1.1, (http/1.1, h2c)}{0.0.0.0:8080}
2022-10-19 14:27:44.913:INFO :oejus.SslContextFactory:org.example.App.main(): x509=X509@2ccfc58d(jetty,h=[jetty.mortbay.org],a=[],w=[]) for Server@581927b7[provider=null,keyStore=file:///home/joakim/code/jetty/github/jetty-redirect-experiments/src/main/resources/ssl/keystore,trustStore=null]
2022-10-19 14:27:45.002:INFO :oejs.AbstractConnector:org.example.App.main(): Started ServerConnector@69068116{SSL, (ssl, alpn, h2, http/1.1)}{0.0.0.0:8443}
2022-10-19 14:27:45.009:INFO :oejs.Server:org.example.App.main(): Started Server@7173d5c8{STARTING}[10.0.12,sto=0] @1404ms
2022-10-19 14:27:45.009:INFO :oe.App:org.example.App.main(): Server is listening for HTTP/1.1 on URI: http://localhost:8080/
2022-10-19 14:27:45.009:INFO :oe.App:org.example.App.main(): Server is listening for HTTP/2 on URI: https://localhost:8443/
```

Tip: use Ctrl+C to close/shutdown the server.

## To test with curl and HTTP/2.

This is the command line with curl version "7.81.0 (x86_64-pc-linux-gnu)".  
This command line does _not follow_ redirects. (default behavior in curl)

```shell
curl --insecure --http2 'https://localhost:8443/post/info' -X POST -H "Content-type: text/xml" --data-binary @pom.xml -vvv
```

You should see something like ...

``` plain
Note: Unnecessary use of -X or --request, POST is already inferred.
*   Trying 127.0.0.1:8443...
* Connected to localhost (127.0.0.1) port 8443 (#0)
* ALPN, offering h2
* ALPN, offering http/1.1
* TLSv1.0 (OUT), TLS header, Certificate Status (22):
..(snip)..
* TLSv1.3 (OUT), TLS handshake, Finished (20):
* SSL connection using TLSv1.3 / TLS_AES_256_GCM_SHA384
* ALPN, server accepted to use h2
* Server certificate:
..(snip)..
*  SSL certificate verify result: EE certificate key too weak (66), continuing anyway.
* Using HTTP2, server supports multiplexing
* Connection state changed (HTTP/2 confirmed)
* Copying HTTP/2 data in stream buffer to connection buffer after upgrade: len=0
..(snip)..
* Using Stream ID: 1 (easy handle 0x55ed2a2c5ba0)
* TLSv1.2 (OUT), TLS header, Supplemental data (23):
> POST /post/info HTTP/2
> Host: localhost:8443
> user-agent: curl/7.81.0
> accept: */*
> content-type: text/xml
> content-length: 3324
> 
* TLSv1.2 (OUT), TLS header, Supplemental data (23):
..(snip)...
* TLSv1.2 (IN), TLS header, Supplemental data (23):
< HTTP/2 307 
< server: Jetty(10.0.12)
< x-powered-by: Jetty(10.0.12)
< date: Wed, 19 Oct 2022 19:28:34 GMT
< location: https://localhost:8443/other/
< content-length: 0
< 
* TLSv1.2 (IN), TLS header, Supplemental data (23):
* TLSv1.3 (IN), TLS handshake, Newsession Ticket (4):
* old SSL session ID is stale, removing
* Connection #0 to host localhost left intact
```

## To test with curl and HTTP/2 and follow redirects

This is the command line with curl version "7.81.0 (x86_64-pc-linux-gnu)".  
This command line will follow redirects.  (the `--location` line tells curl to follow redirects)

```shell
curl --insecure --location --http2 'https://localhost:8443/post/info' -X POST -H "Content-type: text/xml" --data-binary @pom.xml -vvv
```

You'll see the following ...

``` plain
*   Trying 127.0.0.1:8443...
* Connected to localhost (127.0.0.1) port 8443 (#0)
* ALPN, offering h2
* ALPN, offering http/1.1
* TLSv1.0 (OUT), TLS header, Certificate Status (22):
..(snip)..
* TLSv1.3 (OUT), TLS handshake, Finished (20):
* SSL connection using TLSv1.3 / TLS_AES_256_GCM_SHA384
* ALPN, server accepted to use h2
* Server certificate:
..(snip)..
* TLSv1.2 (OUT), TLS header, Supplemental data (23):
> POST /post/info HTTP/2
> Host: localhost:8443
> user-agent: curl/7.81.0
> accept: */*
> content-type: text/xml
> content-length: 3324
> 
* TLSv1.2 (OUT), TLS header, Supplemental data (23):
..(snip)..
* TLSv1.2 (IN), TLS header, Supplemental data (23):
< HTTP/2 307 
< server: Jetty(10.0.12)
< x-powered-by: Jetty(10.0.12)
< date: Wed, 19 Oct 2022 19:36:07 GMT
< location: https://localhost:8443/other/
< content-length: 0
< 
* TLSv1.2 (IN), TLS header, Supplemental data (23):
* TLSv1.3 (IN), TLS handshake, Newsession Ticket (4):
* old SSL session ID is stale, removing
* Connection #0 to host localhost left intact
* Issue another request to this URL: 'https://localhost:8443/other/'
* Found bundle for host localhost: 0x558adbe71ff0 [can multiplex]
* Re-using existing connection! (#0) with host localhost
* Connected to localhost (127.0.0.1) port 8443 (#0)
* Using Stream ID: 3 (easy handle 0x558adbe7aba0)
* TLSv1.2 (OUT), TLS header, Supplemental data (23):
> POST /other/ HTTP/2
> Host: localhost:8443
> user-agent: curl/7.81.0
> accept: */*
> content-type: text/xml
> content-length: 3324
> 
* TLSv1.2 (OUT), TLS header, Supplemental data (23):
* We are completely uploaded and fine
* TLSv1.2 (IN), TLS header, Supplemental data (23):
< HTTP/2 200 
< server: Jetty(10.0.12)
< x-powered-by: Jetty(10.0.12)
< date: Wed, 19 Oct 2022 19:36:07 GMT
< content-type: text/plain;charset=utf-8
< content-length: 29
< 
Read 3,324 bytes from request
* Connection #0 to host localhost left intact
```

This shows that the POST is followed, and resubmitted by curl to the followed location.