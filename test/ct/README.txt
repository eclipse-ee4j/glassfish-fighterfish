#
# Copyright (c) 2010, 2018 Oracle and/or its affiliates. All rights reserved.
#

Author: sanjeeb.sahoo@oracle.com
Date: 22 Feb 2013

Please read the instructions below if you want to run OSGi Compliance Tests against GlassFish.

Layout
------
Given below is a brief description of files in this dir tree:

ct/  <- Root of the hierarchy
    misc/ <- Misc files used during CT run
        script/ <- Handful of scripts to help us in automation
        bundle/  <- Additional helper bundles used to work around CT issues
            delay/ <- This contains a simple bundle which introduces a delay
            javax.servlet-api_2.5/ <- This reexports javax.servlet 3.0 as 2.5


Instructions to run CT:
-----------------------
1. You must checkout OSGi CT workspace from OSGi Alliance svn repo like this:

svn co https://www.osgi.org/members/svn/build/tags/r4v42-enterprise-ri-ct-final/ r4v42-final

From here on, we refer to r4v42-final dir as ${cthome}. 

2. We need to build the CT workspace. Use JDK 1.6 to build r4v42 CT as not all sources compile with JDK 1.7.
More over, JDK 1.7 uses SHA-256 digest algorithm, yet Web container CT assumes SHA-1 digest headers. 
This has been reported as a bug to alliance. See https://www.osgi.org/members/bugzilla/show_bug.cgi?id=2500

You can build like this:
export PATH=/path/to/JDK1.6/bin:$PATH
cd ${cthome}
for d in org.osgi.test.cases.transaction org.osgi.test.cases.http org.osgi.test.cases.webcontainer org.osgi.test.cases.jdbc
do
 pushd $d
 ant clean all
 popd
done

2. Install GlassFish. We refer to glassfish dir as ${gfhome} from now on.

3. How to run:
There are two ways to run the CT and hence there are two scripts, viz:
run-osgi-ct.sh and run-osgi-ct-direct.sh.
The former one uses a bnd file for instructions where as the latter one has all the instructions.
The latter one launches the JVM directly which is used to run the CT, 
where as the former one uses bnd to launch the JVM. 
The advantage of the latter one is that we can control things like osgi cache location, individual test to run.
The former one can only be used to run entire suite.
There is no other difference. Both start the targer JVM in debug mode and the debug port is 9009.

For more details, execute:

misc/script/run-osgi-ct.sh --help
misc/script/run-osgi-ct-direct.sh --help

We recommend using run-osgi-ct-direct.sh.

Here are examples of how you can run it:

# Runs entire OSGi HTTP CT
./misc/script/run-osgi-ct-direct.sh /tmp/gf/ /space/ss141213/WS/osgi/r4v42-final http

# Runs one test from OSGi HTTP CT
./misc/script/run-osgi-ct-direct.sh /tmp/gf/ /space/ss141213/WS/osgi/r4v42-final http junit.HttpTestBundle1

