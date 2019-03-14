# Changelog

All notable changes to this project will be documented in this file.

## [Unreleased]

### Improvements

### Fixes

### Notes

## [3.0.0]

## Notes

Major changes:

- Workspace builds as a single Maven reactor with a single version. Using the
 next major version not used by any of the modules: `3.0.0`
- Unique parent pom, improved version management
- Support JDK8
- Fixing and enabling all tests
- Testing against latest GlassFish 5.1.0 (EE4J_8)
- Add Jenkins multi-branch pipeline
- Fix copyrights and enforce copyright check in the pipeline build
- Fix spotbugs high priority errors and enforce spotbugs check in the pipeline
 build
- Fix checkstyle in all modules and enforce checkstyle check in the pipeline
 build
- Removed testing on Equinox and focus on testing GlassFish exclusively
- Override local fighterfish modules when testing on GlassFish
- Update to OSGi 6.0
- Update JavaEE APIs to 8.0 and to JakartaEE artifacts

## osgi-cdi-1.0.6

### Notes

Project donated to Eclipse, first release under Eclipse for the EE4J 8 release.

## osgi-cdi-1.0.5

### Notes

Copyright/License Headers Updated.

## osgi-cdi-1.0.4

### Notes

- CDI 2.0 upgrade.

## osgi-cdi-1.0.3

### Fixes

- [GLASSFISH-18370]: OSGi Services injected with CDI have their exceptions
 wrapped..
- [GLASSFISH-18227]: Removed certain debug messages from INFO level

## osgi-cdi-1.0.2

### Fixes

- [GLASSFISH-16947]: osgi-cdi extension is too verbose

## osgi-cdi-1.0.1

### Fixes

- [GLASSFISH-16741]: dyanamic=false in @OSGiService does not cause bean resolution
 error when there is no matching service
- [GLASSFISH-16196]: The extension checks for conditions where injection is
 requested into non-OSGi archives and logs a user-friendly error.

## osgi-ee-resources-1.0.3

### Notes

Project donated to Eclipse, first release under Eclipse for the EE4J 8 release.

## osgi-ee-resources-1.0.3

### Notes

Copyright/License Headers Updated.

## osgi-ee-resources-1.0.2

### Fixes

- [GLASSFISH-17253]: osgi-ee-resources module does not work with GlassFish 4.0
 builds
- [GLASSFISH-15787]: Administered resource's life cycle mapping to corresponding
 OSGi service has some issues

## osgi-ee-resources-1.0.1

### Fixes

- [GLASSFISH-18839]: Extenders are getting stopped with invalid BundleContext
- [GLASSFISH-18846]: Avoid use of deprecated HK2 API

## osgi-ejb-container-1.0.5

### Notes

Project donated to Eclipse, first release under Eclipse for the EE4J 8 release.

## osgi-ejb-container-1.0.4

### Notes

Copyright/License Headers Updated.

## osgi-ejb-container-1.0.3

### Fixes

- [GLASSFISH-18840]: Extenders are getting stopped with invalid BundleContext

## osgi-ejb-container-1.0.2

### Fixes

- [GLASSFISH-18810]: [osgi/ejb] Adapt to binary incompatible EJB DOL changes made
 by EJB Team while splitting EJB DOL

## osgi-ejb-container-1.0.1

### Fixes

- [GLASSFISH-18589]: [osgi/ejb] return correct ArchiveType for DOL processing to
 happen with new DOL

## osgi-http-1.0.8

### Notes

Project donated to Eclipse, first release under Eclipse for the EE4J 8 release.

## osgi-http-1.0.7

### Notes

Copyright/License headers Updated.

## osgi-http-1.0.6

### Fixes

- [GLASSFISH-18833]: [osgi/http] Avoid use of deprecated HK2 API
- [GLASSFISH-18842]: Extenders are stopped with invalid bundle context

## osgi-http-1.0.5

### Fixes

- [GLASSFISH-18492]: [osgi/http] InitParams for Servlets installed via OSGi using
 HttpService are not correctly processed by web container

## osgi-http-1.0.4

