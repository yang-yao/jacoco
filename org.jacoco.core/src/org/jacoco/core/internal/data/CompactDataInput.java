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

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

/**
 * Additional data input methods for compact storage of data structures.
 *
 * @see CompactDataOutput
 */
public class CompactDataInput extends DataInputStream {

	/**
	 * Creates a new {@link CompactDataInput} that uses the specified underlying
	 * input stream.
	 *
	 * @param in
	 *            underlying input stream
	 */
	public CompactDataInput(final InputStream in) {
		super(in);
	}

	/**
	 * Reads a variable length representation of an integer value.
	 *
	 * @return read value
	 * @throws IOException
	 *             if thrown by the underlying stream
	 */
	public int readVarInt() throws IOException {
		final int value = 0xFF & readByte();
		if ((value & 0x80) == 0) {
			return value;
		}
		return (value & 0x7F) | (readVarInt() << 7);
	}

	/**
	 * Reads a boolean array.
	 *
	 * @return boolean array
	 * @throws IOException
	 *             if thrown by the underlying stream
	 */
	public boolean[] readBooleanArray() throws IOException {
		final boolean[] value = new boolean[readVarInt()];
		int buffer = 0;
		for (int i = 0; i < value.length; i++) {
			if ((i % 8) == 0) {
				buffer = readByte();
			}
			value[i] = (buffer & 0x01) != 0;
			buffer >>>= 1;
		}
		return value;
	}

	public HashSet[] readSetArray() throws IOException {
		final HashSet<String>[] sets = new HashSet[readVarInt()];
		for (int i = 0; i < sets.length; i++) {
			HashSet<String> set = new HashSet<>();
			sets[i] = set;
			String value = readUTF();
			while (!"next".equals(value)) {
				set.add(value);
				value = readUTF();
			}
		}
		return sets;
	}

	public Set<String> readStrSet() throws IOException {
		int size = readVarInt();
		Set<String> calledChainStrSets = new HashSet<>();
		while (size > 0) {
			size--;
			calledChainStrSets.add(readUTF());
		}
		return calledChainStrSets;
	}

	public static final int WRITE_READ_UTF_MAX_LENGTH = 65535;

	public Set<ChainNode> readChainNodeSet() throws IOException {
		int size = readVarInt();
		Set<ChainNode> chainNodes = new HashSet<>();
		while (size-- > 0) {
			String str = readUTF();
			StringBuilder sb = new StringBuilder();
			while (str.length() >= WRITE_READ_UTF_MAX_LENGTH) {
				sb.append(str);
				str = readUTF();
			}
			sb.append(str);
			ChainNode chainNode = FastJsonUtil.deserialize(sb.toString(),
					ChainNode.class);
			chainNodes.add(chainNode);
		}
		return chainNodes;
	}

}
