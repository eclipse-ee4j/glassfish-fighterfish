# Test app 6

Servlet -> JPA(Java SE) Emp/Dept CRUD Application

## Description

Here the Servlet uses `Persistence.createEMF` instead of `@PersistenceUnit`.
Since it uses in Java SE style, it has to explicitly configure
`PersistenceUnit` with `transaction-type=JTA` and jta-data-source.

Structure:

```
wab
    WEB-INF/classes/.../CRUDServlet.class, 
    WEB-INF/lib.entities.jar (JPA classes, p.xml)
```

Data model: Employee, Department.  Employee has `@ManyToOne(FetchType=LAZY)`
relationship with Department.

## Request-Response

- `/test.app7/crud?action=createDepartment&amp;departmentName=hr`
- `/test.app7/crud?action=createDepartment&amp;departmentName=finance`
- `/test.app7/crud?action=createEmployee&amp;departmentName=finance`
- `/test.app7/crud?action=createEmployee&amp;departmentName=hr`
- `/test.app7/crud?action=readEmployee&amp;employeeId=1`
- `/test.app7/crud?action=deleteEmployee&amp;employeeId=2`
- `/test.app7/crud?action=deleteDepartment&amp;departmentName=finance`
