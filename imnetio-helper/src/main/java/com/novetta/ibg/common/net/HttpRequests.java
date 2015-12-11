/*
 * (c) 2015 Mike Chaberski
 */
package com.novetta.ibg.common.net;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import static com.google.common.base.Preconditions.checkNotNull;
import com.google.common.base.Strings;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import com.google.common.io.ByteStreams;
import com.novetta.ibg.common.net.HttpRequests.DefaultHttpRequester.HttpGetRequestFactory;
import com.novetta.ibg.common.net.HttpRequests.DefaultHttpRequester.RequestConfigFactory;
import com.novetta.ibg.common.net.HttpRequests.DefaultHttpRequester.SystemHttpClientFactory;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Map;
import javax.annotation.Nullable;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.HttpClients;
 
/**
 * Class that provides utilities relating to HTTP requests. Primarily 
 * provides a simple HTTP client to make simple requests.
 * 
 * <p>Depends on Java 7, Google Guava 18, a JSR-305 implementation, and Apache HTTP Client 4.4ish.
 * @author mchaberski
 */
public class HttpRequests {
 
    private HttpRequests() {}
    
    private static final ImmutableMultimap<String, String> emptyMultimap = ImmutableMultimap.of();
 
    private static final ImmutableSortedSet<Integer> DEFAULT_NON_ERROR_STATUS_CODES = 
            ImmutableSortedSet.<Integer>naturalOrder()
            .add(HttpStatus.SC_OK)
            .add(HttpStatus.SC_CREATED)
            .add(HttpStatus.SC_ACCEPTED)
            .add(HttpStatus.SC_NON_AUTHORITATIVE_INFORMATION)
            .add(HttpStatus.SC_NO_CONTENT)
            .add(HttpStatus.SC_RESET_CONTENT)
            .add(HttpStatus.SC_PARTIAL_CONTENT)
            .add(HttpStatus.SC_MOVED_PERMANENTLY)
            .add(HttpStatus.SC_MOVED_TEMPORARILY) 
            .add(HttpStatus.SC_TEMPORARY_REDIRECT)
            .add(HttpStatus.SC_NOT_MODIFIED)
            .build();
    
    public static ImmutableSortedSet<Integer> getDefaultNonErrorStatusCodes() {
        return DEFAULT_NON_ERROR_STATUS_CODES;
    }
    
    /**
     * Class that represents a response from an HTTP request.
     */
    public static class ResponseData {
 
        
        /**
         * Response headers.
         */
        public final ImmutableMultimap<String, String> headers;
        
        /**
         * Request URI.
         */
        public final URI requestUri;
        
        /**
         * Response status code. For example, 200 for OK or 404 for Not Found.
         * If it's zero, it means something went horribly wrong, such as a 
         * hostname resolution failure.
         */
        public final int code;
        
        /**
         * Response bytes. This is never null, but it may be empty.
         */
        public final byte[] data;
        
        /**
         * Exception thrown in making HTTP request. This is never null, 
         * but it may not be {@link Optional#isPresent() present}.
         */
        public final Optional<Exception> exception;
 
        public ResponseData(URI requestUri, int code, byte[] data, Multimap<String, String> headers) {
            this(requestUri, code, data, headers, null);
        }
        
        private ResponseData(URI requestUri, int code, byte[] data, Multimap<String, String> headers, @Nullable Exception exception) {
            this.code = code;
            this.data = checkNotNull(data);
            this.exception = Optional.fromNullable(exception);
            this.headers = ImmutableMultimap.copyOf(headers);
            this.requestUri = checkNotNull(requestUri);
        }
 
        public ResponseData(URI requestUri, Exception exception) {
            this(requestUri, 0, new byte[0], emptyMultimap, checkNotNull(exception));
        }
 
        public ResponseData(URI requestUri, int code, Multimap<String, String> headers, Exception exception) {
            this(requestUri, code, new byte[0], headers, checkNotNull(exception));
        }
        
        @Override
        public String toString() {
            return "ResponseData{" 
                    + "code=" + code 
                    + ", data.length=" + data.length 
                    + ", hasException=" + exception.isPresent() + '}';
        }
        
        /**
         * Returns an iterable over all header values corresponding to the given header name.
         * Correspondence evaluation is case-insensitive. This never returns null, but it may
         * return an empty iterable.
         * @param headerName the header name
         * @return an iterable over the header values
         */
        public Iterable<String> getHeaderValues(String headerName) {
            checkNotNull(headerName, "headerName");
            Iterable<String> headerValues = ImmutableList.of();
            for (String possibleHeaderName : headers.keySet()) {
                if (headerName.equalsIgnoreCase(possibleHeaderName)) {
                    headerValues = Iterables.concat(headerValues, headers.get(possibleHeaderName));
                }
            }
            return headerValues;
        }
    }
 
    /**
     * Default implementation of a response handler that creates a 
     * response data object.
     * @see ResponseData
     */
    public static class ResponseDataResponseHandler implements ResponseHandler<ResponseData> {
 
        private final URI requestUri;
 
        public ResponseDataResponseHandler(URI requestUri) {
            this.requestUri = checkNotNull(requestUri);
        }
        
        public static ImmutableMultimap<String, String> buildHeaders(Header[] headers) {
            checkNotNull(headers);
            ImmutableMultimap.Builder<String, String> b = ImmutableMultimap.builder();
            for (Header header : headers) {
                b.put(header.getName(), Strings.nullToEmpty(header.getValue()));
            }
            return b.build();
        }
        
