package com.siyeh.ig.classlayout;

import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiSuperMethodUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.ReadonlyStatusHandler;
import com.intellij.openapi.vfs.VirtualFile;
import com.siyeh.ig.*;

public class AbstractMethodOverridesAbstractMethodInspection extends MethodInspection {
    private final AbstractMethodOverridesAbstractMethodFix fix = new AbstractMethodOverridesAbstractMethodFix();

    public String getDisplayName() {
        return "Abstract method overrides abstract method";
    }

    public String getGroupDisplayName() {
        return GroupNames.CLASSLAYOUT_GROUP_NAME;
    }

    public String buildErrorString(PsiElement location) {
        return "Abstract method '#ref' overrides abstract method #loc";
    }

    protected InspectionGadgetsFix buildFix(PsiElement location) {
        return fix;
    }

    private static class  AbstractMethodOverridesAbstractMethodFix extends InspectionGadgetsFix {
        public String getName() {
            return "Remove redundant abstract method declaration";
        }

        public void applyFix(Project project, ProblemDescriptor descriptor) {
            if (ReadonlyStatusHandler.getInstance(project).ensureFilesWritable(new VirtualFile[]{descriptor.getPsiElement().getContainingFile().getVirtualFile()}).hasReadonlyFiles()) return;
            final PsiElement methodNameIdentifier = descriptor.getPsiElement();
            final PsiElement method = methodNameIdentifier.getParent();
            deleteElement(method);
        }
    }

    public BaseInspectionVisitor createVisitor(InspectionManager inspectionManager, boolean onTheFly) {
        return new AbstractMethodOverridesAbstractMethodVisitor(this, inspectionManager, onTheFly);
    }

    private static class AbstractMethodOverridesAbstractMethodVisitor extends BaseInspectionVisitor {
        private AbstractMethodOverridesAbstractMethodVisitor(BaseInspection inspection,
                                                             InspectionManager inspectionManager, boolean isOnTheFly) {
            super(inspection, inspectionManager, isOnTheFly);
        }

        public void visitMethod(PsiMethod method) {
            //no call to super, so we don't drill into anonymous classes
            if (method.isConstructor()) {
                return;
            }
            final PsiClass containingClass = method.getContainingClass();
            if (!method.hasModifierProperty(PsiModifier.ABSTRACT) &&
                    !containingClass.isInterface()) {
                return;
            }
            final PsiMethod[] superMethods = PsiSuperMethodUtil.findSuperMethods(method);
            for (int i = 0; i < superMethods.length; i++) {
                final PsiMethod superMethod = superMethods[i];
                final PsiClass superClass = superMethod.getContainingClass();
                if (superClass.isInterface() ||
                        superMethod.hasModifierProperty(PsiModifier.ABSTRACT)) {
                    registerMethodError(method);
                    return;
                }
            }
        }
    }
}
