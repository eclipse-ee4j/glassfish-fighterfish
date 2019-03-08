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
package org.glassfish.fighterfish.test.app5;

import javax.el.ValueExpression;
import javax.faces.FactoryFinder;
import javax.faces.application.Application;
import javax.faces.application.ApplicationFactory;
import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * Faces message factory.
 */
public final class MessageFactory {

    /**
     * Cannot be instanciated.
     */
    private MessageFactory() {
    }

    /**
     * Get a message.
     * @param messageId message ID
     * @param params parameters
     * @return FacesMessage
     */
    public static FacesMessage getMessage(final String messageId,
            final Object[] params) {

        Locale locale;
        FacesContext context = FacesContext.getCurrentInstance();
        if ((context != null) && (context.getViewRoot() != null)) {
            locale = context.getViewRoot().getLocale();
            if (locale == null) {
                locale = Locale.getDefault();
            }
        } else {
            locale = Locale.getDefault();
        }
        return getMessage(locale, messageId, params);
    }

    /**
     * Get a message.
     * @param locale locale to use
     * @param messageId message ID
     * @param params parameters
     * @return FacesMessage
     */
    public static FacesMessage getMessage(final Locale locale,
            final String messageId, final Object[] params) {

        String summary = null;
        String detail = null;
        String bundleName;
        ResourceBundle bundle;

        bundleName = getApplication().getMessageBundle();
        bundle = ResourceBundle.getBundle(bundleName, locale,
                                getCurrentLoader(bundleName));
        if (bundleName != null && bundle != null) {
            try {
                summary = bundle.getString(messageId);
                detail = bundle.getString(messageId + "_detail");
            } catch (MissingResourceException e) {
            }
        }

        if (summary == null) {
            bundle = ResourceBundle.getBundle("javax.faces.Messages", locale,
                    getCurrentLoader(bundleName));
            if (null == bundle) {
                throw new NullPointerException();
            }
            try {
                summary = bundle.getString(messageId);
                detail = bundle.getString(messageId + "_detail");
            } catch (MissingResourceException e) {
            }
        }

        if (summary == null) {
            return null;
        }
        if (bundle == null) {
            throw new NullPointerException(" bundle " + bundle);
        }
        return new MessageFactory.BindingFacesMessage(locale, summary, detail,
                params);
    }

    /**
     * Get a message.
     * @param context faces context
     * @param messageId message ID
     * @return FacesMessage
     */
    public static FacesMessage getMessage(final FacesContext context,
            final String messageId) {

        return getMessage(context, messageId, null);
    }

    /**
     * Get a message.
     * @param context faces context
     * @param messageId message ID
     * @param params parameters
     * @return FacesMessage
     */
    public static FacesMessage getMessage(final FacesContext context,
            final String messageId, final Object[] params) {

        if ((context == null) || (messageId == null)) {
            throw new NullPointerException(" context " + context
                    + " messageId " + messageId);
        }
        Locale locale;
        if (context.getViewRoot() != null) {
            locale = context.getViewRoot().getLocale();
        } else {
            locale = Locale.getDefault();
        }
        if (null == locale) {
            throw new NullPointerException(" locale " + locale);
        }
        FacesMessage message = getMessage(locale, messageId, params);
        if (message != null) {
            return message;
        }
        locale = Locale.getDefault();
        return getMessage(locale, messageId, params);
    }

    /**
     * Get a message.
     * @param context faces context
     * @param messageId message ID
     * @param param0 first parameter
     * @return FacesMessage
     */
    public static FacesMessage getMessage(final FacesContext context,
            final String messageId, final Object param0) {

        return getMessage(context, messageId, new Object[]{param0});
    }

    /**
     * Get a message.
     * @param context faces context
     * @param messageId message ID
     * @param param0 first parameter
     * @param param1 second parameter
     * @return FacesMessage
     */
    public static FacesMessage getMessage(final FacesContext context,
            final String messageId, final Object param0, final Object param1) {

        return getMessage(context, messageId, new Object[]{param0, param1});
    }

