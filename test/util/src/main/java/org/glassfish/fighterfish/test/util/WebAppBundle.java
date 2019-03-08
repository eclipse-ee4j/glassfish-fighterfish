/*
 * Copyright (c) 2011, 2019 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0, which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the
 * Eclipse Public License v. 2.0 are satisfied: GNU General Public License,
 * version 2 with the GNU Classpath Exception, which is available at
 * https://www.gnu.org/software/classpath/license.html.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0
 */
package org.glassfish.fighterfish.test.util;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;

import javax.servlet.ServletContext;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

/**
 * This class is used by tests to deploy WABs. Since a WAB deployment happens
 * asynchronously when a WAB is activated, for a test case to know whether the
 * deployment is successful or not is not as simple as checking if wab.start()
 * returns successfully or not. This is where this class is helpful. It listens
 * to events raised by the OSGi Web Container as required by the OSGi Web
 * Application spec and depending on the events, returns success or failure when
 * a WAB is deployed. It also uses a timeout mechanism if the deployment does
 * not happen in a specified amount of time.
 *
 * @author Sanjeeb.Sahoo@Sun.COM
 */
public class WebAppBundle {

    enum State {
        DEPLOYING, DEPLOYED, UNDEPLOYING, UNDEPLOYED, FAILED
    }

    private BundleContext context;

    private Bundle b;

    private String contextPath;

    private State state;

    private CountDownLatch deploymentSignal = new CountDownLatch(1);

    private int requestReadTimeout = -1; //seeing GLASSFISH-19854

    /**
     * A handle to the web application bundle being deployed.
     *
     * @param context BundleContext of test used for various OSGi operation.
     * This is not the context of the WAB.
     * @param b Web App Bundle
     */
    public WebAppBundle(BundleContext context, Bundle b) {
        this.context = context;
        this.b = b;
    }

    /**
     * Deploy the given OSGi Web Application Bundle.
     *
     * @param timeout Amount of time it will wait for the deployment to happen
     * before failing
     * @param timeUnit
     * @return ServletContext associated with the deployed web application
     * @throws InterruptedException
     * @throws BundleException
     * @throws TimeoutException if deployment takes longer than the specified
     * timeout value.
     */
    public ServletContext deploy(long timeout, TimeUnit timeUnit)
            throws InterruptedException, BundleException, TimeoutException {

        WABDeploymentEventHandler eventHandler
                = new WABDeploymentEventHandler(context, b,
                        new WABDeploymentEventHandler.Callback() {

                    @Override
                    public void deploying() {
                        state = State.DEPLOYING;
                    }

                    @Override
                    public void deployed(String contextPath) {
                        state = State.DEPLOYED;
                        WebAppBundle.this.contextPath = contextPath;
                        deploymentSignal.countDown();
                    }

                    @Override
                    public void undeploying() {
                        state = State.UNDEPLOYING;
                    }

                    @Override
                    public void undeployed() {
                        state = State.UNDEPLOYED;
                    }

                    @Override
                    public void failed(Throwable throwable, String collision,
                            Long[] collisionBundleIds) {
                        state = State.FAILED;
                        deploymentSignal.countDown();
                    }

                });
        b.start(Bundle.START_TRANSIENT);
        deploymentSignal.await(timeout, timeUnit);
        if (State.DEPLOYED.equals(state)) {
            return (ServletContext) context.getService(context.
                    getServiceReference(ServletContext.class.getName()));
        }
        throw new TimeoutException(
                "Deployment timedout. Check log to see what went wrong.");
    }

    /**
     * Undeploy the OSGi Web Application Bundle. There is no timeout needed
     * here, because the OSGi Web Application Spec requires undeployment to be
     * synchronously handled when a WAB is stopped.
     *
     * @throws BundleException
     */
    public void undeploy() throws BundleException {
        b.stop(Bundle.STOP_TRANSIENT);
    }

    public ServletContext getServletContext() {
        return (ServletContext) context.getService(context.getServiceReference(
                ServletContext.class.getName()));
    }

    private String getHost() {
        return "localhost";
    }

    private int getPort() {
        return 8080;
    }

    public Bundle getBundle() {
        return b;
    }

    //Implementing GLASSFISH-19794
    //Adding a getHttpPostResponse for post request
    public String getHttpPostResponse(String relativePath)
            throws IOException {

        return getHttpResponseUsingHttpClient(relativePath, "POST", null);
    }

    //seeing GLASSFISH-20099
    public String getHttpPostResponse(String relativePath, String contentType)
            throws IOException {

        return getHttpResponseUsingHttpClient(relativePath, "POST",
                contentType);
    }

    //Implementing GLASSFISH-19794
    //Adding a getHttpGetResponse for get request
    public String getHttpGetResponse(String relativePath)
            throws IOException {

        return getHttpResponseUsingHttpClient(relativePath, "GET", null);
    }

    //Implementing GLASSFISH-20088
    private String getHttpResponseUsingHttpClient(String relativePath,
            String mode, String contentType)
            throws IOException {

        String result = null;
            RequestConfig config = RequestConfig.custom()
                .setSocketTimeout(getRequestReadTimeout())
                    .build();
        CloseableHttpClient httpClient = HttpClientBuilder.create()
                .setDefaultRequestConfig(config)
                .build();

        try {
            //Setting ReadTimeOut For current httpClient
            URL serverAddress = new URL("http", getHost(), getPort(),
                    contextPath + relativePath);

            HttpRequestBase httpRequest;

            if ("GET".endsWith(mode)) {
                httpRequest = new HttpGet();
            } else {
                //Creating POST Method
                httpRequest = new HttpPost();
            }

            try {
                httpRequest.setURI(serverAddress.toURI());
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }

            if (contentType != null) {
                //setting ContentType
                httpRequest.setHeader("Content-Type", contentType);
            }

            HttpResponse response = httpClient.execute(httpRequest);

            if (response.getStatusLine().getStatusCode() == 404) {
                throw new FileNotFoundException(
                        "Request Resource is not available.");
            }

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(
                            response.getEntity().getContent()));

            String inputLine;
            StringBuilder sb = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                sb.append(inputLine);
            }

            result = sb.toString();

            in.close();

        } catch (java.net.SocketTimeoutException e) {
            //seeing GLASSFISH-20569
            throw new TimeoutException(e);
        } finally {
            // When HttpClient instance is no longer needed,
            // shut down the connection manager to ensure
            // immediate deallocation of all system resources
            httpClient.close();
        }

        return result;
    }

    // seeing GLASSFISH-19854
    // offering a method called setRequestReadTimeout() which a test can use
    // to override if it likes to
    public void setRequestReadTimeout(int requestReadTimeout) {
        this.requestReadTimeout = requestReadTimeout;
    }

    // seeing GLASSFISH-19854
    private int getRequestReadTimeout() {
        return requestReadTimeout > 0 ? requestReadTimeout
                : (int) Math.min(TestsConfiguration.getInstance().getTimeout(),
                        (long) Integer.MAX_VALUE);
    }
}
