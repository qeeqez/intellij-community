// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.execution.junit.references

import com.intellij.psi.*
import com.siyeh.ig.psiutils.TestUtils
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UElement

class MethodSourceReference(element: PsiLanguageInjectionHost) : BaseJunitAnnotationReference(element) {
  override fun hasNoStaticProblem(method: PsiMethod, literalClazz: UClass, literalElement: UElement?): Boolean {
    val isStatic = method.hasModifierProperty(PsiModifier.STATIC)
    val psiClass = method.containingClass ?: return false
    return method.parameterList.isEmpty && TestUtils.testInstancePerClass(psiClass) != isStatic
  }
}