    /**
     * Get a message.
     * @param context faces context
     * @param messageId message ID
     * @param param0 first parameter
     * @param param1 second parameter
     * @param param2 third parameter
     * @return FacesMessage
     */
    public static FacesMessage getMessage(final FacesContext context,
            final String messageId, final Object param0, final Object param1,
            final Object param2) {

        return getMessage(context, messageId, new Object[]{param0, param1,
            param2});
    }

    /**
     * Get a message.
     * @param context faces context
     * @param messageId message ID
     * @param param0 first parameter
     * @param param1 second parameter
     * @param param2 third parameter
     * @param param3 fourth parameter
     * @return FacesMessage
     */
    public static FacesMessage getMessage(final FacesContext context,
            final String messageId, final Object param0, final Object param1,
            final Object param2, final Object param3) {

        return getMessage(context, messageId, new Object[]{param0, param1,
            param2, param3});
    }

    /**
     * Get a label.
     * @param context faces context
     * @param component UI component
     * @return Object
     */
    public static Object getLabel(final FacesContext context,
            final UIComponent component) {

        Object o = component.getAttributes().get("label");
        if (o == null) {
            o = component.getValueExpression("label");
        }

        if (o == null) {
            o = component.getClientId(context);
        }
        return o;
    }

    /**
     * Get the current application.
     * @return Application
     */
    private static Application getApplication() {
        FacesContext context = FacesContext.getCurrentInstance();
        if (context != null) {
            return FacesContext.getCurrentInstance().getApplication();
        }
        ApplicationFactory afactory = (ApplicationFactory) FactoryFinder
                .getFactory("javax.faces.application.ApplicationFactory");
        return afactory.getApplication();
    }

    /**
     * Get the current class-loader.
     * @param fallbackClass class to derive a fallback class-loader
     * @return ClassLoader
     */
    private static ClassLoader getCurrentLoader(final Object fallbackClass) {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();

        if (loader == null) {
            loader = fallbackClass.getClass().getClassLoader();
        }
        return loader;
    }

    /**
     * Faces message implementation.
     */
    private static final class BindingFacesMessage extends FacesMessage {

        /**
         * Locale.
         */
        private final Locale locale;

        /**
         * Parameters to bind.
         */
        private final Object[] parameters;

        /**
         * Resolved parameters.
         */
        private final Object[] resolvedParameters;

        /**
         * Create a new instance.
         * @param loc locale to use
         * @param messageFormat message format
         * @param detailMessageFormat detailed message format
         * @param params resolved parameters
         */
        BindingFacesMessage(final Locale loc, final String messageFormat,
                final String detailMessageFormat, final Object[] params) {

            super(messageFormat, detailMessageFormat);
            this.locale = loc;
            this.parameters = params;
            if (params != null) {
                this.resolvedParameters = new Object[params.length];
            } else {
                this.resolvedParameters = null;
            }
        }

        @Override
        public String getSummary() {
            String pattern = super.getSummary();
            resolveBindings();
            return getFormattedString(pattern, this.resolvedParameters);
        }

        @Override
        public String getDetail() {
            String pattern = super.getDetail();
            resolveBindings();
            return getFormattedString(pattern, this.resolvedParameters);
        }

        /**
         * Resolve the parameters from the faces context.
         */
        private void resolveBindings() {
            FacesContext context = null;
            if (this.parameters != null) {
                for (int i = 0; i < this.parameters.length; i++) {
                    Object o = this.parameters[i];
                    if ((o instanceof ValueExpression)) {
                        if (context == null) {
                            context = FacesContext.getCurrentInstance();
                        }
                        o = ((ValueExpression) o)
                                .getValue(context.getELContext());
                    }

                    if (o == null) {
                        o = "";
                    }
                    this.resolvedParameters[i] = o;
                }
            }
        }

        /**
         * Format a message.
         * @param msgtext message to format
         * @param params parameters
         * @return String
         */
        private String getFormattedString(final String msgtext,
                final Object[] params) {

            String localizedStr = null;
            if ((params == null) || (msgtext == null)) {
                return msgtext;
            }
            StringBuilder b = new StringBuilder();
            MessageFormat mf = new MessageFormat(msgtext);
            if (this.locale != null) {
                mf.setLocale(this.locale);
                b.append(mf.format(params));
                localizedStr = b.toString();
            }
            return localizedStr;
        }
    }
}
