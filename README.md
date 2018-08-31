## FighterFish Development Process
This section is for FighterFish development team members. 
It contains information related to building and testing of 
FighterFish. If you are interested in using FighterFish, 
then you should look at [our user's guide](http://glassfish.java.net/public/GF-OSGi-Features.pdf) instead.

### Development Tools 

**Maven:**  
You can use Maven 3.0 or Maven 2, you must use version 2.2.1, as previous versions of MAVEN have led to issues building FighterFish modules. We recommend using maven 3.0.4 or higher as we internally use this version. If you don't have Maven installed, download it [here][41]. 

**Java:**  
You must use JDK 1.6 Update Release 4 or above to build FighterFish and make sure that it's in the PATH. Even if you use JDK 1.7, FighterFish modules are built with a target=1.6 to remain compatible with GlassFish 3.x series. 

### Workspace Details 

`git clone https://github.com/javaee/fighterfish.git`

The workspace is organized as shown below:

    fighterfish/
             module/                      - Contains source code for various FighterFish modules.
                   osgi-javaee-base/      - Base module for all OSGi/JavaEE integration modules
                   osgi-ejb-container/    - OSGi/EJB integration
                   osgi-web-container/    - OSGi/Web integration (OSGi spec based)
                   osgi-http/             - OSGi/HTTP Service integration (OSGi spec based)
                   osgi-jta/              - OSGi/JTA integration (OSGi spec based)
                   osgi-jpa/              - OSGi/JPA integration
                   osgi-jpa-extension/    - OSGi/JPA extension to fix some inherent issues in JPA implementations while running in OSGi
                   osgi-cdi-api/          - GlassFish OSGi/CDI Extension API
                   osgi-cdi/              - GlassFish OSGi/CDI Extension
                   osgi-jdbc/             - OSGi/JDBC integration
                   osgi-ee-resources/     - OSGi/Java EE resource integration (generic)
             sample/                      - Contains samples (there are lots of samples available)
             test/                        - Contains testing related code
                   util/                  - Contains FighterFish Testing Framework - based on PAX EXAM
                   app/                   - Actual test applications used by integration tests
                   it/                    - Integration Tests (JUnit test classes)
             doc/                         - Documentation folder
             scripts/                     - Scripts used during build, test, release cycle
             distro/                      - Builds various distributions like zip-based, ips packages, sample-only, etc.
    
    

### Maven POM Setup 

We consistently use org.glassfish.fighterfish as the groupId.  
We try to use path from fighterfish dir with file separator replaced by '.' as the artifactId except for modules which historically used the directory name as the artifactId.  
Every leaf module is released separately, so they are all set up with correct scm configuration in their pom.xml.  
We don't use aggregator module as parent. This is against maven prescribed policy, but I don't believe it is a good idea to make the aggregator as the parent. Moreover, we never have to release the aggregator module. It is purely existing for use in continuous environment for facilitating whole workspace build. 

### Dependency Management 

We have a highly modular setup, so it is almost never necessary to build the whole workspace as an individual developer. There is no SNAPSHOT relationships between any two modules, so don't be surprised if you change something in osgi-javaee-base and it does not get reflected when you build a dependent module like osgi-web-container. In such a case, you have to temporarily update the dependency version in dependent module's pom. 

All the plugins and dependencies are available in maven central repository, so there is no reason to use any additional repository. 

Whenever we depend on GlassFish artifacts, we use 3.1.x versions as much as possible to stay compatible with GlassFish 3.1.x series. This avoid having to maintain different versions for different GlassFish versions. It has so far worked very well for us. We are in the process of setting up continuous integration jobs to test compatibility of our artifacts against various GlassFish versions. (**TODO**) 

### Versioning Scheme 

We use OSGi sematic versioning scheme. 

### How to Build? 

There is hardly any reason to build the whole workspace as an individual developer. At any given point of time you are working in one or two modules, so it is enough to just build those one or two modules. To build any module, just do:

    mvn clean install -f <Path to your module's pom.xml>
    

All the artifacts and plugins that the build depends on are in maven's official central repository, so there is no reason to use any special repository. When you are inside corporate firewall, please add a suitable mirror server in **~/.m2/settings.xml**. The example below applies to the company I am currently employed at:

    <mirrors>
    <mirror>
    <id>internal-glassfish-nexus</id>
    <url>http://gf-maven.us.oracle.com/nexus/content/groups/internal-gf-nexus/</url>
    <mirrorOf>*</mirrorOf>
    </mirror>
    </mirrors>
    

**SPECIAL NOTES**

1. There is no SNAPSHOT relationships between any two modules, so don't be surprised if you change something in osgi-javaee-base and it does not get reflected when you build a dependent module like osgi-web-container. In such a case, you have to temporarily update the dependency version in dependent module's pom.
2. If your code change impacts other modules, it is recommended that you do a "clean" build of the other modules. I don't think these modern build tools invented handle build dependencies as well as good old "makefile" did.

### Test Details 

#### Integration Tests 

##### Integration Test Layout 

FighterFish testing related code is kept under fighterfish/test directory. This is a brief description of the this directory structure: 

\*test/util: contains a small wrapper/utility/framework based on PAX EXAM to write tests against GlassFish even easier. We leverage this utility in our test. 

\*test/app: A number of Java EE and JavaEE/OSGi applications are used during testing. 

\*test/it: This is the \*i\*ntegration \*t\*esting (it) module which contains only JUnit test classes. If you look at its pom.xml, you shall notice that this module has a dependency on fighterfish/sample and fighterfish/test/app modules, because the unit tests actually provision them. Since we never use SNAPSHOT versions of any module and all our modules are released to central repo, there is no requirement to build any other module before building this module. This is a change since Jan 2013\. Prior to that, we had to first build sample and test/app. 

##### Integration Test Setup 

The tests need a glassfish full profile installation. We are in the process of making this step optional as we can automate the process of downloading and installing GlassFish server. (TODO) 

During testing, GlassFish server is provisioned on a chosen OSGi platform which is embedded in the same surefire/failsafe JVM executing tests. This allows one to see everything in same console output and debug just one process. 

While invoking the tests, please pass the location of glassfish dir using glassfish.home property. It must point to glassfish3/glassfish/ dir in installation. 

There is a Proxy profile setup in the pom.xml to use a proxy server in case some tests require Internet connection which is otherwise forbidden by corporate firewall. Don't use this profile unless you see network connection related issues. 

##### How to execute tests? 

We recommend using a new GlassFish domain just in case there are left over changes from previous execution  
We are going to fix the test to create a new domain everytime. (TODO)

    asadmin delete-domain domain1 && asadmin create-domain domain1
    

Following command checksout and executes all tests:

    svn co https://svn.java.net/svn/glassfish~svn/trunk/fighterfish/test/it
    cd it
    mvn clean test -Dglassfish.home=path/to/glassfish[34]/glassfish/
    

##### How to test a module being developed? 

The tests are run against the given GlassFish installation, so they don't use the FighterFish modules that you would have built. If you want your FighterFish modules to be tested, then you must overwrite the FighterFish module found in glassfish/modules/autostart/ dir by the jar produced by your build. 

##### How to run integration tests on Equinox? 

By default, tests use Felix OSGi runtime. To run tests on Equinox, use Equinox profile as shown below (Note you have to disable Felix profile by using **P** syntax)

    mvn clean test  -P-Felix -PEquinox -Dglassfish.home=path/to/glassfish3/glassfish/
    

##### How to debug an integration test? 

There is a Debug profile available which will cause the test JVM to wait for a debugger to attach on port 9009\. So, you need to invoke:

    mvn clean test -PDebug -Dglassfish.home=path/to/glassfish3/glassfish
    

##### How to run a single integration test: 

Because of a limitation in PAX EXAM, you can't select a single test method using -Dtest=testClass\#testMethod syntax, so you have to select the entire test class. 

For faster execution of tests, we configure PAX EXAM to use EagerSingleStagedReactorFactory, which mean it uses the same framework instance and hence same instance of embedded GlassFish to run all test methods found in a test class. The test methods are designed to be isolated from each other. So, if you want to run only one test method, we recommend copying and pasting it in a separate test class and running that test class alone. For this reason, we provide a dummy SingleTest.java. So, you can do something like this:

    mvn clean test -Dtest=SingleTest -Dglassfish.home=path/to/glassfish3/glassfish
    

##### How to run integration tests on IBM JDK: 

The embedded Java process needs a slightly different configuration for embedded GlassFish to work correctly when IBM J9 JVM is used. This is captured in IBM-J9-VM profile in the pom.xml. 

The pom.xml has the ability to select appropriate profile based on the JVM that's used to invoke maven. So, just have the right Java in your path and you are done. 

#### OSGi Compliance Tests 

Please follow the instructions in test/ct/README.txt. The steps are very simple, it's just that not everyone has access to OSGi CT which is maintained in OSGi Alliance source code repository. We have continuous integration jobs set up to run compliance tests against every new GlassFish build. 

### Continuous Integration Jobs 

We have following CI jobs: (add job details) 

OSGi Compliance Test using latest GlassFish Trunk builds 

FighterFish Integration Tests using latest GlassFish Trunk builds 

FighterFish Integration Tests using latest GlassFish Trunk builds and latest FighterFish builds 

FighterFish Integration Tests using latest supported release of GlassFish 3.1.x and latest FighterFish builds 

### Release (Promotion) Process 

#### Overview 

Modules are released individually. We have hardly a reason to release multiple modules simultaneously unless we are releasing samples or test applications.  
The release process is a multistep process most part of which has been automated and greatly simplified by using **maven-release-plugin**. 

In order to maintain a consistent release process, we have checked in a script called scripts/promote.sh containing the commands that take care of building, tagging and deploying. As you can see from that script, it is effectively a two line script which calls maven-release-plugin:prepare and maven-release-plugin:perform goals. For record keeping purpose, the script creates a new directory for each invocation of release plugin. This directory comes in handy during rollback process as discussed later. 

#### Release Jobs 

Since maven central requires the artifacts to be signed by an authorised person, we don't release the artifacts from any random developer's environment. Otherwise, the release process actually has no dependency on specific environment. For consistency, sanity and authenticity reasons, we use GlassFish Release Engineering environment only to release artifacts. We have a job configured in GlassFish release engineering Hudson server called **fighterfish-promotion** which has two tasks called promotion-task and rollback-task for promotion and rollback respectively. The job is configured such that promotion process uses RE's credentials to update svn and RE's gpg-passphrase to sign the artifacts. 

#### Release Tag Location 

Every released artifact source is found under [https://svn.java.net/svn/glassfish~svn/tags/fighterfish-releases][45]. Except a few artifacts, all our released artifacts use one consistent tag format. Because some of the modules don't specify tag details correctly in pom.xml, to ensure correct tag location, while invoking release plugin, we pass the following argument to tag the source code:

    -DtagBase=https://svn.java.net/svn/glassfish~svn/tags/fighterfish-releases -DtagNameFormat=@{project.groupId}.@{project.artifactId}-@{project.version}
    

#### Artifact Deployment 

In the spirit of maven, we also make use of **Nexus** staging repository in our release process. Our artifact deployment is therefore a two step process. First the artifact is deployed to [java.net nexus staging repository][46]. It then undergoes additional testing before being released to maven central repository. release:perform goal deploys the artifacts to staging repo only. The staging repository is configured for all in distributionManagement section of net.java:jvnet-parent:pom:3 which is the root of POM hierarchy in our case. 

#### How to Release? 

1. Update the module names to be released in scripts/promote.sh and check in the script.
2. Login to GlassFish RE Hudson server and go to [fighterfish-promotion][47] job.
3. Visit the tasks link and execute "promotion" task.
4. Each promotion uses a new timestamped directory for record keeping purpose. See the task output for the directory name. You will need this if you have to rollback the release for whatever reason.

#### How to test staged artifacts? 

It is a good idea to test the artifacts while they are in staging repo. If they have bugs, then we have a chance to drop them and re-release them. Note down the staging repository URL by logging into [java.net nexus staging repository][48] using your java.net credentials. Create a staging profile in your ~/.m2/settings.xml and add the repository details as shown below:

      <profiles>
        <profile>
          <id>staging</id>
          <repositories>
            <repository>
              <snapshots>
                <updatePolicy>never</updatePolicy>
              </snapshots>
              <id>jvnet-nexus-staging-repo</id>
              <name>Java.net Nexus Staging Repository</name>
              <url>http://maven.java.net/content/repositories/orgglassfish-137/</url>
            </repository>
          </repositories>
        </profile>
      </profiles>
    
      <mirror>
        <id>internal-glassfish-nexus</id>
        <url>http://gf-maven.us.oracle.com/nexus/content/groups/internal-gf-nexus/</url>
        <mirrorOf>*,!jvnet-nexus-staging-repo</mirrorOf>
      </mirror>
    
    

Then use this profile to be able to use the staged artifacts. If the artifacts are not release-worthy, then follow the instructions in the next section to rollback the release. 

#### How to Rollback? 

There are two reasons to rollback a release:

1. The release artifacts were found to be not release worthy.
2. The release process failed to complete.  
In a complex environment involving so many servers, there are many failure points in the system. I have lately suffered from svn failures to proxy failures to nexus issues. So, we have to have a process in place for cleaning up the mess left behind by an unsuccessful release.

**NEVER ROLLBACK A RELEASE THAT'S ALREADY PUSHED/RELEASED OUT OF NEXUS** 

Follow the steps below to rollback a release. No. of steps that need to be executed depends on how far the release process had progressed. If promotion-task has completed release:perform goal successfully, we can't rollback the release using rollback task, because the release plugin would have cleaned up backup pom.xml. In that case, we have to manually rollback the pom.xml in addition to removing the scm tag and dropping the artifact from staging repository.

1. Update the DIRNAME and MODULE\_NAME property in scripst/rollback.sh and check it in.
2. Login to GlassFish RE Hudson server and go to fighterfish-promotion job.
3. Visit the tasks link and execute "rollback" task.
4. Remove the scm tags if prepare phase had created it. This is needed due to an unimplemented feature ([MRELEASE-229][49]) in maven-release-plugin.
5. Drop the artifact from nexus if it has been deployed by release:perform goal.
6. Remove the artifact from maven-local-repo if it exists there. See FAQ for details.

#### Mailing Lists 

FighterFish developers use [dev@glassfish.java.net][50] mailing list. 

FighterFish users use [users@glassfish.java.net][51] mailing list or [GlassFish forum][52]. 

#### Issue Tracking 

We use [GlassFish JIRA][53] for our issue tracking, because GlassFish is the primary delivery vehicle for these modules. Choose component as **OSGi** or **OSGi-JavaEE** depending on issue type. 

Resolution of an issue requires two separate steps as described below:

1. You first fix the issue in appropriate module in FighterFish workspace.
2. Update JIRA with your svn commit details, but don't mark the issue as RESOLVED. This is because GlassFish integrates specific versions of FihterFish modules. The issue can't be resolved until you have integrated the version of your module containing the fix with GlassFish. Refer to "release process" for details about how to release a module.

#### Check-in Guidelines 

We don't like more than issue to be fixed in one svn transaction nor do we like one svn transaction to contain partial fix.  
We recommend you to follow the following format in your comment: 

_Issue ID - Issue Synopsys followed by details_ 

Each module has a RELEASENOTE.txt. Please update it immediately after fixing a bug. We hope to be able to automate this at some point, but it's not done yet. 

#### FAQ 

1. Why don't we perform multi-module release using aggregator pom?  
We adopt a modular We have hardly a reason to release all the modules under a given directory at once. Since every released artifact is independently of others, we also like each of them to be tagged a uniform way. Our experience shows that when a aggregator module is built, the submodules are tagged under the aggregator module thus violating our uniform tagging requirement.
2. When do I have to ever remove something from local repo?  
Typically prepare goal executes "clean verify" phases, so a failed prepare goal is not likely to cause the artifact to find its way to local repo. The perform goal installs the artifacts in local repo, so if it has run, then clean the artifact from local repo. It's for this reason, we are thinking of configuring the release process to use its own maven local repository. If you have tested with an artifact that's going to be dropped, then it might have been downloaded to your local repo. In that case, you have to remove it as well. This is where I think the improvement about artifact resolution based on repository settings that has gone into maven 3 helps. If an artifact has been downloaded from a staging repo, it is unlikely to be used when you are not using staging profile.

#### Contact: 

If you have questions or comments that are not suitable for mailing list discussion, please contact Sahoo who is the author of this document as well as leading the FighterFish development effort. 

#### More Coming: 

Test description  
Test framework description  
Submodule details  
Design Documents  
Road Map 

  
Copyright (c) 2005-2017 Oracle Corporation and/or its affiliates.