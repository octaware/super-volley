Jackson Converter
=================

A `Converter` which uses [Jackson][1] for serialization to and from JSON.

A default `ObjectMapper` instance will be created or one can be configured and passed to the
`JacksonConverterFactory` construction to further control the serialization.


Download
--------

Download [the latest AAR][2] or grab via Maven:
```xml
<dependency>
  <groupId>com.android.supervolley</groupId>
  <artifactId>jackson-converter</artifactId>
  <version>1.0.0</version>
  <type>pom</type>
</dependency>
```
or Gradle:
```groovy
compile 'com.android.supervolley:jackson-converter:1.0.0'
```

 [1]: http://wiki.fasterxml.com/JacksonHome
 [2]: https://bintray.com/octaware/super-volley/download_file?file_path=com%2Fandroid%2Fsupervolley%2Fjackson%2F1.0.0%2Fjackson-1.0.0.aar