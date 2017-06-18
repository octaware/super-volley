SuperVolley Converters
===================

SuperVolley ships with support for OkHttp's `RequestBody` and `ResponseBody` types but the library is
content-format agnostic. The child modules contained herein are additional converters for other
popular formats.

To use, supply an instance of your desired converter when building your `SuperVolley` instance.

```java
SuperVolley volley = new SuperVolley.Builder()
    .baseUrl("https://api.example.com")
    .addConverterFactory(GsonConverterFactory.create())
    .build();
```

Inspired by [Square Retrofit Converters][1]

[1]: https://github.com/square/retrofit/tree/master/retrofit-converters