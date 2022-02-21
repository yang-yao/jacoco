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
import org.objectweb.asm.Type;

/**
 * @author wl
 */
public class MethodUriAdapter {

	private static final String SPECIAL_TYPE = "int";
	private static final String OBJECT_TYPE = "Integer";
	private static final String JAVA_SPECIAL_CHAR = ".";
	private static final String ASM_SPECIAL_CHAR = "/";

	private static final String JAVA_SPECIAL_SPLIT_CHAR = "\\.";

	public static boolean getMethodUri(String diffMethodName,
			String methodUri) {
		diffMethodName = specialTypeConvert(diffMethodName);
		// 获取类名，判断是不是同一个类
		String[] arr1 = diffMethodName.split(JAVA_SPECIAL_SPLIT_CHAR);
		String[] arr2 = methodUri.split(JAVA_SPECIAL_SPLIT_CHAR);
		if (arr1.length < 2 || arr2.length < 2) {
			return false;
		}
		if (!arr1[0].equals(arr2[0])) {
			return false;
		}
		// 获取方法名，判断是不是同一个方法
		arr1 = arr1[1].split("\\(");
		arr2 = arr2[1].split("\\(");
		if (arr1.length < 2 || arr2.length < 2) {
			return false;
		}
		if (!arr1[0].equals(arr2[0])) {
			return false;
		}
		// 判断参数是否出现为空的情况
		arr1 = arr1[1].split("\\)");
		arr2 = arr2[1].split("\\)");
		if (arr1.length == 0 && StringUtils.isBlank(arr2[0])) {
			return true;
		} else if (arr1.length != 0 && StringUtils.isBlank(arr2[0])) {
			return false;
		} else if (arr1.length == 0 && StringUtils.isNotBlank(arr2[0])) {
			return false;
		}
		if (arr1.length != arr2.length
				&& (arr1.length == 0 || arr2.length == 0)) {
			return false;
		}
		// 获取参数类型，判断形参数量是否一致，并且类型相同
		arr1 = arr1[0].split(";");
		// asm生成参数都是以分号结尾
		arr2 = arr2[0].split(";");
		if (arr1.length != arr2.length) {
			return false;
		}
		for (int i = 0; i < arr1.length; i++) {
			// 这里大写判断是因为java基础类型首字母是小写
			if (!arr2[i].trim().toUpperCase()
					.endsWith(arr1[i].trim().toUpperCase())) {
				return false;
			}
		}
		return true;
	}

	private static String specialTypeConvert(String methodName) {
		methodName = methodName.replace(SPECIAL_TYPE + ",", OBJECT_TYPE + ",");
		String[] arr = methodName.split("\\(");
		arr[1] = arr[1].replace(JAVA_SPECIAL_CHAR, ASM_SPECIAL_CHAR);
		return arr[0] + "(" + arr[1];
	}

	/**
	 * 参数匹配，方法实现来自
	 * https://gitee.com/Dray/jacoco/blob/master/org.jacoco.core/src/org/jacoco/core/internal/diff/CodeDiffUtil.java
	 *
	 * @param params
	 * @param desc
	 * @return
	 */
	public static boolean checkParamsIn(String params, String desc) {
		// 解析ASM获取的参数
		Type[] argumentTypes = Type.getArgumentTypes(desc);
		// 说明是无参数的方法，匹配成功
		if (params.length() == 0 && argumentTypes.length == 0) {
			return Boolean.TRUE;
		}
		// 切割符号来自：com/test/diff/services/internal/JavaFileCodeComparator.MethodVisitor#visit方法中定义
		String[] diffParams = params.split(";");
		// 只有参数数量完全相等才做下一次比较，Type格式：I C Ljava/lang/String;
		if (diffParams.length > 0
				&& argumentTypes.length == diffParams.length) {
			for (int i = 0; i < argumentTypes.length; i++) {
				// 去掉包名只保留最后一位匹配,getClassName格式： int java/lang/String
				String[] args = argumentTypes[i].getClassName().split("\\.");
				String arg = args[args.length - 1];
				// 如果参数是内部类类型，再截取下
				if (arg.contains("$")) {
					arg = arg.split("\\$")[arg.split("\\$").length - 1];
				}
				if (!diffParams[i].contains(arg)) {
					return Boolean.FALSE;
				}
			}
			// 只有个数和类型全匹配到才算匹配
			return Boolean.TRUE;
		}
		return Boolean.FALSE;
	}
}
