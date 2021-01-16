# CXF Extension for Quarkus
<!-- ALL-CONTRIBUTORS-BADGE:START - Do not remove or modify this section -->
[![All Contributors](https://img.shields.io/badge/all_contributors-3-orange.svg?style=flat-square)](#contributors-)
<!-- ALL-CONTRIBUTORS-BADGE:END -->

[![Build](https://github.com/quarkiverse/quarkiverse-cxf/workflows/Build/badge.svg)](https://github.com/quarkiverse/quarkiverse-cxf/actions?query=workflow%3ABuild)
[![Maven Central](https://img.shields.io/maven-central/v/io.quarkiverse.cxf/quarkus-cxf.svg?label=Maven%20Central)](https://search.maven.org/artifact/io.quarkiverse.cxf/quarkus-cxf)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

SOAP (Simple Object Access Protocol) is a normalized exchange protocol based on XML, predating the era of REST services.

This extension enables you to develop web services that consume and produce SOAP payloads using the [Apache CXF](http://cxf.apache.org/) libraries.

  - [Contributors](#contributors)
  - [Configuration](#configuration)
  - [Creating a SOAP Web service](#creating-a-soap-web-service)
  - [Creating a SOAP Client](#creating-a-soap-client)
  - [Native Mode Limitations](#native-mode-limitations)


## Contributors ✨

Thanks goes to these wonderful people ([emoji key](https://allcontributors.org/docs/en/emoji-key)):

<!-- ALL-CONTRIBUTORS-LIST:START - Do not remove or modify this section -->
<!-- prettier-ignore-start -->
<!-- markdownlint-disable -->
<table>
  <tr>
    <td align="center"><a href="https://github.com/shumonsharif"><img src="https://avatars2.githubusercontent.com/u/13334073?v=4" width="100px;" alt=""/><br /><sub><b>shumonsharif</b></sub></a><br /><a href="https://github.com/quarkiverse/quarkiverse-cxf/commits?author=shumonsharif" title="Code">💻</a> <a href="#maintenance-shumonsharif" title="Maintenance">🚧</a></td>
    <td align="center"><a href="https://github.com/dufoli"><img src="https://avatars0.githubusercontent.com/u/202057?v=4" width="100px;" alt=""/><br /><sub><b>dufoli</b></sub></a><br /><a href="https://github.com/quarkiverse/quarkiverse-cxf/commits?author=dufoli" title="Code">💻</a> <a href="#maintenance-dufoli" title="Maintenance">🚧</a></td>
    <td align="center"><a href="https://github.com/dufgui"><img src="https://avatars0.githubusercontent.com/u/237211?v=4" width="100px;" alt=""/><br /><sub><b>dufgui</b></sub></a><br /><a href="https://github.com/quarkiverse/quarkiverse-cxf/commits?author=dufgui" title="Code">💻</a> <a href="#maintenance-dufgui" title="Maintenance">🚧</a></td>
  </tr>
</table>

<!-- markdownlint-enable -->
<!-- prettier-ignore-end -->
<!-- ALL-CONTRIBUTORS-LIST:END -->

This project follows the [all-contributors](https://github.com/all-contributors/all-contributors) specification. Contributions of any kind welcome!

## Configuration

After configuring `quarkus BOM`:

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>io.quarkus</groupId>
            <artifactId>quarkus-bom</artifactId>
            <version>${insert.newest.quarkus.version.here}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

You can just configure the `quarkus-cxf` extension by adding the following dependency:

```xml
<dependency>
    <groupId>io.quarkiverse.cxf</groupId>
    <artifactId>quarkus-cxf</artifactId>
    <version>${latest.release.version}</version>
</dependency>
```
<!--
***NOTE:*** You can bootstrap a new application quickly by using [code.quarkus.io](https://code.quarkus.io) and choosing `quarkus-cxf`.
-->

## Creating a SOAP Web service

In this example, we will create an application to manage a list of fruits.

First, let's create the `Fruit` bean as follows:

```java
package org.acme.cxf;

import java.util.Objects;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlType(name = "Fruit")
@XmlRootElement
public class Fruit {

    private String name;

    private String description;

    public Fruit() {
    }

    public Fruit(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    @XmlElement
    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    @XmlElement
    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Fruit)) {
            return false;
        }

        Fruit other = (Fruit) obj;

        return Objects.equals(other.getName(), this.getName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.getName());
    }
}
```

Now, create the `org.acme.cxf.FruitWebService` class as follows:

```java
package org.acme.cxf;

import java.util.Set;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;

@WebService
public interface FruitWebService {

    @WebMethod
    Set<Fruit> list();

    @WebMethod
    Set<Fruit> add(Fruit fruit);

    @WebMethod
    Set<Fruit> delete(Fruit fruit);
}
```

Then, create the `org.acme.cxf.FruitWebServiceImpl` class as follows:

```java
package org.acme.cxf;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Set;

import javax.jws.WebService;

@WebService(endpointInterface = "org.acme.cxf.FruitWebService")
public class FruitWebServiceImpl implements FruitWebService {

    private Set<Fruit> fruits = Collections.newSetFromMap(Collections.synchronizedMap(new LinkedHashMap<>()));

    public FruitWebServiceImpl() {
        fruits.add(new Fruit("Apple", "Winter fruit"));
        fruits.add(new Fruit("Pineapple", "Tropical fruit"));
    }

    @Override
    public Set<Fruit> list() {
        return fruits;
    }

    @Override
    public Set<Fruit> add(Fruit fruit) {
        fruits.add(fruit);
        return fruits;
    }

    @Override
    public Set<Fruit> delete(Fruit fruit) {
        fruits.remove(fruit);
        return fruits;
    }
}
```

The implementation is pretty straightforward and you just need to define your endpoints using the `application.properties`.

```properties
quarkus.cxf.path=/cxf
quarkus.cxf.endpoint."/fruit".implementor=org.acme.cxf.FruitWebServiceImpl
```

The following sample curl command can be used to test your Fruit service.

```
curl -X POST "http://localhost:8080/cxf/fruit" \
 -H 'Content-Type: text/xml' \
 -H 'SOAPAction:' \
 -d '
 <soapenv:Envelope 
 xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
 xmlns:cxf="http://cxf.acme.org/">
   <soapenv:Header/>
   <soapenv:Body>
      <cxf:list/>
   </soapenv:Body>
</soapenv:Envelope>'
```

## Creating a SOAP Client

In order to support a SOAP client, register the endpoint URL and the service endpoint interface (same as the server) with the following configuration:

```properties
quarkus.cxf.endpoint."/fruit".client-endpoint-url=http://localhost:8080/
quarkus.cxf.endpoint."/fruit".service-interface=org.acme.cxf.FruitWebService
```

Then inject the client as shown below to use it. Note that the Quarkus container must instantiate the client, ie. the client must be injected in a class which is managed and instantiated by the container. If you need a main, you can use the QuarkusMain annotation (cf https://quarkus.io/guides/lifecycle).

```java
public class MySoapClient {

    @Inject
    FruitWebService clientService;

    public int getCount() {
        return clientService.count();
    }
}
```

If MySoapClient is not handled by the container but is instantiated by a main you have to use:

```java
public class MySoapClient {

    FruitWebService clientService = CDI.current().select(FruitWebService.class).get();;

    public int getCount() {
        return clientService.count();
    }
}
```


## Native Mode Limitations

- The native mode is supported (java 8 and java 11 are both supported).

## Advanced configuration

Interceptors and features can be added to client/server thanks to :
- annotation
- configuration file

On cxf website,a full list of [Cxf embeded interceptors](https://cxf.apache.org/docs/interceptors.html) and [Cxf embeded features](https://cxf.apache.org/docs/featureslist.html) are available,
but you can implement a custom one.

### Annotation

Annotations can be put on the service interface or implementor class.

```java
@org.apache.cxf.feature.Features (features = {"org.apache.cxf.feature.LoggingFeature"})
@org.apache.cxf.interceptor.InInterceptors (interceptors = {"com.example.Test1Interceptor" })
@org.apache.cxf.interceptor.InFaultInterceptors (interceptors = {"com.example.Test2Interceptor" })
@org.apache.cxf.interceptor.OutInterceptors (interceptors = {"com.example.Test1Interceptor" })
@org.apache.cxf.interceptor.InFaultInterceptors (interceptors = {"com.example.Test2Interceptor","com.example.Test3Intercetpor" })
@WebService(endpointInterface = "org.apache.cxf.javascript.fortest.SimpleDocLitBare",
            targetNamespace = "uri:org.apache.cxf.javascript.fortest")
public class SayHiImplementation implements SayHi {
   public long sayHi(long arg) {
       return arg;
   }
   //...
}
```


### Configuration

Else, you can use quarkus configuration file.
Feature and interceptor will be try with CDI first and if no bean found,
the constructor with no param will be used.
```properties
quarkus.cxf.endpoint."/greeting".features=org.apache.cxf.feature.LoggingFeature
quarkus.cxf.endpoint."/greeting".in-interceptors=com.example.MyInterceptor
quarkus.cxf.endpoint."/greeting".out-interceptors=com.example.MyInterceptor
quarkus.cxf.endpoint."/greeting".in-fault-interceptors=com.example.MyInterceptor
quarkus.cxf.endpoint."/greeting".out-fault-interceptors=com.example.MyInterceptor
```
