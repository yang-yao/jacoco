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
package org.jacoco.core.runtime;

import org.jacoco.core.data.ChainNode;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

public class ChainNodeHandle {

	public static Set<ChainNode> chainsSet = new HashSet<>();
	private static final ReentrantLock addChainLock = new ReentrantLock();
	private static final ReentrantLock setCalledFlagsLock = new ReentrantLock();

	private static final ThreadLocal<ChainNode> headNode = new ThreadLocal<>();
	private static final ThreadLocal<ChainNode> calledNode = new ThreadLocal<>();
	private static final ThreadLocal<ChainNode> tailNode = new ThreadLocal<>();

	public static void addChainNode(String uri) {
		// System.out.println(">>>> add chain node, uri: " + uri + " <<<<");
		ChainNode currentNode = new ChainNode();
		currentNode.setUri(uri);
		// set headNode
		if (Objects.isNull(headNode.get())) {
			// System.out.println(">>>> set headNode: " + currentNode.getUri() +
			// " <<<<");
			headNode.set(currentNode);
		}
		// set preNode
		if (!Objects.isNull(tailNode.get())) {
			currentNode.setPreNode(tailNode.get());
		}
		if (!Objects.isNull(calledNode.get())) {
			// 为了避免相互引用太复杂，调用节点全部直接生成
			// currentNode.setCalledNode(calledNode.get());
			ChainNode call = new ChainNode();
			call.setUri(calledNode.get().getUri());
			currentNode.setCalledNode(call);
		}
		calledNode.set(currentNode);
		tailNode.set(currentNode);
	}

	public static void setCalledNode(String uri) {
		// System.out.println(">>>> set called node, uri: " + uri + " <<<<");
		/**
		 * tailNode.get().getPreNode() != null :我理解的是，如果调用链只有一个函数，那么过滤掉
		 */
		if (!Objects.isNull(headNode.get())
				&& uri.equals(headNode.get().getUri())
				&& tailNode.get().getPreNode() != null) {
			// System.out.println(">>>> current head node uri: " + uri + "
			// <<<<");
			// System.out.println(">>>> current thread name: "
			// + Thread.currentThread().getName() + " <<<<");
			addChainLock.lock();
			try {
				chainsSet.add(tailNode.get());
				System.out.println("---------->>>> method chain : "
						+ tailNode.get().toString() + " <<<<--------------");
			} finally {
				/**
				 * headNode节点set为null，在spirng接口里面，当你方法执行完后，
				 * spring会继续操作你的result对象，这时候会出现很多单个方法的调用链
				 * 每个线程应该只有一个headNode，并且不会改变
				 *
				 * 上面解释的不正确，在tomcat中，有固定的线程池，每次都是分配这些线程，
				 * 当headNode不设置为null后，那么这个线程只有第一次设置的headNode请求，才会被记录
				 */
				addChainLock.unlock();
				headNode.set(null);
				tailNode.set(null);
				calledNode.set(null);
			}
		} else {
			if (Objects.isNull(calledNode.get())
					|| Objects.isNull(calledNode.get().getCalledNode()))
				return;
			calledNode.set(calledNode.get().getCalledNode());
		}

	}

	public static void setCalledFlags(HashSet set) {
		setCalledFlagsLock.lock();
		try {
			if (Objects.isNull(tailNode.get())
					|| Objects.isNull(tailNode.get().getCalledNode())
					|| Objects.isNull(tailNode.get().getCalledNode().getUri())
					|| tailNode.get().getCalledNode().getUri().equals("")) {
				return;
			}
			set.add(tailNode.get().getCalledNode().getUri());
		} finally {
			setCalledFlagsLock.unlock();
		}
	}
}
