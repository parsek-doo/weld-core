/*
 * JBoss, Home of Professional Open Source
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,  
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.webbeans.servlet;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.jboss.webbeans.CurrentManager;
import org.jboss.webbeans.context.SessionContext;
import org.jboss.webbeans.context.beanmap.BeanMap;
import org.jboss.webbeans.context.beanmap.SimpleBeanMap;
import org.jboss.webbeans.log.LogProvider;
import org.jboss.webbeans.log.Logging;

/**
 * Implementation of the Web Beans lifecycle that can react to servlet events
 * 
 * This implementation boots the Web Beans container.
 * 
 * @author Pete Muir
 * @author Nicklas Karlsson
 */
@Deprecated
public class ServletLifecycle extends AbstractLifecycle
{
   
   public static final String REQUEST_ATTRIBUTE_NAME = ServletLifecycle.class.getName() + ".requestBeanMap";
   
   static
   {
      AbstractLifecycle.setInstance(new ServletLifecycle());
   }
   
   public static ServletLifecycle instance()
   {
      return (ServletLifecycle) AbstractLifecycle.instance();
   }
   
   private static LogProvider log = Logging.getLogProvider(ServletLifecycle.class);
   
   @Override
   public void initialize()
   {
      // No-op, we'll do the init ourselves!
   }

   /**
    * Starts the application
    * 
    * Runs the bootstrapper for bean discover and initialization
    * 
    * @param context The servlet context
    */
   public void beginApplication(ServletContext servletContext)
   {
      ServletInitialization servletInitialization = new ServletInitialization(servletContext).initialize();
      super.initialize();
      super.beginApplication(servletContext.getServletContextName(), new ApplicationBeanMap(servletContext));
      BeanMap requestBeanMap = new SimpleBeanMap();
      super.beginDeploy(requestBeanMap);
      servletInitialization.start();
      super.endDeploy(requestBeanMap);
   }

   /**
    * Ends the application
    */
   public void endApplication(ServletContext servletContext)
   {
      super.endApplication(servletContext.getServletContextName(), new ApplicationBeanMap(servletContext));
   }

   /**
    * Begins a session
    * 
    * @param session The HTTP session
    */
   public void beginSession(HttpSession session)
   {
      super.beginSession(session.getId(), null);
   }

   /**
    * Ends a session
    * 
    * @param session The HTTP session
    */
   public void endSession(HttpSession session)
   {
      super.endSession(session.getId(), restoreSessionContext(session));
   }
   
   /**
    * Restore the session from the underlying session object. Also allow the 
    * session to be injected by the Session manager
    * 
    * @param session
    * @return
    */
   protected BeanMap restoreSessionContext(HttpSession session)
   {
      BeanMap sessionBeanMap = new HttpSessionBeanMap(session);
      SessionContext.INSTANCE.setBeanMap(sessionBeanMap);
      CurrentManager.rootManager().getInstanceByType(HttpSessionManager.class).setSession(session);
      return sessionBeanMap;
   }
   
   /**
    * Begins a HTTP request
    * 
    * Sets the session into the session context
    * 
    * @param request The request
    */
   public void beginRequest(HttpServletRequest request)
   {
      restoreSessionContext(request.getSession());
      BeanMap beanMap = new SimpleBeanMap();
      request.setAttribute(REQUEST_ATTRIBUTE_NAME, beanMap);
      super.beginRequest(request.getRequestURI(), beanMap);
   }

   /**
    * Ends a HTTP request
    * 
    * @param request The request
    */
   public void endRequest(HttpServletRequest request)
   {
      BeanMap beanMap = (BeanMap) request.getAttribute(REQUEST_ATTRIBUTE_NAME);
      request.removeAttribute(REQUEST_ATTRIBUTE_NAME);
      super.endRequest(request.getRequestURI(), beanMap);
      SessionContext.INSTANCE.setBeanMap(null);
   }

}
