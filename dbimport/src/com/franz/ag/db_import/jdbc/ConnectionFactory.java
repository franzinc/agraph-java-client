package com.franz.ag.db_import.jdbc;

import java.net.URL;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.util.Properties;


import org.apache.log4j.Logger;

/**
* Modified substantially.  Originally by author below:
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

public class ConnectionFactory {
  private String odbc = null;
  private String jdbc = null;
  private String jdbcDriver = null;
  private String databaseUsername = null;
  private String databasePassword = null;

  /** log4j logger used for this class */
  private static Logger log = Logger.getLogger(ConnectionFactory.class);

  /** JDBC Connection used to retrieve data */
  private Connection connection = null;

  /** Classpath of JDBC Driver (JAR) used to establish the connection */
  private URL driverClasspath = null;


  public ConnectionFactory() {}
  
  public void setJDBCDriverClass (String driverClass) {
	  this.jdbcDriver = driverClass;
  }
  
  public void setODBCDSN (String odbcDSN) {
	  this.odbc = odbcDSN;
  }
  public void setJDBCDSN (String jdbcDSN) {
	  this.jdbc = jdbcDSN;
  }
  
  public void setUserName (String username) {
	  this.databaseUsername = username;
  }
  
  public void setPassword (String pwd) {
	  this.databasePassword = pwd;
  }


  /**
   * If a valid Connection has previously been set/created, it will be returned.
   * Otherwise a new connection will be made and will be cached for the next
   * call.
   *
   * NOTE: It is assumed the connection will be closed (or set to null) when
   * it is no longer needed (processing is complete).
   *
   * @throws SQLException
   * @return Connection
   */
  public Connection getConnection() throws SQLException {

    //value to be returned
    Connection conn = null;
    //if there is a previous connection use it, otherwise try to create
    //one using the details supplied
    try {
      //validate connection
      if ( (this.connection != null)  && (!this.connection.isClosed())) {
        if (log.isDebugEnabled()) {
          log.debug("Retreving existing connection.");
        }
        //use existing connection
        conn = this.connection;
      }
      else {
        // Connect to database
        String url = "";
        //setup required information
        if (this.getOdbc() != null) {
          url = "jdbc:odbc:" + this.getOdbc();
        }
        else if (this.getJdbc() != null) {
          url = this.getJdbc();
        }
        if (log.isDebugEnabled()) {
          log.debug("Creating new connection. URL: " + url);
        }
        //make a new connection
        if (url != "") {
          //Driver used to establish connection
          Driver driver = this.createDriver();
          //use the Driver to establish a connection
          Properties connectionProperties = new Properties();
          //add the username and password to the properties
          if (this.getDatabaseUsername() != null &&
              this.getDatabasePassword() != null) {
            connectionProperties.setProperty("user", this.getDatabaseUsername());
            connectionProperties.setProperty("password",
                                             this.getDatabasePassword());
          }
          else {

            //no username/password supplied, used empty values
            connectionProperties.setProperty("user", "");
            connectionProperties.setProperty("password", "");
          }
          //connect to the URL using the Driver
          if (driver != null) {
            conn = driver.connect(url, connectionProperties);
          } else {

            throw new SQLException("Could not establish Connection. " +
                                   "Cannot obtain Driver.");
          }
        }
        else {
          throw new SQLException(
              "Could not connect to database because of missing URL.");
        }
        //cache connection
        this.setConnection(conn);
      }
    }
    catch (SQLException ex) {
      String message = "SQL Exception caught: ";
      while (ex != null) {
        message += " SQLState: " + ex.getSQLState();
        message += "Message:  " + ex.getMessage();
        message += "Vendor:   " + ex.getErrorCode();
        ex = ex.getNextException();
      }
      throw new SQLException(message);
    }
    if (log.isDebugEnabled()) {
      log.debug("Returning connection: " + conn);
    }
    return conn;
  }

  /**
   * Creates a new JDBC Driver from this Object's Connection properties.
   *
   * @throws SQLException
   * @return Driver
   */
  private Driver createDriver() throws SQLException {
    //value to be returned
    Driver driver = null;
    //name of Driver Class
    String driverClass = null;
    try {
      //get required information
      if (this.getOdbc() != null) {
        driverClass = "sun.jdbc.odbc.JdbcOdbcDriver";
      }
      else if (this.getJdbcDriver() != null) {
        driverClass = this.getJdbcDriver();
      }
      else {
        throw new SQLException("Could not connect to database because of " +
                               "missing Driver.");
      }
      //if there is a classpath supplied, use it to instantiate Driver
      if (this.getDriverClasspath() != null) {
        //dynamically load and instantiate Driver from the classPath URL
        driver = DriverFactory.getInstance().getDriverInstance(driverClass,
            this.getDriverClasspath());
      }
      else {
        //attempt to load and instantiate Driver from the current classpath
        driver = DriverFactory.getInstance().getDriverInstance(driverClass);
      }
    }
    catch (SQLException ex) {
      throw new SQLException("Could not instantiate Driver class because " + ex.getMessage());
    }
    return driver;
  }

  /**
   * Sets the Connection member.
   * @param connection Connection
   */
  public void setConnection(Connection connection) {
    this.connection = connection;
  }

  /**
   * Sets the driver classpath member.
   *
   * @param connection Connection
   */
  public void setDriverClasspath(URL classpath) {
    this.driverClasspath = classpath;
  }

  /**
   * Returns the classpath that is used to load JDBC Drivers.
   *
   * @return URL
   */
  public URL getDriverClasspath(){
    return this.driverClasspath;
  }

  /**
   * Returns the ODBC data source name.
   * @return odbcDSN
   */
  private String getOdbc() {
    return this.odbc;
  }

  /**
   * Returns the JDBC data source name.
   * @return jdbcDSN
   */
  private String getJdbc() {
    return this.jdbc;
  }

  /**
   * Returns the JDBC driver.
   * @return jdbcDriver
   */
  private String getJdbcDriver() {
    return this.jdbcDriver;
  }

  /**
   * Returns the database username.
   * @return username
   */
  private String getDatabaseUsername() {
    return this.databaseUsername;
  }

  /**
   * Returns the database password.
   * @return password
   */
  private String getDatabasePassword() {
    return this.databasePassword;
  }

 }