### Fixes

- [GLASSFISH-16880]: NPE in Activator.stop() if start() has failed

## osgi-http-1.0.3

### Fixes

- [GLASSFISH-16764]: request.getSession().getServletContext() should return the
 same OSGiServletContext that's used during setAttrbute().

## osgi-http-1.0.2

### Fixes

- [GLASSFISH-16761]: Make osg-http use lazy activation policy

## osgi-http-1.0.1

### Fixes

- [GLASSFISH-16538]: Switch to using Grizzly API reflectively to be able to use
 in both Grizzly 1.9 and 2.0.

## osgi-javaee-base-1.0.9

### Notes

Project donated to Eclipse, first release under Eclipse for the EE4J 8 release.

## osgi-javaee-base-1.0.8

### Notes

Copyright/License Headers Updated.

## osgi-javaee-base-1.0.7

### Fixes

- [GLASSFISH-19727]: wab expanded directory can not be cleaned after accessing
 the wab resource

## osgi-javaee-base-1.0.6

### Fixes

- [GLASSFISH-19261]: NPE(Specified service reference cannot be null) during
 server shutdown is preventing clean up to complete
- [GLASSFISH-20023]: Wrong property is used to configure OSGi application
 deployment timeout
- [GLASSFISH-19688]: Don't explode OSGi applications in tmpdir
- [GLASSFISH-19662]: wab fragment deployed using deploy command is not working

## osgi-javaee-base-1.0.5

### Fixes

- [GLASSFISH-19092]: Exceptions are not always logged
- [GLASSFISH-19093]: Avoid use deprecated Habitat.getComponent API

## osgi-javaee-base-1.0.4

### Fixes

- [GLASSFISH-18887]: ExtenderManager deadlocks if glassfish is immediately
 restarted in the same VM

## osgi-javaee-base-1.0.3

### Fixes

- [GLASSFISH-18838]: Avoid use of deprecated HK2 API

## osgi-javaee-base-1.0.2

### Fixes

- [GLASSFISH-18159]: Fix added to take care potential deadlock in
 osgi-javaee-container due to race condition between deployer and undeployer
 threads.

## osgi-javaee-base-1.0.1

### Fixes

- [GLASSFISH-16477]: Add a work around for FELIX-2935 in our code so that we
 don't try to add duplicate entries in jars.

## osgi-jdbc-1.0.3

### Notes

Project donated to Eclipse, first release under Eclipse for the EE4J 8 release.

## osgi-jdbc-1.0.2

### Notes

Copyright/License Headers Updated.

## osgi-jdbc-1.0.1

### Fixes

- [GLASSFISH-18837]: [osgi/jdbc] Avoid use of deprecated HK2 API
- [GLASSFISH-18845]: [osgi/jdbc] Extenders are stopped with invalid bundle
 context

## osgi-jpa-1.0.4

### Notes

Project donated to Eclipse, first release under Eclipse for the EE4J 8 release.

## osgi-jpa-1.0.3

### Notes

Copyright/License Headers Updated.

## osgi-jpa-1.0.2

### Fixes

- [GLASSFISH-18844]: Extenders are stopped with invalid bundle context

## osgi-jpa-1.0.1

### Fixes

- [GLASSFISH-16754]: Occasional "IllegalStateException: Can only register
 services while bundle is active or activating" during deployment of WAB/Ejb
 bundle with JPA

## osgi-jpa-extension-1.0.4

### Notes

Project donated to Eclipse, first release under Eclipse for the EE4J 8 release.

## osgi-jpa-extension-1.0.3

### Notes

Copyright/License Headers Updated.

## osgi-jpa-extension-1.0.2

### Fixes

