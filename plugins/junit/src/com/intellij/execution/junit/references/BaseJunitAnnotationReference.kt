// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.execution.junit.references

import com.intellij.codeInsight.lookup.AutoCompletionPolicy
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.codeInspection.reference.PsiMemberReference
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.*
import com.intellij.psi.search.searches.ClassInheritorsSearch
import com.intellij.psi.util.ClassUtil
import com.intellij.psi.util.InheritanceUtil
import com.intellij.psi.util.PsiUtil
import org.jetbrains.uast.*

abstract class BaseJunitAnnotationReference(
  element: PsiLanguageInjectionHost
) : PsiReferenceBase<PsiLanguageInjectionHost>(element, false), PsiMemberReference {
  override fun bindToElement(element: PsiElement): PsiElement {
    return when (element) {
      is PsiMethod -> handleElementRename(element.name)
      else -> super.bindToElement(element)
    }
  }

  override fun handleElementRename(newElementName: String): PsiElement {
    val methodName = value
    val className = StringUtil.getPackageName(methodName, '#')
    val selfClassReference = className.isEmpty() || ClassUtil.findPsiClass(
      element.manager, className, null, false, element.resolveScope
    ) == null
    return super.handleElementRename(if (selfClassReference) newElementName else "$className#$newElementName")
  }

  override fun isReferenceTo(element: PsiElement): Boolean {
    val myLiteral = getElement().toUElement(UExpression::class.java) ?: return false
    val uMethod = element.toUElement(UMethod::class.java) ?: return false
    val method = uMethod.javaPsi
    val methodName = myLiteral.evaluate() as? String ?: return false
    val shortName = StringUtil.getShortName(methodName, '#')
    if (shortName != method.name) return false
    val methodClass = method.containingClass ?: return false
    if (method.hasModifierProperty(PsiModifier.STATIC)) {
      val className = StringUtil.getPackageName(methodName, '#')
      if (className.isNotEmpty()) {
        return className == ClassUtil.getJVMClassName(methodClass)
      }
    }
    val literalClazz = myLiteral.getParentOfType(UClass::class.java) ?: return false
    val psiClazz = literalClazz.javaPsi
    return InheritanceUtil.isInheritorOrSelf(psiClazz, methodClass, true) ||
           InheritanceUtil.isInheritorOrSelf(methodClass, psiClazz, true)
  }

  override fun resolve(): PsiElement? {
    val literalExpressionToFind = element.toUElement(UExpression::class.java) ?: return null

    val containingMethodOrAnnotation = literalExpressionToFind.getParentOfType(UMethod::class.java)
                                       ?: literalExpressionToFind.getParentOfType(UClass::class.java)
                                       ?: return null

    return findReferencedMethod(literalExpressionToFind, containingMethodOrAnnotation)
  }

  private fun findReferencedMethod(expressionToFind: UExpression, containingElement: UElement): PsiElement? {
    val containingClass = containingElement.getParentOfType(UClass::class.java) ?: return null
    var psiClazz = containingClass.javaPsi

    var methodNameToFind = expressionToFind.evaluate() as? String ?: return null

    val className = StringUtil.getPackageName(methodNameToFind, '#')
    if (className.isNotEmpty()) {
      val aClass = ClassUtil.findPsiClass(psiClazz.manager, className, null, false, psiClazz.resolveScope)
      if (aClass != null) {
        psiClazz = aClass
        methodNameToFind = StringUtil.getShortName(methodNameToFind, '#')
      }
    }

    var clazzMethods = findMethods(psiClazz, methodNameToFind)
    if (clazzMethods.isEmpty()) clazzMethods = psiClazz.innerClasses.flatMap { findMethods(it, methodNameToFind) }
    if (clazzMethods.isEmpty() && (psiClazz.isInterface || PsiUtil.isAbstractClass(psiClazz))) {
      return ClassInheritorsSearch.search(psiClazz, psiClazz.resolveScope, false)
        .mapNotNull { findMethods(it, methodNameToFind, false) }
        .mapNotNull { filteredMethod(it, containingClass, containingElement) }
        .map { PsiElementResolveResult(it) }
        .firstOrNull()?.element
    }

    return filteredMethod(clazzMethods, containingClass, containingElement)
  }

  private fun findMethods(psiClass: PsiClass, methodName: String, checkBase: Boolean = true): List<PsiMethod> {
    return psiClass.findMethodsByName(methodName, checkBase).filterNotNull()
  }

  /**
   * Filters the given list of methods to return a method that has no static problems.
   * If no such method exists, returns the first method in the list.
   *
   * @param methods List of methods to filter.
   * @param containingClass The UClass object representing the class containing the methods.
   * @param containingElement The UElement object representing the method or annotation that contains the annotation, can be null if the annotation is class-level.
   * @return A PsiMethod object that has no static problems or the first method in the list if none exists.
   */
  private fun filteredMethod(methods: List<PsiMethod>, containingClass: UClass, containingElement: UElement?): PsiMethod? {
    return methods.firstOrNull { hasNoStaticProblem(it, containingClass, containingElement) }
           ?: methods.firstOrNull()
  }

  override fun getVariants(): Array<Any> {
    val myLiteral = element.toUElement(UExpression::class.java) ?: return emptyArray()
    val topLevelClass = myLiteral.getParentOfType(UClass::class.java) ?: return emptyArray()
    val current = myLiteral.getParentOfType(UMethod::class.java)
    val psiTopLevelClass = topLevelClass.javaPsi
    val methods = psiTopLevelClass.allMethods
    val list = mutableListOf<Any>()
    for (method in methods) {
      val aClass = method.containingClass ?: continue
      if (CommonClassNames.JAVA_LANG_OBJECT == aClass.qualifiedName) continue
      if (current != null && method.name == current.name) continue
      if (current != null && !hasNoStaticProblem(method, topLevelClass, current)) continue
      val builder = LookupElementBuilder.create(method)
      list.add(builder.withAutoCompletionPolicy(AutoCompletionPolicy.SETTINGS_DEPENDENT))
    }
    return list.toTypedArray()
  }

  /**
   * @param method method referenced from within JUnit annotation
   * @param literalClazz the class where the annotation is located
   * @param literalElement the JUnit annotated method or annotation. Is null in case the annotation is class-level
   * @return true in case static check is successful
   */
  protected abstract fun hasNoStaticProblem(method: PsiMethod, literalClazz: UClass, literalElement: UElement?): Boolean
}
