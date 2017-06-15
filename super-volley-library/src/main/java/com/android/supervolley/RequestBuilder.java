package com.android.supervolley;

import com.android.supervolley.BaseRequest;
import com.android.volley.Request;
import com.android.volley.RetryPolicy;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import okhttp3.FormBody;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okio.Buffer;
import okio.BufferedSink;

final class RequestBuilder {
    private static final char[] HEX_DIGITS =
            {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
    private static final String PATH_SEGMENT_ALWAYS_ENCODE_SET = " \"<>^`{}|\\?#";

    private int method;

    private okhttp3.HttpUrl baseUrl;
    private String relativeUrl;
    private okhttp3.HttpUrl.Builder urlBuilder;

    private Map<String, String> headers;
    private RetryPolicy retryPolicy;
    private Request.Priority priority;
    private String tag;
    private boolean shouldCache;

    private MediaType contentType;

    private boolean hasBody;
    private MultipartBody.Builder multipartBuilder;
    private FormBody.Builder formBuilder;
    private RequestBody body;

    RequestBuilder() {
        headers = new HashMap<>();
        shouldCache = false;
        hasBody = false;
    }

    RequestBuilder method(String method) {
        this.method = MethodHelper.translateMethod(method);
        return this;
    }

    RequestBuilder baseUrl(okhttp3.HttpUrl baseUrl) {
        this.baseUrl = baseUrl;
        return this;
    }

    RequestBuilder relativeUrl(String relativeUrl) {
        this.relativeUrl = relativeUrl;
        return this;
    }

    RequestBuilder headers(Map<String, String> headers) {
        this.headers = headers;
        return this;
    }

    RequestBuilder contentType(MediaType contentType) {
        this.contentType = contentType;
        return this;
    }

    RequestBuilder hasBody(boolean hasBody) {
        this.hasBody = hasBody;
        return this;
    }

    RequestBuilder isFormEncoded(boolean isFormEncoded) {
        if (isFormEncoded) {
            // Will be set to 'body' in 'build'.
            formBuilder = new FormBody.Builder();
        }
        return this;
    }

    RequestBuilder isMultipart(boolean isMultipart) {
        if (isMultipart) {
            // Will be set to 'body' in 'build'.
            multipartBuilder = new MultipartBody.Builder();
            multipartBuilder.setType(MultipartBody.FORM);
        }
        return this;
    }

    RequestBuilder retryPolicy(RetryPolicy retryPolicy) {
        this.retryPolicy = retryPolicy;
        return this;
    }

    RequestBuilder priority(Request.Priority priority) {
        this.priority = priority;
        return this;
    }

    RequestBuilder tag(String tag) {
        this.tag = tag;
        return this;
    }

    RequestBuilder shouldCache(boolean shouldCache) {
        this.shouldCache = shouldCache;
        return this;
    }

    void setRelativeUrl(Object relativeUrl) {
        if (relativeUrl == null) throw new NullPointerException("@Url parameter is null.");
        this.relativeUrl = relativeUrl.toString();
    }

    void addHeader(String name, String value) {
        if ("Content-Type".equalsIgnoreCase(name)) {
            MediaType type = MediaType.parse(value);
            if (type == null) {
                throw new IllegalArgumentException("Malformed content type: " + value);
            }
            contentType = type;
        } else {
            headers.put(name, value);
        }
    }

    void addPathParam(String name, String value, boolean encoded) {
        if (relativeUrl == null) {
            // The relative URL is cleared when the first query parameter is set.
            throw new AssertionError();
        }
        relativeUrl = relativeUrl.replace("{" + name + "}", canonicalizeForPath(value, encoded));
    }

    private static String canonicalizeForPath(String input, boolean alreadyEncoded) {
        int codePoint;
        for (int i = 0, limit = input.length(); i < limit; i += Character.charCount(codePoint)) {
            codePoint = input.codePointAt(i);
            if (codePoint < 0x20 || codePoint >= 0x7f
                    || PATH_SEGMENT_ALWAYS_ENCODE_SET.indexOf(codePoint) != -1
                    || (!alreadyEncoded && (codePoint == '/' || codePoint == '%'))) {
                // Slow path: the character at i requires encoding!
                Buffer out = new Buffer();
                out.writeUtf8(input, 0, i);
                canonicalizeForPath(out, input, i, limit, alreadyEncoded);
                return out.readUtf8();
            }
        }

        // Fast path: no characters required encoding.
        return input;
    }

    private static void canonicalizeForPath(Buffer out, String input, int pos, int limit,
                                            boolean alreadyEncoded) {
        Buffer utf8Buffer = null; // Lazily allocated.
        int codePoint;
        for (int i = pos; i < limit; i += Character.charCount(codePoint)) {
            codePoint = input.codePointAt(i);
            if (alreadyEncoded
                    && (codePoint == '\t' || codePoint == '\n' || codePoint == '\f' || codePoint == '\r')) {
                // Skip this character.
            } else if (codePoint < 0x20 || codePoint >= 0x7f
                    || PATH_SEGMENT_ALWAYS_ENCODE_SET.indexOf(codePoint) != -1
                    || (!alreadyEncoded && (codePoint == '/' || codePoint == '%'))) {
                // Percent encode this character.
                if (utf8Buffer == null) {
                    utf8Buffer = new Buffer();
                }
                utf8Buffer.writeUtf8CodePoint(codePoint);
                while (!utf8Buffer.exhausted()) {
                    int b = utf8Buffer.readByte() & 0xff;
                    out.writeByte('%');
                    out.writeByte(HEX_DIGITS[(b >> 4) & 0xf]);
                    out.writeByte(HEX_DIGITS[b & 0xf]);
                }
            } else {
                // This character doesn't need encoding. Just copy it over.
                out.writeUtf8CodePoint(codePoint);
            }
        }
    }

    void addQueryParam(String name, String value, boolean encoded) {
        if (relativeUrl != null) {
            // Do a one-time combination of the built relative URL and the base URL.
            urlBuilder = baseUrl.newBuilder(relativeUrl);
            if (urlBuilder == null) {
                throw new IllegalArgumentException(
                        "Malformed URL. Base: " + baseUrl + ", Relative: " + relativeUrl);
            }
            relativeUrl = null;
        }

        if (encoded) {
            urlBuilder.addEncodedQueryParameter(name, value);
        } else {
            urlBuilder.addQueryParameter(name, value);
        }
    }

    void addFormField(String name, String value, boolean encoded) {
        if (encoded) {
            formBuilder.addEncoded(name, value);
        } else {
            formBuilder.add(name, value);
        }
    }

    void addPart(Headers headers, RequestBody body) {
        multipartBuilder.addPart(headers, body);
    }

    void addPart(MultipartBody.Part part) {
        multipartBuilder.addPart(part);
    }

    void setBody(RequestBody body) {
        this.body = body;
    }

    Request build() {
        okhttp3.HttpUrl url;
        okhttp3.HttpUrl.Builder urlBuilder = this.urlBuilder;
        if (urlBuilder != null) {
            url = urlBuilder.build();
        } else {
            // No query parameters triggered builder creation, just combine the relative URL and base URL.
            url = baseUrl.resolve(relativeUrl);
            if (url == null) {
                throw new IllegalArgumentException(
                        "Malformed URL. Base: " + baseUrl + ", Relative: " + relativeUrl);
            }
        }

        RequestBody body = this.body;
        if (body == null) {
            // Try to pull from one of the builders.
            if (formBuilder != null) {
                body = formBuilder.build();
            } else if (multipartBuilder != null) {
                body = multipartBuilder.build();
            } else if (hasBody) {
                // Body is absent, make an empty body.
                body = RequestBody.create(null, new byte[0]);
            }
        }

        BaseRequest request = new BaseRequest(method, url.toString());
        request.setPriority(priority);
        request.setRetryPolicy(retryPolicy);
        request.setShouldCache(shouldCache);
        request.setTag(tag);
        request.addHeaders(headers);

        MediaType contentType = this.contentType;
        if (contentType != null) {
            if (body != null) {
                body = new ContentTypeOverridingRequestBody(body, contentType);
            } else {
                request.addHeader("Content-Type", contentType.toString());
            }
        }

        if (body != null) {
            request.addBody(body);
        }

        return request;
    }

    private static class ContentTypeOverridingRequestBody extends RequestBody {
        private final RequestBody delegate;
        private final MediaType contentType;

        ContentTypeOverridingRequestBody(RequestBody delegate, MediaType contentType) {
            this.delegate = delegate;
            this.contentType = contentType;
        }

        @Override
        public MediaType contentType() {
            return contentType;
        }

        @Override
        public long contentLength() throws IOException {
            return delegate.contentLength();
        }

        @Override
        public void writeTo(BufferedSink sink) throws IOException {
            delegate.writeTo(sink);
        }
    }
}
