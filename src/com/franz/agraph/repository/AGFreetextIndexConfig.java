/******************************************************************************
** Copyright (c) 2008-2011 Franz Inc.
** All rights reserved. This program and the accompanying materials
** are made available under the terms of the Eclipse Public License v1.0
** which accompanies this distribution, and is available at
** http://www.eclipse.org/legal/epl-v10.html
******************************************************************************/

package com.franz.agraph.repository;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.rio.ntriples.NTriplesUtil;

public class AGFreetextIndexConfig {

	public static final String PREDICATES = "predicates";
	public static final String INDEX_LITERALS = "indexLiterals";
	public static final String INDEX_LITERAL_TYPES = "indexLiteralTypes";
	public static final String INDEX_RESOURCES = "indexResources";
	public static final String INDEX_FIELDS = "indexFields";
	public static final String MINIMUM_WORD_SIZE = "minimumWordSize";
	public static final String STOP_WORDS = "stopWords";
	public static final String WORD_FILTERS = "wordFilters";
	
	private static final ValueFactory vf = new ValueFactoryImpl(); 

	private List<URI> predicates;
	private boolean indexLiterals;
	private List<String> indexLiteralTypes;
	private String indexResources;
	private List<String> indexFields;
	private int minimumWordSize;
	private List<String> stopWords;
	private List<String> wordFilters;

	public static AGFreetextIndexConfig newInstance() {
		return new AGFreetextIndexConfig();
	}
	
	private AGFreetextIndexConfig() {	
		predicates = new ArrayList<URI>();
		indexLiterals = true;
		indexLiteralTypes = new ArrayList<String>();
		indexResources = "false";
		indexFields = new ArrayList<String>();
		indexFields.add("object");
		minimumWordSize = 3;
		stopWords = new ArrayList<String>();
		wordFilters = new ArrayList<String>();
	}
	
	AGFreetextIndexConfig(JSONObject config) {
		predicates = initPredicates(config);
		indexLiterals = initIndexLiterals(config);
		indexLiteralTypes = getJSONArrayAsListString(config, INDEX_LITERAL_TYPES);
		indexResources = config.optString(INDEX_RESOURCES);
		indexFields = getJSONArrayAsListString(config, INDEX_FIELDS);
		minimumWordSize = config.optInt(MINIMUM_WORD_SIZE);
		stopWords = getJSONArrayAsListString(config, STOP_WORDS);
		wordFilters = getJSONArrayAsListString(config, WORD_FILTERS);
	}
	
	private boolean initIndexLiterals(JSONObject config) {
		boolean bool;
		try {
			// Can't use optBoolean here, it defaults to false
			bool = config.getBoolean(INDEX_LITERALS);
		} catch (JSONException e) {
			// TODO: perhaps log this if it happens.
			bool = true;
		}
		return bool;
	}
	
	private List<URI> initPredicates(JSONObject config) {
		List<URI> predList = new ArrayList<URI>();
		JSONArray preds = config.optJSONArray(PREDICATES);
		if (preds!=null) {
			for (int i = 0; i < preds.length(); i++) {
				String uri_nt = preds.optString(i);
				URI uri = NTriplesUtil.parseURI(uri_nt, vf);
				predList.add(uri);
			}
		}
		return predList;
	}
	
	public List<URI> getPredicates() {
		return predicates;
	}
	
	public boolean getIndexLiterals() {
		return indexLiterals;
	}
	
	public void setIndexLiterals(boolean bool) {
		indexLiterals = bool;
	}
	
	public List<String> getIndexLiteralTypes() {
		return indexLiteralTypes;
	}
	
	public String getIndexResources() {
		return indexResources;
	}
	
	public void setIndexResources(String str) {
		//TODO: validity check
		indexResources = str;
	}
	
	public List<String> getIndexFields() {
		return indexFields;
	}
	
	public int getMinimumWordSize() {
		return minimumWordSize;
	}
	
	public void setMinimumWordSize(int size) {
		minimumWordSize = size;
	}
	
	public List<String> getStopWords() {
		return stopWords;
	}
	
	public List<String> getWordFilters() {
		return wordFilters;
	}
	
	private List<String> getJSONArrayAsListString(JSONObject config, String key) {
		List<String> list = new ArrayList<String>();
			JSONArray array = config.optJSONArray(key);
			if (array!=null) {
				for (int i = 0; i < array.length(); i++) {
					list.add(array.optString(i));
				}
			}
		return list;
	}
	
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("<AGFreetextIndexConfig predicates: ").append(predicates);
		sb.append(", indexLiterals: ").append(indexLiterals);
		sb.append(", indexLiteralTypes: ").append(indexLiteralTypes);
		sb.append(", indexResources: ").append(indexResources);
		sb.append(", indexFields: ").append(indexFields);
		sb.append(", minimumWordSize: ").append(minimumWordSize);
		sb.append(", stopWords: ").append(stopWords.size());
		sb.append(", wordFilters: ").append(wordFilters);
		sb.append(">");
		return sb.toString();
	}
}
