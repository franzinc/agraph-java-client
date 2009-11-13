package com.franz.agbase;

/**
 * A UPI is an internal AllegroGraph part identifier.
 * 
 * There are no public constructors or accessors.
 * Instances are created when triples or triple parts are retrieved from the server.
 * A retained UPI is an efficient and unambiguous way to denote a known triple part. 
 * @author mm
 *
 */
public interface UPI extends com.franz.ag.UPI {

}
