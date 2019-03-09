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
# This script uses following options which are supplied to it from command line:
#
# gfhome - specifies where glassfish has been installed.
# cthome - specifies where OSGi CT has been checked out
# ctname - refers to the OSGi CT test suite that we want to run. e.g., http|transaction|webcontainer
#

set -e
print_usage() {
 echo "Usage: $0 <glassfish.home> <OSGi CT Home> <CT Name> [bnd file name]"
 echo "Test results will be reported in `pwd`/reports/"
 exit 1
}

if [ "$1" = "--help" ] || [ $# -lt 3 ]
then
 print_usage
fi

gfhome=${1}
cthome=${2}
ctname=${3}
bndfile=${4:-osgi-ct.bnd}

# We set glassfish.home, ct.home and ct.name properties as they are referenced in bnd files.
options="-Dglassfish.home=${gfhome} -Dct.home=${cthome} -Dct.name=${ctname} \
  -jar ${cthome}/licensed/repo/biz.aQute.bnd/biz.aQute.bnd-latest.jar runtests ${bndfile}"
echo Executing cmd: [java ${options}]
java ${options}

# bnd generates report in ./reports/<base name of bnd file without extension>.xml
# So, we have to calculate report file path.
# Refer to http://www.cyberciti.biz/faq/unix-linux-extract-filename-and-extension-in-bash/ for following variable expansion tricks
bndfilebasename=${bndfile##*/}
bndfilenamewithoutextension=${bndfilebasename%.*}
reportfile=`pwd`/reports/${bndfilenamewithoutextension}.xml
summaryfile=`dirname ${reportfile}`/summary.txt
echo "Test results are kept in ${reportfile} and ${summaryfile}"
scriptdir=`dirname $0`
set +e
${scriptdir}/find-failed-ct-test.sh ${reportfile} > ${summaryfile} 2>&1
failed=$?
cat ${summaryfile}
exit ${failed}

