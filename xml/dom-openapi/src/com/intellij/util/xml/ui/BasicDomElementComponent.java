// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package com.intellij.util.xml.ui;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.xml.DomElement;
import com.intellij.util.xml.DomFileElement;
import com.intellij.util.xml.DomUtil;
import com.intellij.util.xml.GenericDomValue;
import com.intellij.util.xml.highlighting.DomElementAnnotationsManager;
import com.intellij.util.xml.reflect.AbstractDomChildrenDescription;
import com.intellij.util.xml.reflect.DomChildrenDescription;
import com.intellij.util.xml.reflect.DomCollectionChildDescription;
import com.intellij.util.xml.reflect.DomFixedChildDescription;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public abstract class BasicDomElementComponent<T extends DomElement> extends AbstractDomElementComponent<T> {
  private static final Logger LOG = Logger.getInstance(BasicDomElementComponent.class);
  private final Map<JComponent, DomUIControl> myBoundComponents = new HashMap<>();

  public BasicDomElementComponent(T domElement) {
    super(domElement);
  }

  protected final void bindProperties() {
    bindProperties(getDomElement());
  }

  protected boolean commitOnEveryChange(GenericDomValue element) {
    return false;
  }

  protected final void bindProperties(final DomElement domElement) {
    if (domElement == null) return;

    DomElementAnnotationsManager.getInstance(domElement.getManager().getProject()).addHighlightingListener(new DomElementAnnotationsManager.DomHighlightingListener() {
      @Override
      public void highlightingFinished(final @NotNull DomFileElement element) {
        ApplicationManager.getApplication().invokeLater(() -> {
          if (getComponent().isShowing() && element.isValid()) {
            updateHighlighting();
          }
        });
      }
    }, this);

    for (final AbstractDomChildrenDescription description : domElement.getGenericInfo().getChildrenDescriptions()) {
      final JComponent boundComponent = getBoundComponent(description);
      if (boundComponent != null) {
        if (description instanceof DomFixedChildDescription && DomUtil.isGenericValueType(description.getType())) {
          if ((description.getValues(domElement)).size() == 1) {
            final GenericDomValue element = domElement.getManager().createStableValue(
              () -> domElement.isValid() ? (GenericDomValue)description.getValues(domElement).get(0) : null);
            doBind(DomUIFactory.createControl(element, commitOnEveryChange(element)), boundComponent);
          }
          else {
            //todo not bound

          }
        }
        else if (description instanceof DomCollectionChildDescription) {
          doBind(DomUIFactory.getDomUIFactory().createCollectionControl(domElement, (DomCollectionChildDescription)description), boundComponent);
        }
      }
    }
    reset();
  }

  protected void doBind(final DomUIControl control, final JComponent boundComponent) {
    myBoundComponents.put(boundComponent, control);
    control.bind(boundComponent);
    addComponent(control);
  }

  private JComponent getBoundComponent(final AbstractDomChildrenDescription description) {
    for (Field field : getClass().getDeclaredFields()) {
      try {
        field.setAccessible(true);

        if (description instanceof DomChildrenDescription childrenDescription) {
          if (convertFieldName(field.getName(), childrenDescription).equals(childrenDescription.getXmlElementName()) && field.get(this) instanceof JComponent) {
            return (JComponent)field.get(this);
          }
        }
      }
      catch (IllegalAccessException e) {
        LOG.error(e);
      }
    }

    return null;
  }

  private @NotNull String convertFieldName(@NotNull String propertyName, final DomChildrenDescription description) {
    propertyName = StringUtil.trimStart(propertyName, "my");

    String convertedName = description.getDomNameStrategy(getDomElement()).convertName(propertyName);

    if (description instanceof DomCollectionChildDescription) {
      final String unpluralizedStr = StringUtil.unpluralize(convertedName);

      if (unpluralizedStr != null) return unpluralizedStr;
    }
    return convertedName;
  }

  public final Project getProject() {
    return getDomElement().getManager().getProject();
  }

  public final Module getModule() {
    return getDomElement().getModule();
  }

  protected final DomUIControl getDomControl(JComponent component) {
    return myBoundComponents.get(component);
  }
}
