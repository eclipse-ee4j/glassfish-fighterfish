# Build

```
mvn clean package
```

It will create `target/fighterfish-sample-embeddedgf-provisionerwebapp.war`

# Prerequisite

You must have installed some other container like WLS or Tomcat and deployed
 the earlier webapp called `fighterfish-sample-embeddedgf-webosgibridge.war`.

You must also add `simple-glassfish-api.jar` to the system classpath of the
 host container. This step is required if you want other applications deployed
 in the host container to use GlassFish.

You must define a property called `com.sun.aas.installRoot` in system or in
 osgi.properties file to point to your glassfish installation. You can also
 decide to configure this web app using a runtime deployment descriptor or
 deployment plan if you chose to. Look at the environment entry called `gfHome`
 in this webapp.

# How to use this to embed GlassFish inside another web container

Deploy `target/fighterfish-sample-embeddedgf-provisionerwebapp.war` to your
 application server.

This web app does the following:

- Gets hold of the OSGi framework created by earlier web app and provisions
 GlassFish modules inside that.
- It then binds the `org.glassfish.embeddable.GlassFish` object to JNDI under
 `java:global/glassfish-instance`.
- Now you can run commands like "nadmin version" from another terminal to verify
 GlassFish is actually started. You can also deploy other applications in the
 host container and access GlassFish using GlassFish embeddable API.