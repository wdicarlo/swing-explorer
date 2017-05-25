/*
 *   Swing Explorer. Tool for developers exploring Java/Swing-based application internals. 
 * 	 Copyright (C) 2012, Maxim Zakharenkov
 *
 *   This library is free software; you can redistribute it and/or
 *   modify it under the terms of the GNU Lesser General Public
 *   License as published by the Free Software Foundation; either
 *   version 2.1 of the License, or (at your option) any later version.
 *
 *   This library is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *   Lesser General Public License for more details.
 *
 *   You should have received a copy of the GNU Lesser General Public
 *   License along with this library; if not, write to the Free Software
 *   Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *   
 */
package org.swingexplorer.eclipseplugin;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import javax.management.Notification;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.text.IDocument;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.texteditor.ITextEditor;


public class ActOpenSourceLine implements javax.management.NotificationListener {
    
    IJavaProject project;
    ILaunch launch;
    
    public ActOpenSourceLine(IJavaProject _project, ILaunch _launch) {
        project = _project;
        launch = _launch;
    }
    
    public void handleNotification(Notification notification, Object handback) {
        
        @SuppressWarnings("rawtypes")
		HashMap data = (HashMap)notification.getUserData();
        final String className = (String)data.get("className");
        final int lineNumber = (Integer)data.get("lineNumber");
        IJavaElement element = null;
        
        try {
            // find element
            element = internalFindType(project, className, new HashSet<IJavaProject>());
        } catch (Exception e) {
            Utils.logError("Can not find class: " + className, e);
            return;
        }
        
        final IJavaElement finalElement = element;        
        Display display = getDisplay();
        display.syncExec(new Runnable() {
            public void run() {
                try {
                    // open editor
                    IEditorPart editorPart = JavaUI.openInEditor(finalElement);
                    
                    // select line
                    ITextEditor textEditor = (ITextEditor)editorPart;
                    IDocument document= textEditor.getDocumentProvider().getDocument(textEditor.getEditorInput());
                    textEditor.selectAndReveal(document.getLineOffset(lineNumber-1), document.getLineLength(lineNumber-1));
                } catch (Exception e) {
                    Utils.logError("Error opening source file for class: " + className + " line: " + lineNumber, e);
                }
            }
        });
    }
    
    private Display getDisplay() {
        Display display;
        display= Display.getCurrent();
        if (display == null)
            display= Display.getDefault();
        return display;     
    }
    
    private IType internalFindType(IJavaProject project, String className,
                    Set<IJavaProject> visitedProjects)
                    throws JavaModelException {
        if (visitedProjects.contains(project)) return null;

        
        IType type = project.findType(className, (IProgressMonitor) null);
        if (type != null) return type;

        // fix for bug 87492: visit required projects explicitly to also find
        // not exported types
        visitedProjects.add(project);
        IJavaModel javaModel = project.getJavaModel();
        String[] requiredProjectNames = project.getRequiredProjectNames();
        for (int i = 0; i < requiredProjectNames.length; i++) {
            IJavaProject requiredProject = javaModel
                            .getJavaProject(requiredProjectNames[i]);
            if (requiredProject.exists()) {
                type = internalFindType(requiredProject, className,
                                visitedProjects);
                if (type != null) return type;
            }
        }
        return null;
    }
}