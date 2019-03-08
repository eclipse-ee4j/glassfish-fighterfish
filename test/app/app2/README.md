# Test app 2

UAS Web App - Contains Servlets and  Interface-less EJB and JPA. Everything in
WEB-INF/classes.

## Description

JPA enhancement happens during bundle installation, but Java2DB happens during
deployment as part of EE deployment.

## Request-Response

- `/RegistrationServlet?name=foo2&amp;password=bar`: `Registered foo`
- `/LoginServlet?name=foo2&amp;password=bar`: `Welcome foo`
