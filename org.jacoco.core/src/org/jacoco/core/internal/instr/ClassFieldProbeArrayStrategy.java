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
package org.jacoco.core.internal.instr;

import org.jacoco.core.runtime.IExecutionDataAccessorGenerator;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * The strategy for regular classes adds a static field to hold the probe array
 * and a static initialization method requesting the probe array from the
 * runtime.
 */
class ClassFieldProbeArrayStrategy implements IProbeArrayStrategy {

	/**
	 * Frame stack with a single boolean array.
	 */
	private static final Object[] FRAME_STACK_ARRZ = new Object[] {
			InstrSupport.DATAFIELD_DESC };

	/**
	 * Empty frame locals.
	 */
	private static final Object[] FRAME_LOCALS_EMPTY = new Object[0];

	private final String className;
	private final long classId;
	private final boolean withFrames;
	private final IExecutionDataAccessorGenerator accessorGenerator;

	ClassFieldProbeArrayStrategy(final String className, final long classId,
			final boolean withFrames,
			final IExecutionDataAccessorGenerator accessorGenerator) {
		this.className = className;
		this.classId = classId;
		this.withFrames = withFrames;
		this.accessorGenerator = accessorGenerator;
	}

	public String getClassName() {
		return className;
	}

	public void callChainHandleMethod(final MethodVisitor mv,
			final String uri) {
		mv.visitLdcInsn(uri);
		mv.visitMethodInsn(Opcodes.INVOKESTATIC,
				InstrSupport.CHAIN_HANDLE_CLASSNAME,
				InstrSupport.ADD_CHAIN_NODE_METHOD_NAME,
				InstrSupport.ADD_CHAIN_NODE_METHOD_DESC, false);
	}

	public void SetCalledNodeMethod(final MethodVisitor mv, final String uri) {
		mv.visitLdcInsn(uri);
		mv.visitMethodInsn(Opcodes.INVOKESTATIC,
				InstrSupport.CHAIN_HANDLE_CLASSNAME,
				InstrSupport.SET_CALLED_NODE_METHOD_NAME,
				InstrSupport.SET_CALLED_NODE_METHOD_DESC, false);
	}

	public int storeInstance(final MethodVisitor mv, final boolean clinit,
			final int variable) {
		// if (!clinit) { // init方法不插入这个
		// // 先获取jacocoCalledFlagSets
		// mv.visitMethodInsn(Opcodes.INVOKESTATIC, className,
		// InstrSupport.INITSETMETHOD_NAME,
		// InstrSupport.INITSETMETHOD_DESC, false);
		// mv.visitVarInsn(Opcodes.ASTORE, variable - 1);
		// }
		mv.visitMethodInsn(Opcodes.INVOKESTATIC, className,
				InstrSupport.INITMETHOD_NAME, InstrSupport.INITMETHOD_DESC,
				false);
		mv.visitVarInsn(Opcodes.ASTORE, variable);
		return 1;
		// return 2; // 因为现在存储了2个变量，所以改为返回2
		// return clinit ? 1 : 2;
	}

	public void addMembers(final ClassVisitor cv, final int probeCount) {
		createDataField(cv);
		createInitMethod(cv, probeCount);

		// 添加另一个静态变量 $jacocoSet用来存储 调用者的uri
		createSetDataField(cv);
		createSetInitMethod(cv, probeCount);
	}

	private void createSetDataField(final ClassVisitor cv) {
		cv.visitField(InstrSupport.DATAFIELD_ACC,
				InstrSupport.SET_DATA_FIELD_NAME,
				InstrSupport.SET_DATA_FIELD_DESC, null, null);
	}

	private void createSetInitMethod(final ClassVisitor cv,
			final int probeCount) {
		MethodVisitor mv = cv.visitMethod(InstrSupport.INITMETHOD_ACC,
				InstrSupport.INITSETMETHOD_NAME,
				InstrSupport.INITSETMETHOD_DESC, null, null);

		mv.visitCode();

		// [$jacocoSet_ref]
		mv.visitFieldInsn(Opcodes.GETSTATIC, className,
				InstrSupport.SET_DATA_FIELD_NAME, "[Ljava/util/HashSet;");

		// [$jacocoSet_ref, $jacocoSer_ref]
		mv.visitInsn(Opcodes.DUP);

		// [$jacocoSet_ref]
		final Label alreadyInitialized = new Label();
		mv.visitJumpInsn(Opcodes.IFNONNULL, alreadyInitialized);

		mv.visitInsn(Opcodes.POP);// []

		// [runtimeData_ref]
		mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/UnknownError",
				"$jacocoAccess", "Ljava/lang/Object;");

		// 强转类型 [RuntimeData_obj]
		mv.visitTypeInsn(Opcodes.CHECKCAST,
				"org/jacoco/core/runtime/RuntimeData");

		// [runtimeData_ref, 3]
		mv.visitInsn(Opcodes.ICONST_3);

		// [runtimeData_ref, array_ref]
		mv.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/Object");

