// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.execution.junit

import com.intellij.codeInsight.MetaAnnotationUtil
import com.intellij.codeInspection.flattenedAttributeValues
import com.intellij.execution.junit.references.MethodSourceReference
import com.intellij.navigation.GotoRelatedItem
import com.intellij.navigation.GotoRelatedProvider
import com.intellij.psi.*
import com.siyeh.ig.junit.JUnitCommonClassNames
import org.jetbrains.uast.*
import kotlin.streams.asSequence

class JUnitParameterizedSourceGotoRelatedProvider : GotoRelatedProvider() {
  override fun getItems(psiElement: PsiElement): List<GotoRelatedItem> {
    val uElement = psiElement.parent.toUElementOfType<UElement>() ?: return emptyList()
    return when (val javaMethodOrAnnotation = uElement.javaPsi) {
      is PsiMember -> findGoToValues(javaMethodOrAnnotation)
      else -> return emptyList()
    }
  }

  private fun findGoToValues(currentElement: PsiMember): List<GotoRelatedItem> {
    return MetaAnnotationUtil.findMetaAnnotations(
      currentElement,
      setOf(JUnitCommonClassNames.ORG_JUNIT_JUPITER_PARAMS_PROVIDER_METHOD_SOURCE)
    )
      .asSequence()
      .flatMap { methodSourceAnnotation -> findPointingMethods(currentElement, methodSourceAnnotation) }
      .map { method -> GotoRelatedItem(method) }
      .toList()
  }

  private fun findPointingMethods(currentElement: PsiMember, annotation: PsiAnnotation): List<PsiMethod> {
    val annotationMemberValue = annotation.flattenedAttributeValues("value")
    val containingClass = currentElement.containingClass ?: return emptyList()

    return if (annotationMemberValue.isEmpty()) {
      if (annotation.findAttributeValue(PsiAnnotation.DEFAULT_REFERENCED_METHOD_NAME) == null) return emptyList()
      val sourceMethod = containingClass.findMethodsByName(currentElement.name, true).singleOrNull {
        it.parameters.isEmpty()
      } ?: return emptyList()
      listOf(sourceMethod)
    }
    else {
      annotationMemberValue.flatMap { annotationValue ->
        annotationValue.references.mapNotNull { ref ->
          if (ref !is MethodSourceReference) return@mapNotNull null
          ref.resolve() as? PsiMethod
        }
      }
    }
  }
}