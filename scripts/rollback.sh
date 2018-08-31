#!/bin/sh -ex
#
# Copyright (c) 2018 Oracle and/or its affiliates. All rights reserved.
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

# MODULE is relative path of module to be promoted wrt fighterfish dir.
# We hard code the module name to avoid having to update the hudson job
# everytime we want to rollback. This also allows us better tracking.
MODULE=sample/uas/simplejaxrs
# Directory relative to WORKSPACE where last failed promotion job ran.
DIRNAME=20130329_042002

if [ "$MODULE" = "" ]
then
 echo "Module name missing"
 exit 1
fi

cd $WORKSPACE/$DIRNAME/$MODULE
 
mvn -Dhttps.proxyHost=www-proxy.us.oracle.com -Dhttps.proxyPort=80 -B release:rollback
