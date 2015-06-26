/*
 * (c) 2015 Mike Chaberski
 */
package com.novetta.ibg.common.net;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.io.ByteStreams;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import javax.annotation.Nullable;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.HttpClients;

/**
 * Class that provides utilities relating to HTTP requests. Primary use
 * of this class is to instantiate a requester object that performs
 * dumb, minimally configurable interaction over HTTP.
 * @author mchaberski
 */
public class HttpRequests {

    private HttpRequests() {}
    
    private static final ImmutableSortedSet<Integer> nonErrorCodes = 
            ImmutableSortedSet.<Integer>naturalOrder()
            .add(HttpURLConnection.HTTP_OK)
            .add(HttpURLConnection.HTTP_CREATED)
            .add(HttpURLConnection.HTTP_ACCEPTED)
            .add(HttpURLConnection.HTTP_NOT_AUTHORITATIVE)
            .add(HttpURLConnection.HTTP_NO_CONTENT)
            .add(HttpURLConnection.HTTP_RESET)
            .add(HttpURLConnection.HTTP_PARTIAL)
            .add(HttpURLConnection.HTTP_MOVED_PERM)
            .add(HttpStatus.SC_MOVED_TEMPORARILY)
            .add(HttpStatus.SC_TEMPORARY_REDIRECT) // 307 = TEMPORARY_REDIRECT and 302 = "FOUND"; see http://docs.python.org/2/library/httplib.html
            .add(HttpURLConnection.HTTP_NOT_MODIFIED)
            .build();
    
    /**
     * Class that represents a response from an HTTP request.
     */
    public static class ResponseData {
        
        /**
         * Response status code. For example, 200 for OK or 404 for Not Found.
         * If it's zero, it means something went horribly wrong.
         */
        public final int code;
        
        /**
         * Response bytes. This is never null.
         */
        public final byte[] data;
        
        /**
         * Exception thrown in making HTTP request. This is never null, 
         * but it may not be {@link Optional#isPresent() present}.
         */
        public final Optional<Exception> exception;

        public ResponseData(int code, byte[] data) {
            this(code, data, null);
        }
        
        private ResponseData(int code, byte[] data, @Nullable Exception exception) {
            this.code = code;
            this.data = Preconditions.checkNotNull(data);
            this.exception = Optional.fromNullable(exception);
        }

        public ResponseData(Exception exception) {
            this(0, new byte[0], Preconditions.checkNotNull(exception));
        }

        public ResponseData(int code, Exception exception) {
            this(code, new byte[0], Preconditions.checkNotNull(exception));
        }
        
        public boolean isNonError() {
            return nonErrorCodes.contains(code);
        }

        @Override
        public String toString() {
            return "ResponseData{" 
                    + "code=" + code 
                    + ", data.length=" + data.length 
                    + ", hasException=" + exception.isPresent() + '}';
        }
        
        
    }
    /**
     * Default implementation of a response handler that creates a 
     * response data object.
     * @see ResponseData
     */
    public static class ResponseDataResponseHandler implements ResponseHandler<ResponseData> {

        @Override
        public ResponseData handleResponse(HttpResponse response) {
            ByteArrayOutputStream out = new ByteArrayOutputStream(100 * 1024);
            HttpEntity entity = response.getEntity();
            ResponseData responseData;
            if (entity == null) { // treat same as no data
                return new ResponseData(response.getStatusLine().getStatusCode(), new byte[0]);
            } else {
                int statusCode = response.getStatusLine().getStatusCode();
                try (InputStream in = entity.getContent()) {
                    ByteStreams.copy(in, out);
                    responseData = new ResponseData(statusCode, out.toByteArray());
                } catch (IOException e) {
                    responseData = new ResponseData(statusCode, e);
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
        return new HttpRequester();
    }
    
    /**
     * Creates and returns a new requester instance configured with a timeout.
     * @param timeoutMs the timeout, in milliseconds
     * @return the requester instance
     */
    public static HttpRequester newRequesterWithTimeout(int timeoutMs) {
        return new HttpRequester(timeoutMs);
    }
    
    /**
     * Class that represents an actor that makes an HTTP request.
     */
    public static class HttpRequester {
        
        /**
         * Default timeout (infinite).
         */
        public static final int DEFAULT_TIMEOUT_MS = 0;
        
        private int timeoutMs;

        public HttpRequester() {
            this(DEFAULT_TIMEOUT_MS);
        }

        public HttpRequester(int timeoutMs) {
            this.timeoutMs = timeoutMs;
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
        
        protected RequestConfig buildRequestConfig() {
            RequestConfig config = RequestConfig.custom()
                    .setSocketTimeout(timeoutMs) // 0 = infinite
                    .setCircularRedirectsAllowed(true) // MC: this was in the original SOCIAL-ID Flickr code, but is it really wise?
                    .build();
            return config;
        }
        
        protected HttpRequestBase createRequest(URI imageUri) {
            return new HttpGet(imageUri);
        }
        
        protected HttpClient createHttpClient(URI requestedUri) {
            HttpClient client = HttpClients.createDefault();
            return client;
        }
        
        /**
         * Sends a request to download a given URI.
         * @param uri the URI
         * @return the response data
         */
        public ResponseData retrieve(URI uri) {
            HttpClient client = createHttpClient(uri);
            HttpRequestBase request = createRequest(uri);
            request.setConfig(buildRequestConfig());
            ResponseData responseData;
            try {
                responseData = client.execute(request, new ResponseDataResponseHandler());
            } catch (IOException ex) {
                responseData = new ResponseData(ex);
            }
            return responseData;
        }
        
    }
    
    
}
