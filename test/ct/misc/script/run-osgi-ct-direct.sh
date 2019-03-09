#!/bin/sh +x
#
# Copyright (c) 2013, 2019 Oracle and/or its affiliates. All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v. 2.0, which is available at
# http://www.eclipse.org/legal/epl-2.0.
#
# This Source Code may also be made available under the following Secondary
# Licenses when the conditions for such availability set forth in the
# Eclipse Public License v. 2.0 are satisfied: GNU General Public License,
# version 2 with the GNU Classpath Exception, which is available at
# https://www.gnu.org/software/classpath/license.html.
#
# SPDX-License-Identifier: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0
#

# This is a simple script that we use to run OSGi Compliance Tests.
# We use the same file for all test suites, viz: 
# org.osgi.test.cases.http, 
# org.osgi.test.cases.transaction, 
# org.osgi.test.cases.webcontainer, 
# org.osgi.test.cases.jdbc, 
# etc.
#
# This script uses following properties which are supplied to it from command line:
#
# gfhome -  specifies where glassfish has been installed.
# cthome - specifies where OSGi CT has been checked out
# ctname - refers to the OSGi CT test suite that we want to run. e.g., http|transaction|webcontainer
# testclass - refers to the individual JUnit test class that you may want to execute. i
# When this is not specified, all tests are executed
#
set -e

print_usage() {
 echo "Usage: $0 <glassfish.home> <ct.home> <ct.name> [test.class]"
 echo "If you don't supply the test class name, all tests from the test suite will get executed."
 echo "Test results will be reported in `pwd`/reports/osgi-ct.xml"
 exit 1
}

if [ "$1" = "--help" ] || [ $# -lt 3 ]
then
 print_usage
fi

gfhome=${1}
cthome=${2}
ctname=${3}
testclass=${4}

echo Executing OSGi ${ctname} CT from ${cthome} against ${gfhome}

scriptdir=`dirname $0`

if [ -d reports ]
then
echo Reusing existing reports dir.
else
 mkdir reports
fi
reportfile=`pwd`/reports/osgi-ct.xml

# Make sure cthome ends with '/'
# We get some strange errors if we have // in bundle path, so we do this magic here.
set +e
echo ${cthome} | grep '/$'
if [ $? -ne 0 ]
then
 cthome=${cthome}/
fi
set -e

if [ "${testclass}" != "" ]
then
 extraopt="-test org.osgi.test.cases.${ctname}.${testclass}"
fi

# Extract the bnd runtime jar from the uber bnd jar
(cd /tmp; jar xvf ${cthome}/licensed/repo/biz.aQute.bnd/biz.aQute.bnd-latest.jar aQute/bnd/test/biz.aQute.runtime.jar)

classpath=/tmp/aQute/bnd/test/biz.aQute.runtime.jar:${gfhome}/osgi/felix/bin/felix.jar:${cthome}licensed/repo/com.springsource.junit/com.springsource.junit-3.8.2.jar:$JAVA_HOME/lib/tools.jar
debug="-Xdebug -Xnoagent -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=9009"

# We set all the properties used for all test suites. That means a few extra proprties are set, but 
# that does not harm. It allows us to have just one script for all suites.
# Pl note that we need to set the bundles first before passing the target bundle, else http CT won't run.
# It makes sense since bundles are used to set up the environment for the target bundle.
options="${debug} \
 -classpath ${classpath} \
 aQute.junit.runtime.Target \
 -export junit.framework;version=3.8 \
 -set org.osgi.framework.storage /tmp/osgi-ct-cache \
 -set org.osgi.framework.storage.clean onFirstInit \
 -set org.osgi.framework.bundle.parent framework \
 -set osgi.support.multipleHosts true \
 -set org.osgi.service.webcontainer.hostname 127.0.0.1 \
 -set org.osgi.service.webcontainer.http.port 8080 \
 -set org.glassfish.osgihttp.ContextPath / \
 -set com.sun.aas.installRoot ${gfhome} \
 -set gosh.args --nointeractive \
 -bundle $HOME/.m2/repository/org/glassfish/fighterfish/fighterfish-test-ct-misc-bundle-servlet-api_2.5/3.0.0-SNAPSHOT/fighterfish-test-ct-misc-bundle-servlet-2.5-.jar \
 -bundle ${cthome}/cnf/repo/org.osgi.impl.service.log/org.osgi.impl.service.log-1.3.2.jar \
 -bundle ${gfhome}/modules/glassfish.jar \
 -bundle $HOME/.m2/repository/org/glassfish/fighterfish/fighterfish-test-ct-misc-bundle-delay/3.0.0-SNAPSHOT/fighterfish-test-ct-misc-bundle-delay.jar \
 -target ${cthome}org.osgi.test.cases.${ctname}/generated/org.osgi.test.cases.${ctname}.jar \
 -report ${reportfile} \
 ${extraopt}"

echo Executing the command: [java ${options}]
java ${options} || true

# Show results
summaryfile=`dirname ${reportfile}`/summary.txt
echo "Test results are kept in ${reportfile} and ${summaryfile}"
scriptdir=`dirname $0`
set +e
${scriptdir}/find-failed-ct-test.sh ${reportfile} > ${summaryfile} 2>&1
failed=$?
cat ${summaryfile}
exit ${failed}
