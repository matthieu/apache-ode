/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.ode.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extensions for java.lang.System
 */

public class SystemUtils {

	/**
	 * @see System#getProperties()
	 */
	public static String javaVersion() {
		return System.getProperty("java.version");
	}

	/**
	 * @see System#getProperties()
	 */
	public static String javaVendor() {
		return System.getProperty("java.vendor");
	}

	/**
	 * @see System#getProperties()
	 */
	public static String javaHome() {
		return System.getProperty("java.home");
	}

	/**
	 * @see System#getProperties()
	 */
	public static String javaClassVersion() {
		return System.getProperty("java.class.version");
	}

	/**
	 * @see System#getProperties()
	 */
	public static String javaClassPath() {
		return System.getProperty("java.class.path");
	}

	/**
	 * @see System#getProperties()
	 */
	public static String javaTemporaryDirectory() {
		return System.getProperty("java.io.tmpdir");
	}

	/**
	 * @see System#getProperties()
	 */
	public static String javaLibraryPath() {
		return System.getProperty("java.library.path");
	}

	/**
	 * @see System#getProperties()
	 */
	public static String operatingSystemArchitecture() {
		return System.getProperty("os.arch");
	}

	/**
	 * @see System#getProperties()
	 */
	public static String operatingSystemName() {
		return System.getProperty("os.name");
	}

	/**
	 * @see System#getProperties()
	 */
	public static String operatingSystemVersion() {
		return System.getProperty("os.version");
	}

	/**
	 * @see System#getProperties()
	 */
	public static String fileSeparator() {
		return System.getProperty("file.separator");
	}

	/**
	 * @see System#getProperties()
	 */
	public static String pathSeparator() {
		return System.getProperty("path.separator");
	}

	/**
	 * @see System#getProperties()
	 */
	public static String lineSeparator() {
		return System.getProperty("line.separator");
	}

	/**
	 * @see System#getProperties()
	 */
	public static String userName() {
		return System.getProperty("user.name");
	}

	/**
	 * @see System#getProperties()
	 */
	public static String userHome() {
		return System.getProperty("user.home");
	}

	/**
	 * @see System#getProperties()
	 */
	public static String userDirectory() {
		return System.getProperty("user.dir");
	}

	/**
	 * Replace system property values in the given String using the ${system.property} convention.
	 *
	 * e.g., "The java version is ${java.version}" ==> "The java version is 1.5.0_11"
	 */
	public static String replaceSystemProperties(String str) {
		Pattern pattern = Pattern.compile("\\$\\{[^\\}]+\\}");
		int start = 0;
		while (true) {
			Matcher match = pattern.matcher(str);
			if (!match.find(start))
				break;
			String property = str.substring(match.start() + 2, match.end() - 1);
			String value = System.getProperty(property);
			if (value != null) {
				str = match.replaceFirst(Matcher.quoteReplacement(value));
			} else {
				// if the system property doesn't exist, no substitution and skip to next
				start = match.end();
			}
		}
		return str;
	}

}
