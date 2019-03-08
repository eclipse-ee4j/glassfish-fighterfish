# Test app 16

Message producer -> MDB -> JPA, Servlet -> JPA.

## Description

Message producer looks up a JMS topic using JNDI and sens messages. 
MDB logs the messages to a DB using an @Inject @OSGiService emf. 
Also uses interceptor to createEM.
Servlet uses @Inject @OSGiService emf to talk to db to retrieve messages.

## Request-Response

- `/MessageReaderServlet`: `Total number of messages: `
