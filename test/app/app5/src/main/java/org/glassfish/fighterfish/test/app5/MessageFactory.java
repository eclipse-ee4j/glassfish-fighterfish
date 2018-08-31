/*
 * Copyright (c) 2011, 2018 Oracle and/or its affiliates. All rights reserved.
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
 * @author sanjeeb.sahoo@oracle.com
 *
 */
public class MessageFactory {
    public static FacesMessage getMessage(String messageId, Object[] params)
    {
      Locale locale = null;
      FacesContext context = FacesContext.getCurrentInstance();

      if ((context != null) && (context.getViewRoot() != null)) {
        locale = context.getViewRoot().getLocale();
        if (locale == null)
          locale = Locale.getDefault();
      }
      else {
        locale = Locale.getDefault();
      }

      return getMessage(locale, messageId, params);
    }

    public static FacesMessage getMessage(Locale locale, String messageId, Object[] params)
    {
      FacesMessage result = null;

      String summary = null;
      String detail = null;
      String bundleName = null;
      ResourceBundle bundle = null;

      if ((null != (bundleName = getApplication().getMessageBundle())) && 
        (null != (bundle = ResourceBundle.getBundle(bundleName, locale, getCurrentLoader(bundleName)))))
      {
        try
        {
          summary = bundle.getString(messageId);
          detail = bundle.getString(messageId + "_detail");
        }
        catch (MissingResourceException e)
        {
        }

      }

      if (null == summary)
      {
        bundle = ResourceBundle.getBundle("javax.faces.Messages", locale, getCurrentLoader(bundleName));

        if (null == bundle) {
          throw new NullPointerException();
        }
        try
        {
          summary = bundle.getString(messageId);
          detail = bundle.getString(messageId + "_detail");
        }
        catch (MissingResourceException e)
        {
        }
      }

      if (null == summary) {
        return null;
      }

      if ((null == summary) || (null == bundle)) {
        throw new NullPointerException(" summary " + summary + " bundle " + bundle);
      }

      return new MessageFactory.BindingFacesMessage(locale, summary, detail, params);
    }

    public static FacesMessage getMessage(FacesContext context, String messageId)
    {
      return getMessage(context, messageId, null);
    }

    public static FacesMessage getMessage(FacesContext context, String messageId, Object[] params)
    {
      if ((context == null) || (messageId == null)) {
        throw new NullPointerException(" context " + context + " messageId " + messageId);
      }

      Locale locale = null;

      if ((context != null) && (context.getViewRoot() != null))
        locale = context.getViewRoot().getLocale();
      else {
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

    public static FacesMessage getMessage(FacesContext context, String messageId, Object param0)
    {
      return getMessage(context, messageId, new Object[] { param0 });
    }

    public static FacesMessage getMessage(FacesContext context, String messageId, Object param0, Object param1)
    {
      return getMessage(context, messageId, new Object[] { param0, param1 });
    }

    public static FacesMessage getMessage(FacesContext context, String messageId, Object param0, Object param1, Object param2)
    {
      return getMessage(context, messageId, new Object[] { param0, param1, param2 });
    }

    public static FacesMessage getMessage(FacesContext context, String messageId, Object param0, Object param1, Object param2, Object param3)
    {
      return getMessage(context, messageId, new Object[] { param0, param1, param2, param3 });
    }

    public static Object getLabel(FacesContext context, UIComponent component)
    {
      Object o = component.getAttributes().get("label");
      if (o == null) {
        o = component.getValueExpression("label");
      }

      if (o == null) {
        o = component.getClientId(context);
      }
      return o;
    }

    protected static Application getApplication() {
      FacesContext context = FacesContext.getCurrentInstance();
      if (context != null) {
        return FacesContext.getCurrentInstance().getApplication();
      }
      ApplicationFactory afactory = (ApplicationFactory)FactoryFinder.getFactory("javax.faces.application.ApplicationFactory");

      return afactory.getApplication();
    }

    protected static ClassLoader getCurrentLoader(Object fallbackClass) {
      ClassLoader loader = Thread.currentThread().getContextClassLoader();

      if (loader == null) {
        loader = fallbackClass.getClass().getClassLoader();
      }
      return loader;
    }
    
    static class BindingFacesMessage extends FacesMessage
    {
      private Locale locale;
      private Object[] parameters;
      private Object[] resolvedParameters;

      BindingFacesMessage(Locale locale, String messageFormat, String detailMessageFormat, Object[] parameters)
      {
        super(messageFormat, detailMessageFormat);
        this.locale = locale;
        this.parameters = parameters;
        if (parameters != null)
          this.resolvedParameters = new Object[parameters.length];
      }

      public String getSummary()
      {
        String pattern = super.getSummary();
        resolveBindings();
        return getFormattedString(pattern, this.resolvedParameters);
      }

      public String getDetail() {
        String pattern = super.getDetail();
        resolveBindings();
        return getFormattedString(pattern, this.resolvedParameters);
      }

      private void resolveBindings() {
        FacesContext context = null;
        if (this.parameters != null)
          for (int i = 0; i < this.parameters.length; i++) {
            Object o = this.parameters[i];
            if ((o instanceof ValueExpression)) {
              if (context == null) {
                context = FacesContext.getCurrentInstance();
              }
              o = ((ValueExpression)o).getValue(context.getELContext());
            }

            if (o == null) {
              o = "";
            }
            this.resolvedParameters[i] = o;
          }
      }

      private String getFormattedString(String msgtext, Object[] params)
      {
        String localizedStr = null;

        if ((params == null) || (msgtext == null)) {
          return msgtext;
        }
        StringBuffer b = new StringBuffer(100);
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