		// set classId
		mv.visitInsn(Opcodes.DUP);// [runtimeData_ref, array_ref, array_ref]
		mv.visitInsn(Opcodes.ICONST_0); // [runtimeData_ref, array_ref,
										// array_ref, 0]
		mv.visitLdcInsn(Long.valueOf(classId)); // [runtimeData_ref, array_ref,
												// array_ref, 0, long_top,
												// long_end]
		mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Long", "valueOf",
				"(J)Ljava/lang/Long;", false);// [runtimeData_ref, array_ref,
												// long]
		mv.visitInsn(Opcodes.AASTORE); // [runtimeData_ref, array_ref]

		// set className
		mv.visitInsn(Opcodes.DUP);// [runtimeData_ref, array_ref, array_ref]
		mv.visitInsn(Opcodes.ICONST_1);
		mv.visitLdcInsn(className);
		mv.visitInsn(Opcodes.AASTORE);

		// set probeCount
		mv.visitInsn(Opcodes.DUP);
		mv.visitInsn(Opcodes.ICONST_2);
		InstrSupport.push(mv, probeCount);
		mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Integer", "valueOf",
				"(I)Ljava/lang/Integer;", false);
		mv.visitInsn(Opcodes.AASTORE); // [runtimeData_ref, array_ref]

		// [array_ref, runtimeData_ref, array_ref]
		mv.visitInsn(Opcodes.DUP_X1);

		// [array_ref, int] int是equals返回的结果
		mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL,
				"org/jacoco/core/runtime/RuntimeData", "generateCalledSetArray",
				"(Ljava/lang/Object;)Z", false);
		mv.visitInsn(Opcodes.POP);// [array_ref]

		// set array_ref = Set[]
		mv.visitInsn(Opcodes.ICONST_0); // [array_ref, 0]
		mv.visitInsn(Opcodes.AALOAD); // [obj_array_ref]

		// 强转类型 [set_array_ref]
		mv.visitTypeInsn(Opcodes.CHECKCAST, "[Ljava/util/HashSet;");

		// [set_array_ref, set_array_ref]
		mv.visitInsn(Opcodes.DUP);

		// [set_array_ref]
		mv.visitFieldInsn(Opcodes.PUTSTATIC, className,
				InstrSupport.SET_DATA_FIELD_NAME,
				InstrSupport.SET_DATA_FIELD_DESC);

		// Return the class' probe array:
		if (withFrames) {
			mv.visitFrame(Opcodes.F_NEW, 0, FRAME_LOCALS_EMPTY, 1,
					new Object[] { InstrSupport.SET_DATA_FIELD_DESC });
		}
		mv.visitLabel(alreadyInitialized);
		// []
		mv.visitInsn(Opcodes.ARETURN);

		mv.visitMaxs(Math.max(6, 2), 0); // Maximum local stack size is 2
		mv.visitEnd();

	}

	private void createDataField(final ClassVisitor cv) {
		cv.visitField(InstrSupport.DATAFIELD_ACC, InstrSupport.DATAFIELD_NAME,
				InstrSupport.DATAFIELD_DESC, null, null);
	}

	private void createInitMethod(final ClassVisitor cv, final int probeCount) {
		final MethodVisitor mv = cv.visitMethod(InstrSupport.INITMETHOD_ACC,
				InstrSupport.INITMETHOD_NAME, InstrSupport.INITMETHOD_DESC,
				null, null);
		mv.visitCode();

		// Load the value of the static data field:
		mv.visitFieldInsn(Opcodes.GETSTATIC, className,
				InstrSupport.DATAFIELD_NAME, InstrSupport.DATAFIELD_DESC);
		mv.visitInsn(Opcodes.DUP);

		// Stack[1]: [Z
		// Stack[0]: [Z

		// Skip initialization when we already have a data array:
		final Label alreadyInitialized = new Label();
		mv.visitJumpInsn(Opcodes.IFNONNULL, alreadyInitialized);

		// Stack[0]: [Z

		mv.visitInsn(Opcodes.POP);
		final int size = genInitializeDataField(mv, probeCount);

		// Stack[0]: [Z

		// Return the class' probe array:
		if (withFrames) {
			mv.visitFrame(Opcodes.F_NEW, 0, FRAME_LOCALS_EMPTY, 1,
					FRAME_STACK_ARRZ);
		}
		mv.visitLabel(alreadyInitialized);
		mv.visitInsn(Opcodes.ARETURN);

		mv.visitMaxs(Math.max(size, 2), 0); // Maximum local stack size is 2
		mv.visitEnd();
	}

	/**
	 * Generates the byte code to initialize the static coverage data field
	 * within this class.
	 *
	 * The code will push the [Z data array on the operand stack.
	 *
	 * @param mv
	 *            generator to emit code to
	 */
	private int genInitializeDataField(final MethodVisitor mv,
			final int probeCount) {
		final int size = accessorGenerator.generateDataAccessor(classId,
				className, probeCount, mv);

		// Stack[0]: [Z

		mv.visitInsn(Opcodes.DUP);

		// Stack[1]: [Z
		// Stack[0]: [Z

		mv.visitFieldInsn(Opcodes.PUTSTATIC, className,
				InstrSupport.DATAFIELD_NAME, InstrSupport.DATAFIELD_DESC);

		// Stack[0]: [Z

		return Math.max(size, 2); // Maximum local stack size is 2
	}

}
