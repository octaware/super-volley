SuperVolley Adapters
=================

SuperVolley ships with a default adapter for executing `Call` instances. The child modules contained
herein are additional adapters for other popular execution mechanisms.

To use, supply an instance of your desired adapter when building your `SuperVolley` instance.

```java
SuperVolley volley = new SuperVolley.Builder()
    .baseUrl("https://api.example.com")
    .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
    .build();
```

Inspired by [Square Retrofit Adapter][1]

[1]: https://github.com/square/retrofit/tree/master/retrofit-adapters