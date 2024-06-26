/*
 * Copyright 2000-2011 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.psi;

import com.intellij.psi.util.PsiModificationTracker;
import org.jetbrains.annotations.ApiStatus;

/**
 * Represents psi element, which can be modified without caches reset.
 * @deprecated because {@link PsiModificationTracker}.getOutOfCodeBlockModificationCount() was removed, there is no more code that calls the method
 */
@ApiStatus.ScheduledForRemoval
@Deprecated
public interface PsiModifiableCodeBlock {
  /**
   * @param place where change was detected
   * @return false if specific caches could be saved after the change
   */
  boolean shouldChangeModificationCount(PsiElement place);
}
