/*******************************************************************************
 * Copyright (c) 2009 TopQuadrant, Inc.
 * All rights reserved. 
 *******************************************************************************/
package com.franz.agraph.query.contexthelper;

import java.util.Iterator;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.topbraid.core.labels.DisplayLabels;
import org.topbraid.sparql.functionmetadata.FunctionsMetadata;
import org.topbraid.sparql.functionmetadata.IArgumentMetadata;
import org.topbraid.sparql.functionmetadata.IFunctionMetadata;
import org.topbraidcomposer.ui.contexthelpers.DefaultContextHelper;

import com.hp.hpl.jena.rdf.model.Resource;


public class SPARQLFunctionContextHelper extends DefaultContextHelper {
	public String getText(Resource resource, List<StyleRange> styleRanges) {
		if(resource.isURIResource()) {
			String uri = resource.getURI();
			IFunctionMetadata function = FunctionsMetadata.get().getMetadata(uri);
			if(function != null) { //  && FunctionRegistry.get().isRegistered(uri)) {
				StringBuffer sb = new StringBuffer();
				
				String label = function.getName();
				sb.append(label);
				StyleRange styleRange = new StyleRange(0, label.length(), null, null, SWT.BOLD);
				styleRanges.add(styleRange);
				
				List<IArgumentMetadata> arguments = function.getArguments();
				sb.append(" (");
				for(Iterator<IArgumentMetadata> it = arguments.iterator(); it.hasNext(); ) {
					IArgumentMetadata arg = it.next();
					sb.append("?");
					sb.append(arg.getName());
					if(it.hasNext()) {
						sb.append(", ");
					}
				}
				sb.append(")");
				Resource outputType = function.getResultType();
				if(outputType != null) {
					sb.append(" : ");
					DisplayLabels.append(sb, outputType);
				}
				
				sb.append("\n\n");
				
				String functionComment = function.getDescription();
				if(functionComment != null) {
					sb.append(functionComment);
				}

				sb.append("\n\n");
				String heading = "Arguments:";
				styleRanges.add(new StyleRange(sb.length(), heading.length(), null, null, SWT.BOLD));
				sb.append(heading);
				for(IArgumentMetadata arg : arguments) {
					String localName = arg.getName();
					String comment = arg.getDescription();
					sb.append("\n - ");
					styleRanges.add(new StyleRange(sb.length(), localName.length(), null, null, SWT.BOLD));
					sb.append(localName);
					Resource type = arg.getType();
					if(type != null) {
						sb.append(" (");
						DisplayLabels.append(sb, type);
						sb.append(")");
					}
					sb.append(": ");
					sb.append(comment);
				}
				
				return sb.toString();
			}
		}
		return null;
	}

	
	/*
	@Override
	public String getText(Resource resource, List<StyleRange> styleRanges) {
		if(resource.isURIResource()) {
			Model model = FunctionsLibraries.getUnionModel();
			// model.contains(resource, RDF.type, (RDFNode)null)
			Resource inModel = (Resource)resource.inModel(model);
			if(FunctionsLibraries.isSMFunction(inModel) && FunctionRegistry.get().isRegistered(resource.getURI())) {
				resource = inModel;
				StringBuffer sb = new StringBuffer();
				
				String label = Labels.getLabel(resource);
				sb.append(label);
				StyleRange styleRange = new StyleRange(0, label.length(), null, null, SWT.BOLD);
				styleRanges.add(styleRange);

				final Map<Property,Integer> indexMap = new HashMap<Property,Integer>();
				final Map<Property,String> commentMap = new HashMap<Property,String>();
				StmtIterator rit = resource.listProperties(RDFS.subClassOf);
				while(rit.hasNext()) {
					Statement s = rit.nextStatement();
					if(s.getObject().isResource()) {
						Resource restriction = s.getResource();
						Statement indexS = restriction.getProperty(SM.argumentIndex);
						if(indexS != null && indexS.getObject().isLiteral()) {
							int index = indexS.getInt();
							Statement propertyS = restriction.getProperty(OWL.onProperty);
							Property property = Properties.asOntProperty(propertyS.getResource());
							if(propertyS != null && propertyS.getObject().isResource()) {
								indexMap.put(property, index);
							}
							Statement commentS = restriction.getProperty(RDFS.comment);
							if(commentS != null && commentS.getObject().isLiteral()) {
								commentMap.put(property, commentS.getString());
							}
						}
					}
				}
				List<Property> properties = new ArrayList<Property>(indexMap.keySet());
				if(properties.size() > 1) {
					Collections.sort(properties, new Comparator<Property>() {
						public int compare(Property o1, Property o2) {
							return indexMap.get(o1).compareTo(indexMap.get(o2));
						}
					});
				}
				
				sb.append(" (");
				for(Iterator<Property> pit = properties.iterator(); pit.hasNext(); ) {
					Property property = pit.next();
					sb.append("?");
					sb.append(property.getLocalName());
					if(pit.hasNext()) {
						sb.append(", ");
					}
				}
				sb.append(")");
				Statement outputTypeS = resource.getProperty(SM.outputType);
				if(outputTypeS != null && outputTypeS.getObject().isResource()) {
					sb.append(" : ");
					Labels.append(sb, outputTypeS.getResource());
				}
				
				sb.append("\n\n");
				
				Statement commentS = resource.getProperty(RDFS.comment);
				if(commentS != null && commentS.getObject().isLiteral()) {
					String comment = commentS.getString();
					sb.append(comment);
				}

				sb.append("\n\n");
				String heading = "Arguments:";
				styleRanges.add(new StyleRange(sb.length(), heading.length(), null, null, SWT.BOLD));
				sb.append(heading);
				for(Property property : properties) {
					String localName = property.getLocalName();
					String comment = commentMap.get(property);
					sb.append("\n - ");
					styleRanges.add(new StyleRange(sb.length(), localName.length(), null, null, SWT.BOLD));
					sb.append(localName);
					Statement rangeS = property.getProperty(RDFS.range);
					String range;
					if(rangeS != null && rangeS.getObject().isURIResource()) {
						range = rangeS.getResource().getLocalName();
					}
					else {
						if(property.hasProperty(RDF.type, OWL.DatatypeProperty)) {
							range = "Literal";
						}
						else if(property.hasProperty(RDF.type, RDF.Property)) {
							range = "Literal or Resource";
						}
						else {
							range = "Resource";
						}
					}
					sb.append(" (");
					sb.append(range);
					sb.append("): ");
					sb.append(comment);
				}
				
				return sb.toString();
			}
		}
		return null;
	}*/
}
