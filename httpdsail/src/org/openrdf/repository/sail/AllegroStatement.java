package org.openrdf.repository.sail;

import java.util.List;

import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.impl.BNodeImpl;
import org.openrdf.model.impl.LiteralImpl;
import org.openrdf.model.impl.StatementImpl;
import org.openrdf.model.impl.URIImpl;

import franz.exceptions.SoftException;

public class AllegroStatement extends StatementImpl {

	private Resource subject = null;
	private URI predicate = null;
	private Value object = null;
	private Resource context = null;
	private List<String> stringTuple = null;

	public AllegroStatement(URI subject, URI predicate, Value object) {
		super(subject, predicate, object);
		this.subject = subject;
		this.predicate = predicate;
		this.object = object;
	}

	protected void setQuad(List<String> stringTuple) {
		//System.out.println("SET QUAD STRING TUPLE " + stringTuple + "   CLASS: " + stringTuple.getClass());
		//System.out.println("   SUBJECT " + stringTuple.get(0) + "  CLASS: " + stringTuple.get(0).getClass());
		if (stringTuple.get(0).startsWith("\"_:"))
			System.out.println("BREEEEEEEAK");
		this.stringTuple = stringTuple;
	}
	
	public Resource getSubject() {
		//System.out.println("GET SUBJECT STRING TUPLE " + stringTuple + "   CLASS: " + stringTuple.getClass());
		
		if (this.subject == null) {
			this.subject = (Resource)this.stringTermToTerm(this.stringTuple.get(0));
		}
		return this.subject;
	}

	public URI getPredicate() {
		if (this.predicate == null) {
			this.predicate = (URI)this.stringTermToTerm(this.stringTuple.get(1));
		}
		return this.predicate;
	}

	public Value getObject() {
		if (this.object == null) {
			this.object = (Value)this.stringTermToTerm(this.stringTuple.get(2));
		}
		return this.object;
	}

	public Resource getContext() {
		if (this.context == null) {
			if (this.stringTuple.size() > 3) {
				this.context = (URI)this.stringTermToTerm(this.stringTuple.get(3));
			} else {
				this.context = null;
			}
		}
		return this.context;
	}

	/**
     * Given a string representing a term in ntriples format, return
     * a URI, Literal, or BNode.
	 */
    protected static Value stringTermToTerm(String stringTerm) {
    	//System.out.println("STRING TERM TO TERM '" + stringTerm + "'");
        if (stringTerm == null) return null;
        if (stringTerm.charAt(0) == '<') {
            String uri = stringTerm.substring(1, stringTerm.length() - 1);
            return new URIImpl(uri);
        }
        else if (stringTerm.charAt(0) == '"') {
            // we have a double-quoted literal with either a data type or a language indicator
            int caratPos = stringTerm.indexOf("^^");
            if (caratPos >= 0) {
                String label = stringTerm.substring(1, caratPos - 1);
                String dType = stringTerm.substring(caratPos + 2);
                URI datatype = (URI)stringTermToTerm(dType);
                return new LiteralImpl(label, datatype);
            }
            int atPos = stringTerm.indexOf('@');
            if (atPos >=0) {
                String label = stringTerm.substring(1,atPos - 1);
                String language = stringTerm.substring(atPos + 1);
                return new LiteralImpl(label, language);
            } else {
                return new LiteralImpl(stringTerm.substring(1, stringTerm.length() - 1));
            }
        } else if (stringTerm.charAt(0) == '_' && stringTerm.charAt(1) == ':') {
        	return new BNodeImpl(stringTerm.substring(2));
        } else {
        	throw new SoftException("Cannot translate '" + stringTerm + "' into an OpenRDF term.");
        }
    }
        
    /**
     * Given a string representing a term in ntriples format, return
     * a URI or the label portion of a literal (a string minus the double quotes).
     * TODO: IMPLEMENT BNODES
     */
    protected static String ntriplesStringToStringValue(String stringTerm) {
        if (stringTerm == null) return null;
        char firstChar = stringTerm.charAt(0);
        if (firstChar == '<') {
            String uri = stringTerm.substring(1, stringTerm.length() - 1);
            return uri;
        }
        else if (firstChar == '"') {
            // we have a double-quoted literal with either a data type or a language indicator
            int caratPos = stringTerm.indexOf("^^");
            if (caratPos >= 0) {
                String label = stringTerm.substring(1, caratPos - 1);
                return label;
            }
            int atPos = stringTerm.indexOf('@');
            if (atPos >=0) {
                String label = stringTerm.substring(1,atPos - 1);
                return label;
            } else {
                return stringTerm.substring(1, stringTerm.length() - 1);
            }
        } else if (firstChar == '_' && stringTerm.charAt(1) == ':') {
        	return stringTerm;
        } else {
        	throw new SoftException("Cannot translate '" + stringTerm + "' into a string value.");
        }
    }

  	/**
   	 * Return a String-representation of this Statement that can be used for debugging.
   	 */
   	public String toString() {
   		StringBuilder sb = new StringBuilder(256);
		sb.append("(");
		sb.append(getSubject());
		sb.append(", ");
		sb.append(getPredicate());
		sb.append(", ");
		sb.append(getObject());
		Resource cxt = this.getContext();
		if (cxt != null)
			sb.append(", " + cxt);
		sb.append(")");
		return sb.toString();
	}


}
