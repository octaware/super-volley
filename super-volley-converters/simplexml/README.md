Simple XML Converter
====================

A `Converter` which uses [SimpleXML][1] for XML serialization.

A default `Serializer` instance will be created or one can be configured and passed to the
`SimpleXMLConverter` construction to further control the serialization.


Android
-------

Simple depends on artifacts which are already provided by the Android platform. When specifying as
a Maven or Gradle dependency, exclude the following transitive dependencies: `stax:stax-api`,
`stax:stax`, and `xpp3:xpp3`.


Download
--------

Download [the latest AAR][2] or grab via Maven:
```xml
<dependency>
  <groupId>com.android.supervolley</groupId>
  <artifactId>xml-converter</artifactId>
  <version>1.0.0</version>
  <type>pom</type>
</dependency>
```
or Gradle:
```groovy
compile 'com.android.supervolley:xml-converter:1.0.0'
```

 [1]: http://simple.sourceforge.net/
 [2]: https://bintray.com/octaware/super-volley/download_file?file_path=com%2Fandroid%2Fsupervolley%2Fsimplexml%2F1.0.0%2Fsimplexml-1.0.0.aar

