/*
 * The contents of this file are subject to the GNU Lesser General Public
 * License Version 2.1 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.opensource.org/licenses/lgpl-license.php
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 *
 * The Original Code is D2R Map.
 *
 * The Initial Developer of the Original Code is Plugged In Software Pty
 * Ltd (http://www.pisoftware.com, mailto:info@pisoftware.com). Portions
 * created by Plugged In Software Pty Ltd are Copyright (C) 2001,2002
 * Plugged In Software Pty Ltd. All Rights Reserved.
 *
 * Contributor(s): Robert Turner, Chris Bizer.
 *
 * [NOTE: The text of this Exhibit A may differ slightly from the text
 * of the notices in the Source Code files of the Original Code. You
 * should use the text of this Exhibit A rather than the text found in the
 * Original Code Source Code for Your Modifications.]
 *
 */
package com.franz.ag.db_import.jdbc;

//Standard Java Libraries
import java.io.DataInputStream;
import java.io.IOException;
import java.net.*;
import java.sql.*;
import java.util.Properties;

//Log4J Libraries
import org.apache.log4j.Logger;

/**
 * Modified substantially.  Originally by author below:
 * 
 * Factory class used to instantiate instances of JDBC Drivers.
 * Implementations are defined in the factories properties file.
 * Implementations can also be instantiated using the class name and the
 * implementation's classpath.
 *
 * @created 2004-05-26
 *
 * @author <a href="mailto:robert.turner@tucanatech.com">Robert Turner</a>
 *
 * @version $Revision: 1.1 $
 *
 * @modified $Date: 2008/10/18 00:01:04 $ by $Author: bmacgregor $
 *
 * @company: <a href="http://www.tucanatech.com/">Tucana Technologies</a>
 *
 * @copyright &copy;2002-2003
 *   <a href="http://www.pisoftware.com/">Plugged In Software Pty Ltd</a>
 *
 */
public class DriverFactory  {

  /** log4j logger used for this class */
  private static Logger log = Logger.getLogger(DriverFactory.class);
  private static Properties properties;

  /** The singleton instance of this class */
  private static DriverFactory instance = null;


  /**
   * Constructor.  Creates and initializes the factory.
   */
  private DriverFactory() throws SQLException {

  }
  
  private void initializeFactory(String propertiesFilename) throws
  SQLException {
	
	// Initialize our properties
	properties = new Properties();
	DataInputStream inputStream = null;
	try {
	  // Open up an input stream to the properties file
	  URL resource = this.getClass().getResource(propertiesFilename);
	  if (resource != null) {
	    inputStream = new DataInputStream(this.getClass().getResource(
	        propertiesFilename).openStream());
	    properties.load(inputStream);
	  }
	}
	catch (IOException ioException) {
	  log.error("Could not load properties from properties file.", ioException);
	  throw new SQLException(
	      "Could not load properties from properties file because " + ioException.getMessage());
	}
	finally {
	  if (inputStream != null) {
	    try {
	      inputStream.close();
	    }
	    catch (IOException ioException) {
	      log.error("Could not shut down the input stream.", ioException);
	    }
	  }
	}
}

  /**
   * Creates a JDBC Driver instance of the specified class name. Loads and
   * instantiates the class with the System Class Loader.
   *
   * @param className String
   * @throws SQLException
   * @return Driver
   */
  public Driver getDriverInstance(String className)
      throws SQLException {
    //value to be returned
    Driver driver = null;
    try {
      Class driverClass = Class.forName(className);
      //get instance of Class from the System ClassLoader
      Object object = driverClass.newInstance();
      if ( (object != null)
          && (object instanceof Driver)) {
        driver = (Driver) object;
      }
      else {
        throw new SQLException("Could not create Driver instance. " +
                                   className + " may not be a valid " +
                                   "implementation of java.sql.Driver. ");
      }
    }
    catch (Exception ex) {
    	if (ex instanceof RuntimeException) throw (RuntimeException)ex;
    	else throw new SQLException("Failed to create driver instance because " + ex.getMessage());
    }
    return driver;
  }
 
  public static DriverFactory getInstance() throws SQLException {
    if (instance == null) {
      synchronized (DriverFactory.class) {
        if (instance == null) {
          // Create the factory
          instance = new DriverFactory();
          // Initialize the factory
          instance.initializeFactory("/driverFactory.properties");
        }
      }
    }
    return instance;
  }

  /**
   * Loads the class (className) from the path URL (classPathURL) and returns an
   * instance of the Class.
   */
  private Class getClassFromURL(URL classPath, String className) throws
      SQLException {
    //value to be returned
    Class loadedClass = null;
    if ( (classPath != null)  && (className != null)) {
      try {
        //get the class from the specified classpath using a seperate loader;
        URLClassLoader loader = URLClassLoader.newInstance(new URL[] {
            classPath}, ClassLoader.getSystemClassLoader());
        loadedClass = loader.loadClass(className);
      }
      catch (ClassNotFoundException ex) {
        throw new SQLException("Could not load Class: " + className + "." +
                                   "The classpath: " + classPath +
                                   " may not be valid because " + ex.getMessage());
      }
    }
    return loadedClass;
  }
  
  /**
   * Loads the class (className) from the path URL (classPathURL) and returns an
   * instance of the Class.
     */
  private Object getInstanceFromURL(URL classPath, String className) throws
      SQLException {
    //value to be returned
    Object instance = null;
    if ( (classPath != null) && (className != null)) {
      try {
        //used to dynamically create an instance of the supplied Driver Class
        Class loadedClass = this.getClassFromURL(classPath, className);
        instance = loadedClass.newInstance();
      }
      catch (Exception ex) {
    	  if (ex instanceof RuntimeException) throw (RuntimeException)ex;
      }
    }

    return instance;
  }

  /**
   * Creates a JDBC Driver instance of the specified class name from the
   * supplied classpath.
   *
   * @param className String
   * @param classPath URL
   * @throws SQLException
   * @return Driver
   */
  public Driver getDriverInstance(String className, URL classPath)
      throws SQLException {
    //value to be returned
    Driver driver = null;
    //load the class from the URL and instantiate
    Object object = this.getInstanceFromURL(classPath, className);
    if ((object != null) && (object instanceof Driver)) {
      driver = (Driver) object;
    } else {
      throw new SQLException("Could not create Driver instance. " +
                                 className + " may not be a valid " +
                                 "implementation of java.sql.Driver. ");
    }
    return driver;
  }

}
