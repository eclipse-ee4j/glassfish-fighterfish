# Test app 18

Tests deployment order of singleton ejbs.

## Description

Has three ejbs:

- FooEJB which depends on ServiceListenerEJB
- ServiceListenerEJB which depends on EjbLifecycleObserverEJB
- EjbLifecycleObserverEJB which which raises an EventAdmin event under the topic
 org/glassfish/fighterfish/test/app18/

When FooEJB the event should be raised.
