/*
 * Copyright 2021 Budapest University of Technology and Economics
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package hu.bme.mit.theta.xcfa.dsl;

import hu.bme.mit.theta.xcfa.dsl.gen.XcfaDslParser.LocContext;
import hu.bme.mit.theta.xcfa.model.XcfaLocation;

import java.util.HashMap;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

final class XcfaLocationSymbol extends InstantiatableSymbol<XcfaLocation> {

	private final boolean init;
	private final boolean finall;
	private final boolean error;
	/**
	 * Name passed to Location to uniquely identify the location
	 */
	private final String canonicalName;
	/**
	 * Name used for location resolution
	 */
	private final String name;
	private final Map<String, String> dictionary;
	private XcfaLocation loc = null;

	XcfaLocationSymbol(final XcfaProcedureSymbol parent, final LocContext context) {
		checkNotNull(context);
		init = (context.init != null);
		finall = (context.finall != null);
		error = (context.error != null);
		name = context.id.getText();
		canonicalName = parent.getCanonicalName() + "::" + name;
		dictionary = new HashMap<>();
	}

	@Override
	public String getName() {
		return name;
	}

	public boolean isInit() {
		return init;
	}

	public boolean isFinal() {
		return finall;
	}

	public boolean isError() {
		return error;
	}

	public XcfaLocation instantiate() {
		if (loc != null) return loc;
		return loc = new XcfaLocation(canonicalName, dictionary);
	}

	void addDictionaryEntry(final String key, final String value) {
		dictionary.put(key, value);
	}

	public Map<String, String> getDictionary() {
		return dictionary;
	}
}