        @Override
        public ResponseData handleResponse(HttpResponse response) {
            ByteArrayOutputStream out = new ByteArrayOutputStream(100 * 1024);
            Multimap<String, String> headers = buildHeaders(response.getAllHeaders());
            HttpEntity entity = response.getEntity();
            ResponseData responseData;
            if (entity == null) { // treat same as no data
                return new ResponseData(requestUri, response.getStatusLine().getStatusCode(), new byte[0], headers);
            } else {
                int statusCode = response.getStatusLine().getStatusCode();
                try (InputStream in = entity.getContent()) {
                    ByteStreams.copy(in, out);
                    responseData = new ResponseData(requestUri, statusCode, out.toByteArray(), headers);
                } catch (IOException e) {
                    responseData = new ResponseData(requestUri, statusCode, headers, e);
                } 
            }
            return responseData;
        }
        
    }
  
    /**
     * Creates and returns a new requester instance.
     * @return the new requester
     */
    public static HttpRequester newRequester() {
        return new DefaultHttpRequester();
    }
    
    /**
     * Creates and returns a new requester instance configured with a timeout.
     * @param timeoutMs the timeout, in milliseconds
     * @return the requester instance
     */
    public static HttpRequester newRequesterWithTimeout(int timeoutMs) {
        return new DefaultHttpRequester(new SystemHttpClientFactory(), new HttpGetRequestFactory(new RequestConfigFactory(timeoutMs)));
    }
    
    /**
     * Class that represents an actor that makes an HTTP request.
     */
    public static interface HttpRequester {

        /**
         * Sends a request to download a given URI with no additional request headers.
         * @param uri the URI
         * @return the response data
         */
        public ResponseData retrieve(URI uri);
        
        /**
         * Sends a request, with headers, to download a given URI with the 
         * @param uri the URI
         * @param requestHeaders the request headers to send
         * @return the response data
         */
        public ResponseData retrieve(URI uri, Multimap<String, String> requestHeaders);

    }
    
    public static class DefaultHttpRequester implements HttpRequester {
        
        /**
         * Default timeout (infinite).
         */
        public static final int DEFAULT_TIMEOUT_MS = 0;
        
        private final Function<URI, HttpClient> httpClientFactory;
        private final HttpRequestFactory httpRequestFactory;
        
        public DefaultHttpRequester() {
            this(new SystemHttpClientFactory(), new HttpGetRequestFactory());
        }
 
        public DefaultHttpRequester(Function<URI, HttpClient> httpClientFactory, HttpRequestFactory httpRequestFactory) {
            this.httpClientFactory = checkNotNull(httpClientFactory, "httpClientFactory");
            this.httpRequestFactory = checkNotNull(httpRequestFactory, "httpRequestFactory");
        }
        
        /**
         * Sends a request to download a given URL.
         * @param url the URL for the HTTP request
         * @return the response data
         * @throws IllegalArgumentException if a URISyntaxException is thrown
         * from {@link URI#create(java.lang.String) }
         */
        public ResponseData retrieve(String url) throws IllegalArgumentException {
            return retrieve(URI.create(url));
        }
        
        @Override
        public ResponseData retrieve(URI uri) {
            return retrieve(uri, emptyMultimap);
        }
        
        @Override
        public ResponseData retrieve(URI uri, Multimap<String, String> requestHeaders) {
            HttpClient client = httpClientFactory.apply(uri);
            HttpUriRequest request = httpRequestFactory.createRequest(uri, requestHeaders);
            ResponseData responseData;
            try {
                responseData = client.execute(request, new ResponseDataResponseHandler(uri));
            } catch (IOException ex) {
                responseData = new ResponseData(uri, ex);
            }
            return responseData;
        }
        
        public static interface HttpRequestFactory {
            
            HttpUriRequest createRequest(URI uri, Multimap<String, String> requestHeaders);
            
        }
        
        public static class SystemHttpClientFactory implements Function<URI, HttpClient> {

            @Override
            public HttpClient apply(URI uri) {
                return HttpClients.createSystem();
            }
            
        }
        
        public abstract static class HttpRequestFactoryBase implements HttpRequestFactory {

            protected final Supplier<RequestConfig> requestConfigFactory;
            
            public HttpRequestFactoryBase(Supplier<RequestConfig> requestConfigFactory) {
                this.requestConfigFactory = checkNotNull(requestConfigFactory, "requestConfigFactory");
            }
            
            protected abstract HttpRequestBase createRequestBase(URI uri);
            
            @Override
            public HttpUriRequest createRequest(URI uri, Multimap<String, String> requestHeaders) {
                checkNotNull(requestHeaders, "requestHeaders");
                HttpRequestBase request = new HttpGet(uri);
                for (Map.Entry<String, String> entry : requestHeaders.entries()) {
                    request.addHeader(entry.getKey(), entry.getValue());
                }
                request.setConfig(requestConfigFactory.get());
                return request;
            }

        }
        
        public static class HttpGetRequestFactory extends HttpRequestFactoryBase {

            public HttpGetRequestFactory() {
                this(new RequestConfigFactory());
            }
            
            public HttpGetRequestFactory(RequestConfigFactory requestConfigFactory) {
                super(requestConfigFactory);
            }
            
            @Override
            protected HttpRequestBase createRequestBase(URI uri) {
                return new HttpGet(uri);
            }
            
        }
        
        public static class RequestConfigFactory implements Supplier<RequestConfig> {

            public static final int DEFAULT_TIMEOUT_MS = 0;
            
            private final int timeoutMs;

            public RequestConfigFactory() {
                this(DEFAULT_TIMEOUT_MS);
            }
            
            public RequestConfigFactory(int timeoutMs) {
                this.timeoutMs = timeoutMs;
            }
            
            @Override
            public RequestConfig get() {
                return createAndConfigureRequestConfig().build();
            }
            
            protected RequestConfig.Builder createAndConfigureRequestConfig() {
                return RequestConfig.custom()
                        .setSocketTimeout(timeoutMs);
            }

       }

    }
    
}