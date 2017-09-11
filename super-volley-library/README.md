SuperVolley
========

Type-safe HTTP client for Android inspired by [Retrofit][1] of [Square, Inc.][2] using [Volley][3] of Google as a main networking library.


Introduction
--------

SuperVolley turns your HTTP API into a Java interface.
```java
public interface GitHubService {
  @GET("users/{user}/repos")
  Call<List<Repo>> listRepos(@Path("user") String user);
}
```
The SuperVolley class generates an implementation of the GitHubService interface.

```java
SuperVolley volley = new SuperVolley.Builder()
                .baseUrl(Constants.API_URL)
                .build();
```

```java
GitHubService service = volley.create(GitHubService.class);
```
Each Call from the created GitHubService can make a synchronous or asynchronous HTTP request to the remote webserver.

```java
Call<List<Repo>> repos = service.listRepos("test");
```
Use annotations to describe the HTTP request:

- URL parameter replacement and query parameter support
- Object conversion to request body (e.g., JSON, protocol buffers)
- Multipart request body and file upload


API Declaration
--------

Annotations on the interface methods and its parameters indicate how a request will be handled.

#### REQUEST METHOD

Every method must have an HTTP annotation that provides the request method and relative URL. There are five built-in annotations: GET, POST, PUT, DELETE, and HEAD. The relative URL of the resource is specified in the annotation.

```java
@GET("users/list")
```
You can also specify query parameters in the URL.

```java
@GET("users/list?sort=desc")
```

##### URL MANIPULATION

A request URL can be updated dynamically using replacement blocks and parameters on the method. A replacement block is an alphanumeric string surrounded by { and }. A corresponding parameter must be annotated with @Path using the same string.

```java
@GET("group/{id}/users")
Call<List<User>> groupList(@Path("id") int groupId);
```
Query parameters can also be added.

```java
@GET("group/{id}/users")
Call<List<User>> groupList(@Path("id") int groupId, @Query("sort") String sort);
```
For complex query parameter combinations a Map can be used.

```java
@GET("group/{id}/users")
Call<List<User>> groupList(@Path("id") int groupId, @QueryMap Map<String, String> options);
```

##### REQUEST BODY

An object can be specified for use as an HTTP request body with the @Body annotation.

```java
@POST("users/new")
Call<User> createUser(@Body User user);
```
The object will also be converted using a converter specified on the Retrofit instance. If no converter is added, only RequestBody can be used.

##### FORM ENCODED AND MULTIPART

Methods can also be declared to send form-encoded and multipart data.

Form-encoded data is sent when @FormUrlEncoded is present on the method. Each key-value pair is annotated with @Field containing the name and the object providing the value.

```java
@FormUrlEncoded
@POST("user/edit")
Call<User> updateUser(@Field("first_name") String first, @Field("last_name") String last);
```
Multipart requests are used when @Multipart is present on the method. Parts are declared using the @Part annotation.

```java
@Multipart
@PUT("user/photo")
Call<User> updateUser(@Part("photo") RequestBody photo, @Part("description") RequestBody description);
```
Multipart parts use one of SuperVolley's converters or they can implement RequestBody to handle their own serialization.

##### HEADER MANIPULATION

You can set static headers for a method using the @Headers annotation.

```java
@Headers("Cache-Control: max-age=640000")
@GET("widget/list")
Call<List<Widget>> widgetList();

@Headers({
    "Accept: application/vnd.github.v3.full+json",
    "User-Agent: Sample-App"
})
@GET("users/{username}")
Call<User> getUser(@Path("username") String username);
```
Note that headers do not overwrite each other. All headers with the same name will be included in the request.

A request Header can be updated dynamically using the @Header annotation. A corresponding parameter must be provided to the @Header. If the value is null, the header will be omitted. Otherwise, toString will be called on the value, and the result used.

```java
@GET("user")
Call<User> getUser(@Header("Authorization") String authorization)
```
Headers that need to be added to every request can be specified using an OkHttp interceptor.

##### Caching response

```java
@GET("user")
@CacheResponse
Call<User> getUser(@Header("Authorization") String authorization)
```
Use this annotation on a service method when you want to cache the response in your caching mechanism.
Be sure you set a caching mechanism type on your SuperVolley instance. (e.g MemoryBasedCache, DiskBasedCache, or any other custom Cache).

##### Automatic retries

```java
@GET("user")
@Retries(2)
Call<User> getUser(@Header("Authorization") String authorization)
```
Number of retries if the request fails the first time.

##### Prioritization

```java
@GET("user")
@Priority(Request.Priority.HIGH)
Call<User> getUser(@Header("Authorization") String authorization)
```

##### Tag

```java
@GET("user")
@Tag("getUser")
Call<User> getUser(@Header("Authorization") String authorization)

@PUT("user")
@Tag("updateUser")
Call<User> updateUser(@Header("Authorization") String authorization, @Body User user)
```
Note if you have two request with the same relative url and plan to cancel one of them, in this case it's good to set a custom tag. This can be used to cancel all requests with that specific tag.

#### SYNCHRONOUS VS. ASYNCHRONOUS

Call instances can be executed either synchronously or asynchronously. Each instance can only be used once, but calling clone() will create a new instance that can be used.

On Android, callbacks will be executed on the main thread. On the JVM, callbacks will happen on the same thread that executed the HTTP request.

#### SuperVolley Configuration

SuperVolley is the class through which your API interfaces are turned into callable objects. By default, SuperVolley will give you sane defaults for your platform but it allows for customization.


CONVERTERS
--------

By default, SuperVolley can only deserialize HTTP bodies into OkHttp's ResponseBody type and it can only accept its RequestBody type for @Body.

Converters can be added to support other types. Three sibling modules adapt popular serialization libraries for your convenience.

Gson: com.android.super-volley:gson-converter
Jackson: com.android.super-volley:jackson-converter
Simple XML: com.android.super-volley:xml-converter

Here's an example of using the `GsonConverterFactory` class to generate an implementation of the GitHubService interface which uses Gson for its deserialization.

```
SuperVolley volley = new SuperVolley.Builder()
    .baseUrl("https://api.github.com")
    .addConverterFactory(GsonConverterFactory.create())
    .build();

GitHubService service = volley.create(GitHubService.class);
```

###CUSTOM CONVERTERS

If you need to communicate with an API that uses a content-format that SuperVolley does not support out of the box (e.g. YAML, txt, custom format) or you wish to use a different library to implement an existing format, you can easily create your own converter. Create a class that extends the Converter.Factory` class and pass in an instance when building your adapter.

Download
--------

Download [the latest AAR][4] or grab via Maven:
```xml
<dependency>
  <groupId>com.android.supervolley</groupId>
  <artifactId>super-volley</artifactId>
  <version>1.0.0</version>
  <type>pom</type>
</dependency>
```
or Gradle:
```groovy
compile 'com.android.supervolley:super-volley:1.0.2'
```


SuperVolley requires at minimum Java 7 or Android 4.0.


License
=======

    Copyright 2017

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.


 [1]: https://github.com/square/retrofit
 [2]: https://github.com/square
 [3]: https://github.com/google/volley
 [4]: https://bintray.com/octaware/super-volley/download_file?file_path=com%2Fandroid%2Fsupervolley%2Fsuper-volley-library%2F1.0.0%2Fsuper-volley-library-1.0.0.aar
 