/*******************************************************************************
 * Copyright (c) 2009, 2021 Mountainminds GmbH & Co. KG and Contributors
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Marc R. Hoffmann - initial API and implementation
 *
 *******************************************************************************/
package org.jacoco.core.internal.data;

import com.test.diff.common.util.FastJsonUtil;

import org.jacoco.core.data.ChainNode;

import java.io.*;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Additional data output methods for compact storage of data structures.
 *
 * @see CompactDataInput
 */
public class CompactDataOutput extends DataOutputStream {

	/**
	 * Creates a new {@link CompactDataOutput} instance that writes data to the
	 * specified underlying output stream
	 *
	 * @param out
	 *            underlying output stream
	 */
	public CompactDataOutput(final OutputStream out) {
		super(out);
	}

	/**
	 * Writes a variable length representation of an integer value that reduces
	 * the number of written bytes for small positive values. Depending on the
	 * given value 1 to 5 bytes will be written to the underlying stream.
	 *
	 * @param value
	 *            value to write
	 * @throws IOException
	 *             if thrown by the underlying stream
	 */
	public void writeVarInt(final int value) throws IOException {
		if ((value & 0xFFFFFF80) == 0) {
			writeByte(value);
		} else {
			writeByte(0x80 | (value & 0x7F));
			writeVarInt(value >>> 7);
		}
	}

	/**
	 * Writes a boolean array. Internally a sequence of boolean values is packed
	 * into single bits.
	 *
	 * @param value
	 *            boolean array
	 * @throws IOException
	 *             if thrown by the underlying stream
	 */
	public void writeBooleanArray(final boolean[] value) throws IOException {
		writeVarInt(value.length);
		int buffer = 0;
		int bufferSize = 0;
		for (final boolean b : value) {
			if (b) {
				buffer |= 0x01 << bufferSize;
			}
			if (++bufferSize == 8) {
				writeByte(buffer);
				buffer = 0;
				bufferSize = 0;
			}
		}
		if (bufferSize > 0) {
			writeByte(buffer);
		}
	}

	public void writeSetArray(final HashSet[] sets) throws IOException {
		// int count = 1;
		writeVarInt(sets.length);
		for (Set set : sets) {
			// System.out.println("send ----" + count);
			// count++;
			Iterator iterator = set.iterator();
			while (iterator.hasNext()) {
				Object object = iterator.next();
				if (object == null) {
					object = "";
				}
				String uri = String.valueOf(object);
				writeUTF(uri);
			}
			writeUTF("next");
		}
	}

	public void writeStrSet(final Set<String> sets) throws IOException {
		writeVarInt(sets.size());
		for (String str : sets) {
			writeUTF(str);
		}
	}

	public static final int WRITE_READ_UTF_MAX_LENGTH = 65535;

	public void writeChainNodeSet(Set<ChainNode> chainNodes) throws Exception {
		writeVarInt(chainNodes.size());
		for (ChainNode chainNode : chainNodes) {
			String str = FastJsonUtil.serialize(chainNode);
			// writeVarInt(bytes.length);
			// write(bytes);
			if (str.length() > WRITE_READ_UTF_MAX_LENGTH) {
				for (int i = 1; i < str.length() / WRITE_READ_UTF_MAX_LENGTH
						+ 2; i++) {
					writeUTF(str.substring(WRITE_READ_UTF_MAX_LENGTH * (i - 1),
							WRITE_READ_UTF_MAX_LENGTH * i < str.length()
									? WRITE_READ_UTF_MAX_LENGTH * i
									: str.length()));
				}
			} else {
				writeUTF(str);
			}
		}
	}

}
