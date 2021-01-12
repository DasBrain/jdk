/*
 * Copyright (c) 2001, 2013, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package jdk.internal.reflect;

import jdk.internal.misc.VM;
import sun.reflect.misc.ReflectUtil;

import java.lang.reflect.*;
import jdk.internal.misc.Unsafe;

/** Used only for the first few invocations of a Method; afterward,
    switches to bytecode-based implementation */

class NativeMethodAccessorImpl extends MethodAccessorImpl {
     private static final Unsafe U = Unsafe.getUnsafe();
     private static final long GENERATED_OFFSET
        = U.objectFieldOffset(NativeMethodAccessorImpl.class, "generated");

    private final Method method;
    private DelegatingMethodAccessorImpl parent;
    private int numInvocations;
    private volatile int generated;

    NativeMethodAccessorImpl(Method method) {
        this.method = method;
    }

    public Object invoke(Object obj, Object[]args)
        throws IllegalArgumentException, InvocationTargetException
    {
        // We can't inflate methods belonging to vm-anonymous classes because
        // that kind of class can't be referred to by name, hence can't be
        // found from the generated bytecode.
        if (++numInvocations > ReflectionFactory.inflationThreshold()
                && VM.isModuleSystemInited()
                && !method.getDeclaringClass().isHidden()
                && !ReflectUtil.isVMAnonymousClass(method.getDeclaringClass())
                && generated == 0
                && U.compareAndSetInt(this, GENERATED_OFFSET, 0, 1)) {
            try {
                MethodAccessorImpl acc = (MethodAccessorImpl)ReflectionFactory.createMethodAccessor(method);
                parent.setDelegate(acc);
            } catch (Throwable t) {
                // Throwable happens in generateMethod, restore generated to 0
                generated = 0;
                throw t;
            }
        }

        return invoke0(method, obj, args);
    }

    void setParent(DelegatingMethodAccessorImpl parent) {
        this.parent = parent;
    }

    private static native Object invoke0(Method m, Object obj, Object[] args);
}
