/******************************************************************************
** Copyright (c) 2008-2009 Franz Inc.
** All rights reserved. This program and the accompanying materials
** are made available under the terms of the Eclipse Public License v1.0
** which accompanies this distribution, and is available at
** http://www.eclipse.org/legal/epl-v10.html
******************************************************************************/

package test;
import junit.framework.TestCase;
import junit.framework.TestResult;

public class RunTestMethods {

	/**
	 * One way to run a subset of the AGRepositoryConnectionTest methods.
	 * TODO: generalize.
	 * 
	 * @param args the test method names to run
	 */
	public static void main(String[] args) {
		for (String testName : args) {
			TestCase test = new AGRepositoryConnectionTest(testName);
			TestResult result = test.run();
			System.out.println(testName + ": " + result.wasSuccessful());
		}
	}

}
