/* ============================================================
 * JRobin : Pure java implementation of RRDTool's functionality
 * ============================================================
 *
 * Project Info:  http://www.sourceforge.net/projects/jrobin
 * Project Lead:  Sasa Markovic (saxon@eunet.yu);
 *
 * (C) Copyright 2003, by Sasa Markovic.
 *
 * This library is free software; you can redistribute it and/or modify it under the terms
 * of the GNU Lesser General Public License as published by the Free Software Foundation;
 * either version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with this
 * library; if not, write to the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, MA 02111-1307, USA.
 */

package jrobin.core;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.text.DecimalFormat;

class XmlWriter {
	static final DecimalFormat df = new DecimalFormat("0.0000000000E00");
	static final String INDENT_STR = "   ";

	private PrintWriter writer;
	private StringBuffer indent = new StringBuffer("");
	private Stack openTags = new Stack();

	XmlWriter(OutputStream stream) {
		writer = new PrintWriter(stream);
	}

	void startTag(String tag) {
		writer.println(indent + "<" + tag + ">");
		openTags.push(tag);
		indent.append(INDENT_STR);
	}

	void closeTag() {
		String tag = openTags.pop();
		indent.setLength(indent.length() - INDENT_STR.length());
		writer.println(indent + "</" + tag + ">");
	}

	void writeTag(String tag, Object value) {
		writer.println(indent + "<" + tag + ">" + value + "</" + tag + ">");
	}

	void writeTag(String tag, int value) {
		writeTag(tag, "" + value);
	}

	void writeTag(String tag, long value) {
		writeTag(tag, "" + value);
	}

	void writeTag(String tag, double value, String nanString) {
		if(Double.isNaN(value)) {
			writeTag(tag, nanString);
		}
		else {
			writeTag(tag, df.format(value));
		}
	}

	void writeTag(String tag, double value) {
		writeTag(tag, value, "" + Double.NaN);
	}

	void finish() {
		writer.flush();
	}

	protected void finalize() {
		writer.close();
	}

	void writeComment(Object comment) {
		writer.println(indent + "<!-- " + comment + " -->");
	}

	private class Stack {
		private ArrayList stack = new ArrayList();

		void push(String tag) {
			stack.add(tag);
		}

		String pop() {
			int last = stack.size() - 1;
			if(last >= 0) {
				String tag = (String) stack.get(last);
				stack.remove(last);
				return tag;
			}
			return null;
		}
	}
}