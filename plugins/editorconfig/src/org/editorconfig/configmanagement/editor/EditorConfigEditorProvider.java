// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.editorconfig.configmanagement.editor;

import com.intellij.application.options.CodeStyle;
import com.intellij.lang.Language;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.highlighter.EditorHighlighter;
import com.intellij.openapi.editor.highlighter.EditorHighlighterFactory;
import com.intellij.openapi.fileEditor.AsyncFileEditorProvider;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorPolicy;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.openapi.fileEditor.impl.text.PsiAwareTextEditorProvider;
import com.intellij.openapi.fileEditor.impl.text.TextEditorProvider;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeRegistry;
import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.codeStyle.CodeStyleSettingsManager;
import com.intellij.psi.codeStyle.LanguageCodeStyleSettingsProvider;
import org.editorconfig.language.filetype.EditorConfigFileType;
import org.editorconfig.settings.EditorConfigSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Set;

final class EditorConfigEditorProvider implements AsyncFileEditorProvider, DumbAware {
  private static final String EDITOR_TYPE_ID = "org.editorconfig.configmanagement.editor";

  static final int MAX_PREVIEW_LENGTH = 10000;

  private static final PsiAwareTextEditorProvider ourMainEditorProvider = new PsiAwareTextEditorProvider();

  @Override
  public @NotNull Builder createEditorAsync(@NotNull Project project, @NotNull VirtualFile file) {
    return new MyEditorBuilder(project, file);
  }

  @Override
  public boolean accept(@NotNull Project project, @NotNull VirtualFile file) {
    return FileTypeRegistry.getInstance().isFileOfType(file, EditorConfigFileType.INSTANCE);
  }

  @Override
  public @NotNull FileEditor createEditor(@NotNull Project project, @NotNull VirtualFile file) {
    return new MyEditorBuilder(project, file).build();
  }

  @Override
  public @NotNull String getEditorTypeId() {
    return EDITOR_TYPE_ID;
  }

  @Override
  public @NotNull FileEditorPolicy getPolicy() {
    return FileEditorPolicy.HIDE_DEFAULT_EDITOR;
  }

  private static final class MyEditorBuilder extends Builder {
    final Project myProject;
    final VirtualFile myFile;
    final Set<String> myEncodings;

    private MyEditorBuilder(Project project, VirtualFile file) {
      myProject = project;
      myFile = file;
      myEncodings = EditorConfigStatusListener.extractEncodings(project, file);
    }

    @Override
    public @NotNull FileEditor build() {
      FileEditor result;
      VirtualFile contextFile = EditorConfigPreviewManager.getInstance(myProject).getAssociatedPreviewFile(myFile);
      if (contextFile != null && CodeStyle.getSettings(myProject).getCustomSettings(EditorConfigSettings.class).ENABLED) {
        Document document = EditorFactory.getInstance().createDocument(getPreviewText(contextFile));
        Disposable disposable = Disposer.newDisposable();
        EditorConfigPreviewFile previewFile = new EditorConfigPreviewFile(myProject, contextFile, document, disposable);
        FileEditor previewEditor = createPreviewEditor(document, previewFile);
        TextEditor ecTextEditor = (TextEditor)TextEditorProvider.getInstance().createEditor(myProject, myFile);
        result = new EditorConfigEditorWithPreview(myFile, myProject, ecTextEditor, previewEditor);
        Disposer.register(result, disposable);
      }
      else {
        result = ourMainEditorProvider.createEditor(myProject, myFile);
      }
      EditorConfigStatusListener statusListener = new EditorConfigStatusListener(myProject, myFile, myEncodings);
      CodeStyleSettingsManager.getInstance(myProject).subscribe(statusListener, result);
      return result;
    }

    private FileEditor createPreviewEditor(@NotNull Document document, @NotNull EditorConfigPreviewFile previewFile) {
      Editor previewEditor = EditorFactory.getInstance().createEditor(document, myProject);
      if (previewEditor instanceof EditorEx) {
        EditorHighlighter highlighter = EditorHighlighterFactory.getInstance().createEditorHighlighter(myProject, previewFile);
        ((EditorEx)previewEditor).setHighlighter(highlighter);
      }
      return new EditorConfigPreviewFileEditor(previewEditor, previewFile);
    }

    private static @NotNull String getPreviewText(@NotNull VirtualFile file) {
      if (file.getLength() <= MAX_PREVIEW_LENGTH) {
        try {
          return StringUtil.convertLineSeparators(VfsUtilCore.loadText(file));
        }
        catch (IOException e) {
          // Ignore
        }
      }
      Language language = getLanguage(file);
      if (language != null) {
        LanguageCodeStyleSettingsProvider provider = LanguageCodeStyleSettingsProvider.forLanguage(language);
        if (provider != null) {
          String sample = provider.getCodeSample(LanguageCodeStyleSettingsProvider.SettingsType.INDENT_SETTINGS);
          if (sample != null) return sample;
        }
      }
      return "No preview";
    }
  }

  static @Nullable Language getLanguage(@NotNull VirtualFile virtualFile) {
    FileType fileType = virtualFile.getFileType();
    return fileType instanceof LanguageFileType ? ((LanguageFileType)fileType).getLanguage() : null;
  }
}
