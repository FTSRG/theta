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

import com.google.common.base.Preconditions;
import hu.bme.mit.theta.xcfa.dsl.gen.XcfaDslLexer;
import hu.bme.mit.theta.xcfa.dsl.gen.XcfaDslParser;
import hu.bme.mit.theta.xcfa.dsl.gen.XcfaDslParser.SpecContext;
import hu.bme.mit.theta.xcfa.model.XCFA;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public final class XcfaDslManager {

	private XcfaDslManager() {
	}

	public static XCFA createXcfa(final String inputString) throws IOException {
		final InputStream stream = new ByteArrayInputStream(inputString.getBytes(StandardCharsets.UTF_8.name()));
		return createXcfa(stream);
	}

	public static XCFA createXcfa(final InputStream inputStream) throws IOException {
		final CharStream input = CharStreams.fromStream(inputStream);

		final XcfaDslLexer lexer = new XcfaDslLexer(input);
		final CommonTokenStream tokens = new CommonTokenStream(lexer);
		final XcfaDslParser parser = new XcfaDslParser(tokens);

		final SpecContext context = parser.spec();
		final XcfaSymbol specification = new XcfaSymbol(context);
		return specification.instantiate();
	}

	public static XCFA createXcfa(final InputStream[] inputStreams) throws IOException {
		Preconditions.checkArgument(inputStreams.length != 0,
				"At least one XCFA file is required.");
		XcfaSymbol specification = null;
		for (InputStream inputStream : inputStreams) {
			final CharStream input = CharStreams.fromStream(inputStream);

			final XcfaDslLexer lexer = new XcfaDslLexer(input);
			final CommonTokenStream tokens = new CommonTokenStream(lexer);
			final XcfaDslParser parser = new XcfaDslParser(tokens);

			final SpecContext context = parser.spec();
			if (specification == null) {
				specification = new XcfaSymbol(context);
			} else {
				specification.addContext(context);
			}
		}
		return specification.instantiate();
	}

}
