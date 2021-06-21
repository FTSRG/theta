/*
 *  Copyright 2017 Budapest University of Technology and Economics
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package hu.bme.mit.theta.common.visualization;

import static com.google.common.base.Preconditions.checkNotNull;

import java.awt.Color;

public final class NodeAttributes {
	private final String label;
	private final Color lineColor;
	private final Color fillColor;
	private final LineStyle lineStyle;
	private final String font;
	private final int peripheries;
	private final Shape shape;
	private final Alignment alignment;
	private final boolean invisible;

	private NodeAttributes(final String label, final Color lineColor, final Color fillColor, final LineStyle lineStyle,
						   final String font, final int peripheries, final Shape shape, final Alignment alignment, final boolean invisible) {
		this.label = checkNotNull(label);
		this.lineColor = checkNotNull(lineColor);
		this.fillColor = checkNotNull(fillColor);
		this.lineStyle = checkNotNull(lineStyle);
		this.font = checkNotNull(font);
		this.peripheries = peripheries;
		this.shape = checkNotNull(shape);
		this.alignment = checkNotNull(alignment);
		this.invisible = invisible;
	}

	public String getLabel() {
		return label;
	}

	public Color getLineColor() {
		return lineColor;
	}

	public Color getFillColor() {
		return fillColor;
	}

	public LineStyle getLineStyle() {
		return lineStyle;
	}

	public String getFont() {
		return font;
	}

	public int getPeripheries() {
		return peripheries;
	}

	public Shape getShape() {
		return shape;
	}

	public Alignment getAlignment() {
		return alignment;
	}

	public boolean getInvisible() { return invisible; }

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {
		private String label = "";
		private Color lineColor = Color.BLACK;
		private Color fillColor = Color.WHITE;
		private LineStyle lineStyle = LineStyle.NORMAL;
		private String font = "";
		private int peripheries = 1;
		private Shape shape = Shape.ELLIPSE;
		private Alignment alignment = Alignment.CENTER;
		private boolean invisible = false;

		public Builder label(final String label) {
			this.label = label;
			return this;
		}

		public Builder lineColor(final Color lineColor) {
			this.lineColor = lineColor;
			return this;
		}

		public Builder fillColor(final Color fillColor) {
			this.fillColor = fillColor;
			return this;
		}

		public Builder lineStyle(final LineStyle lineStyle) {
			this.lineStyle = lineStyle;
			return this;
		}

		public Builder font(final String font) {
			this.font = font;
			return this;
		}

		public Builder peripheries(final int peripheries) {
			this.peripheries = peripheries;
			return this;
		}

		public Builder shape(final Shape shape) {
			this.shape = shape;
			return this;
		}

		public Builder alignment(final Alignment alignment) {
			this.alignment = alignment;
			return this;
		}

		public Builder invisible(final boolean invisible) {
			this.invisible = invisible;
			return this;
		}

		public NodeAttributes build() {
			return new NodeAttributes(label, lineColor, fillColor, lineStyle, font, peripheries, shape, alignment, invisible);
		}
	}
}
