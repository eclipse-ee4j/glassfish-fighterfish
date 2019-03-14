# Test app 11

Web app that invokes the remote EJB part of a separate OSGi/EJB bundle.

This test makes sure that generated code is loaded by the OSGi EJB class-loader.

## Description

Deploy the ejb jar as osgi app and war as a regular war. Access
localhost:8080/test.app11/TestServlet

## Request-Response

- `/test.app11/TestServlet`: `EJB is xxx HELLO-WORLD`