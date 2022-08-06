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

package org.jacoco.core.data;

import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ChainNode implements Serializable {

	private static final long serialVersionUID = 1L;

	private String uri;
	/**
	 * 链路上一级节点
	 */
	private ChainNode preNode;
	/**
	 * 调用者节点
	 */
	private ChainNode calledNode;

	public String getUri() {
		return uri;
	}

	public void setUri(String uri) {
		this.uri = uri;
	}

	public ChainNode getPreNode() {
		return preNode;
	}

	public void setPreNode(ChainNode preNode) {
		this.preNode = preNode;
	}

	public ChainNode getCalledNode() {
		return calledNode;
	}

	public void setCalledNode(ChainNode calledNode) {
		this.calledNode = calledNode;
	}

	@Override
	public boolean equals(Object args) {
		if (args == null)
			return false;
		if (!(args instanceof ChainNode))
			return false;
		ChainNode other = (ChainNode) args;
		return this.preNode == null
				? StringUtils.equalsIgnoreCase(this.uri, other.uri)
				: StringUtils.equalsIgnoreCase(this.uri, other.uri)
						&& this.preNode.equals(other.getPreNode());
	}

	@Override
	public int hashCode() {
		int num = this.uri.hashCode();
		ChainNode preNode = this.preNode;
		while (!Objects.isNull(preNode)) {
			num += preNode.uri.hashCode();
			preNode = preNode.preNode;
		}
		return num;
	}

	@Override
	public String toString() {
		List<String> list = new ArrayList<>();
		ChainNode node = this;
		while (true) {
			list.add(node.uri);
			node = node.preNode;
			if (node == null) {
				break;
			}
		}
		StringBuilder sb = new StringBuilder();
		sb.append("{ ");
		for (int i = list.size() - 1; i >= 0; i--) {
			sb.append("[");
			sb.append(list.get(i));
			sb.append("]");
			if (i > 0) {
				sb.append(" --->");
			}
		}
		sb.append(" }");
		return sb.toString();
	}
}
