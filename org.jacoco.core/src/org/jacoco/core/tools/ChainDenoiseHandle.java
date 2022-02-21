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
package org.jacoco.core.tools;

import com.test.diff.common.domain.MethodInfo;
import org.apache.commons.lang3.StringUtils;
import org.jacoco.core.data.ChainNode;
import org.jacoco.core.data.ExecutionData;
import org.jacoco.core.data.ExecutionDataStore;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * 链路降噪处理：处理方法修改和删除对链路数据带来的影响
 *
 * @author wl
 */
public class ChainDenoiseHandle {

	public static Set<ChainNode> execNoise(ExecutionDataStore store,
			Set<ChainNode> chainSets, List<MethodInfo> methodInfos) {
		// 首先找出所有修改或删除方法影响的函数调用链
		Set<ChainNode> affectedChains = new HashSet<>();
		Set<ChainNode> unAffectedChains = new HashSet<>();
		for (ChainNode node : chainSets) {
			boolean flag = true;
			for (MethodInfo methodInfo : methodInfos) {
				if (StringUtils.isBlank(methodInfo.getMethodUri())) {
					continue;
				}
				if (node.toString().contains(methodInfo.getMethodUri())) {
					flag = false;
					affectedChains.add(node);
				}
			}
			if (flag) {
				unAffectedChains.add(node);
			}
		}
		// 去除受影响链路的探针数据
		for (ChainNode node : affectedChains) {
			// 链路从尾逐级查询是否需要降噪影响
			while (node.getPreNode() != null) {
				// 查看当前节点在未受影响链路中，是否被同一个父级节点调用；如果存在，当前节点探针数据不操作
				for (ChainNode goodNode : unAffectedChains) {
					if (!findCalledNode(goodNode, node)) {
						ChainNode calledNode = node.getCalledNode();
						String currentNodeClassName = node.getUri()
								.split("\\.")[0];
						boolean[] currentProbes = findProbes(
								currentNodeClassName, store);
						Set<String>[] currentSets = findSets(
								currentNodeClassName, store);
						if (currentProbes.length < 0 || currentSets == null
								|| currentProbes.length != currentSets.length) {
							continue;
						}
						for (int i = 0; i < currentSets.length; i++) {
							// 如果当前探针代码块只被一个方法调用过，并且这个方法为当前链路的被调用者节点，就清除探针标记
							if (currentSets[i].size() == 1
									&& currentSets[i].iterator().next()
											.equals(calledNode.getUri())) {
								currentProbes[i] = false;
								currentSets[i].remove(calledNode.getUri());
								continue;
							}
							// 如果不唯一，但是存在调用过，那就不改变探针状态，但是去掉调用过的记录
							Iterator iterator = currentSets[i].iterator();
							while (iterator.hasNext()) {
								String uri = (String) iterator.next();
								if (uri.equals(calledNode.getUri())) {
									currentSets[i].remove(uri);
									break;
								}
							}
						}
					}
				}
				node = node.getPreNode();
			}

		}
		return unAffectedChains;
	}

	private static boolean[] findProbes(String className,
			ExecutionDataStore store) {
		for (ExecutionData data : store.getContents()) {
			if (className.equals(data.getName())) {
				return data.getProbes();
			}
		}
		return new boolean[0];
	}

	private static Set<String>[] findSets(String className,
			ExecutionDataStore store) {
		for (ExecutionData data : store.getContents()) {
			if (className.equals(data.getName())) {
				return data.getCalledFlags();
			}
		}
		return null;
	}

	private static boolean findCalledNode(ChainNode funcChains,
			ChainNode targetNode) {
		while (funcChains.getPreNode() != null) {
			if (targetNode.getUri().equals(funcChains.getUri())
					&& targetNode.getCalledNode().getUri()
							.equals(funcChains.getCalledNode().getUri())) {
				return true;
			}
			funcChains = funcChains.getPreNode();
		}
		return false;
	}
}