- [GLASSFISH-18850]: Add hk2-locator metadata file to be compatible with new HK2
(Note it's the same bug as 1.0.1)

## osgi-jpa-extension-1.0.1

### Fixes

- [GLASSFISH-18850]: Add hk2-locator metadata file to be compatible with new HK2

## osgi-jta-1.0.3

### Notes

Project donated to Eclipse, first release under Eclipse for the EE4J 8 release.

## osgi-jta-1.0.2

### Notes

Copyright/License headers Updated.

## osgi-jta-1.0.1

### Fixes

- [GLASSFISH-13383]: change to use portable name for UserTransaction
- [GLASSFISH-18843]: Extenders are stopped with invalid bundle context

## osgi-web-container-2.0.2

### Notes

Project donated to Eclipse, first release under Eclipse for the EE4J 8 release.

## osgi-web-container-2.0.1

### Notes

Copyright/License headers Updated.

## osgi-web-container-2.0.0

### Fixes

- [GLASSFISH-20593]: Regression - Unable to locate static and dynamic resources
 from WAB fragments

## osgi-web-container-1.0.6

### Fixes

- [GLASSFISH-19696]: wab expanded directory does not be cleaned during
 undeployment of WAB due to jar file handle being in use

## osgi-web-container-1.0.5

### Fixes

- [GLASSFISH-18832]: [osgi/web] Avoid use of deprecated HK2 API
- [GLASSFISH-18834]: [osgi/web] NPE in ContextPathCollisionDetector when
 osgi-web-container is stopped without stopping server
- [GLASSFISH-18841]: [osgi/web] Extenders are stopped with invalid bundle context

## osgi-web-container-1.0.4

### Fixes

- [GLASSFISH-18721]: [osgi/web] regression caused by deployment spi
 (ArchiveHandler.getClassPathURIs()) changes

## osgi-web-container-1.0.3

### Fixes

- [GLASSFISH-18590]: [osgi/web] return correct ArchiveType for DOL processing to
 happen with new DOL

## osgi-web-container-1.0.2

### Fixes

- [GLASSFISH-16647]: Static contents from OSGI-INF and OSGI-OPT dirs of WAB must
 not be served
- [GLASSFISH-16640]: WAB deployment event properties are not spec compliant
- [GLASSFISH-16641]: Tolerate when Web-ContextPath header of a WAB does not start
 with '/'
- [GLASSFISH-16642]: Allow decoded URI string when webbundle scheme is used
- [GLASSFISH-16643]: webbundle URL parameter parsing is not spec compliant
- [GLASSFISH-16644]: context path collision detector is not able to handle
 collisions when there are more than 1 bundles waiting to be deployed
- [GLASSFISH-16645]: context path collision detector tries to deploy WABs which
 is already stopped or uninstalled
- [GLASSFISH-16646]: webbundle url handler is not handling signed WARs

## osgi-web-container-1.0.1

### Fixes

- GLASSFISH-16538: Switch to using Grizzly API reflectively to be able to use in
 both Grizzly 1.9 and 2.0.

[Unreleased]: https://github.com/eclipse-ee4j/glassfish-fighterfish/compare/3.0.0...HEAD
[3.0.0]: https://github.com/eclipse-ee4j/glassfish-fighterfish/compare/pre-v3...3.0.0
[GLASSFISH-13383]: https://github.com/eclipse-ee4j/glassfish/issues/13383
[GLASSFISH-15787]: https://github.com/eclipse-ee4j/glassfish/issues/15787
[GLASSFISH-16196]: https://github.com/eclipse-ee4j/glassfish/issues/16196
[GLASSFISH-16477]: https://github.com/eclipse-ee4j/glassfish/issues/16477
[GLASSFISH-16538]: https://github.com/eclipse-ee4j/glassfish/issues/16538
[GLASSFISH-16640]: https://github.com/eclipse-ee4j/glassfish/issues/16640
[GLASSFISH-16641]: https://github.com/eclipse-ee4j/glassfish/issues/16641
[GLASSFISH-16642]: https://github.com/eclipse-ee4j/glassfish/issues/16642
[GLASSFISH-16643]: https://github.com/eclipse-ee4j/glassfish/issues/16643
[GLASSFISH-16644]: https://github.com/eclipse-ee4j/glassfish/issues/16644
[GLASSFISH-16645]: https://github.com/eclipse-ee4j/glassfish/issues/16645
[GLASSFISH-16646]: https://github.com/eclipse-ee4j/glassfish/issues/16646
[GLASSFISH-16647]: https://github.com/eclipse-ee4j/glassfish/issues/16647
[GLASSFISH-16741]: https://github.com/eclipse-ee4j/glassfish/issues/16741
[GLASSFISH-16754]: https://github.com/eclipse-ee4j/glassfish/issues/16754
[GLASSFISH-16761]: https://github.com/eclipse-ee4j/glassfish/issues/16761
[GLASSFISH-16764]: https://github.com/eclipse-ee4j/glassfish/issues/16764
[GLASSFISH-16880]: https://github.com/eclipse-ee4j/glassfish/issues/16880
[GLASSFISH-16947]: https://github.com/eclipse-ee4j/glassfish/issues/16947
[GLASSFISH-17253]: https://github.com/eclipse-ee4j/glassfish/issues/17253
[GLASSFISH-18159]: https://github.com/eclipse-ee4j/glassfish/issues/18159
[GLASSFISH-18227]: https://github.com/eclipse-ee4j/glassfish/issues/18227
[GLASSFISH-18370]: https://github.com/eclipse-ee4j/glassfish/issues/18370
[GLASSFISH-18492]: https://github.com/eclipse-ee4j/glassfish/issues/18589
[GLASSFISH-18589]: https://github.com/eclipse-ee4j/glassfish/issues/18589
[GLASSFISH-18590]: https://github.com/eclipse-ee4j/glassfish/issues/18590
[GLASSFISH-18721]: https://github.com/eclipse-ee4j/glassfish/issues/18721
[GLASSFISH-18810]: https://github.com/eclipse-ee4j/glassfish/issues/18810
[GLASSFISH-18832]: https://github.com/eclipse-ee4j/glassfish/issues/18832
[GLASSFISH-18833]: https://github.com/eclipse-ee4j/glassfish/issues/18833
[GLASSFISH-18834]: https://github.com/eclipse-ee4j/glassfish/issues/18834
[GLASSFISH-18837]: https://github.com/eclipse-ee4j/glassfish/issues/18837
[GLASSFISH-18838]: https://github.com/eclipse-ee4j/glassfish/issues/18838
[GLASSFISH-18839]: https://github.com/eclipse-ee4j/glassfish/issues/18839
[GLASSFISH-18840]: https://github.com/eclipse-ee4j/glassfish/issues/18840
[GLASSFISH-18841]: https://github.com/eclipse-ee4j/glassfish/issues/18841
[GLASSFISH-18842]: https://github.com/eclipse-ee4j/glassfish/issues/18842
[GLASSFISH-18843]: https://github.com/eclipse-ee4j/glassfish/issues/18843
[GLASSFISH-18844]: https://github.com/eclipse-ee4j/glassfish/issues/18844
[GLASSFISH-18845]: https://github.com/eclipse-ee4j/glassfish/issues/18845
[GLASSFISH-18846]: https://github.com/eclipse-ee4j/glassfish/issues/18846
[GLASSFISH-18850]: https://github.com/eclipse-ee4j/glassfish/issues/18850
[GLASSFISH-18887]: https://github.com/eclipse-ee4j/glassfish/issues/18887
[GLASSFISH-19092]: https://github.com/eclipse-ee4j/glassfish/issues/19092
[GLASSFISH-19093]: https://github.com/eclipse-ee4j/glassfish/issues/19093
[GLASSFISH-19261]: https://github.com/eclipse-ee4j/glassfish/issues/19261
[GLASSFISH-19662]: https://github.com/eclipse-ee4j/glassfish/issues/19662
[GLASSFISH-19688]: https://github.com/eclipse-ee4j/glassfish/issues/19688
[GLASSFISH-19696]: https://github.com/eclipse-ee4j/glassfish/issues/19696
[GLASSFISH-19727]: https://github.com/eclipse-ee4j/glassfish/issues/19727
[GLASSFISH-20023]: https://github.com/eclipse-ee4j/glassfish/issues/20023
[GLASSFISH-20593]: https://github.com/eclipse-ee4j/glassfish/issues/20593