# Test app 8

Servlet -> JPA(Java EE) Emp/Dept CRUD Application.

## Description

Same as test.app6 except that it uses JPA in Java EE mode
Here the Servlet uses @PU.
structure:

```
wab
    WEB-INF/classes/.../CRUDServlet.class, 
    WEB-INF/lib/JPA classes, p.xml
```

Data model: Employee, Department.  Employee has `@ManyToOne(FetchType=LAZY)`
relationship with Department.

## Request-Response

- `/test.app8/crud?action=createDepartment\&departmentName=hr`
- `/test.app8/crud?action=createDepartment\&departmentName=finance`
- `/test.app8/crud?action=createEmployee\&departmentName=finance`
- `/test.app8/crud?action=createEmployee\&departmentName=hr`
- `/test.app8/crud?action=readEmployee\&employeeId=1`
- `/test.app8/crud?action=deleteEmployee\&employeeId=2`
- `/test.app8/crud?action=deleteDepartment\&departmentName=finance`
