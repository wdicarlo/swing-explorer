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
package org.swingexplorer.netbeans;

import java.util.HashMap;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.swing.SwingUtilities;
import org.netbeans.api.java.classpath.GlobalPathRegistry;
import org.netbeans.api.project.Project;
import org.openide.cookies.EditorCookie;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataObject;
import org.openide.text.Line;
import org.openide.windows.OutputWriter;

/**
 *
 * @author Maxim Zakharenkov
 */
public class ActOpenSourceLine implements NotificationListener {

    private static final Logger log = Logger.getLogger(ActOpenSourceLine.class.getName());
    
    // project to open source line for (where Swing Explorer was executed)
    Project project;

    public ActOpenSourceLine(Project _project) {
        project = _project;
    }

    
    OutputWriter writer;
    
    private void debug(String msg) {
//        if(writer == null) {
//            InputOutput io = IOProvider.getDefault().getIO("Swing Explorer", false);
//            writer = io.getOut();
//        }
//        writer.println(msg);
        log.log(Level.FINEST, msg);
    }
 
    void x() {
            String classResource = "javax/swing/JComponent.java";
            Set<FileObject> sourceRoots = GlobalPathRegistry.getDefault().getSourceRoots();
            for (FileObject curRoot : sourceRoots) {
                    FileObject fileObject = curRoot.getFileObject(classResource);
                    if (fileObject != null) {
                        // source file object if found
                        // do something e.g. openEditor(fileObject, lineNumber);
                        return;
                    }
            }
    }
    
    public void handleNotification(Notification notification, Object handback) {
        try {
            HashMap map = (HashMap) notification.getUserData();
            String className = (String) map.get("className");
            int lineNumber = (Integer) map.get("lineNumber");
            String classResource = className.replace(".", "/") + ".java";

            // source roots for all projects
            Set<FileObject> sourceRoots = GlobalPathRegistry.getDefault().getSourceRoots();
            FileObject[] roots = sourceRoots.toArray(new FileObject[sourceRoots.size()]);


            for (FileObject curRoot : roots) {
                debug("Root list:" + curRoot.getPath());
            }

            // first try to find class in the opening project's source root
            String projectPath = project.getProjectDirectory().getPath();
            debug("Project path: " + projectPath);
            for (FileObject curRoot : roots) {
                debug("   Checkig for srcRoot:" + curRoot.getPath());
                if (curRoot.getPath().startsWith(projectPath)) {
                    FileObject fileObject = curRoot.getFileObject(classResource);
                    debug("   startsWith project path ok:" + curRoot.getPath());
                    if (fileObject != null) {
                        debug("   Found:" + fileObject + " in the " + curRoot.getPath());
                        openEditor(fileObject, lineNumber);
                        // we are done
                        return;
                    }
                }
            }

            // if we haven't succeeded with launching project
            // try to find sources in the other projects
            debug("Sources not found in the project. Check other projects ");
            for (FileObject curRoot : roots) {
                debug("   Checkig for srcRoot:" + curRoot.getPath());
                if (!curRoot.getPath().startsWith(projectPath)) {
                    debug("   !startsWith project path ok:" + curRoot.getPath());
                    FileObject fileObject = curRoot.getFileObject(classResource);
                    if (fileObject != null) {
                        debug("   Found:" + fileObject + " in the " + curRoot.getPath());
                        openEditor(fileObject, lineNumber);
                        // we are done
                        return;
                    }
                }
            }

            debug("Source for " + classResource + " not found");
            java.awt.Toolkit.getDefaultToolkit().beep();
        } catch(Exception ex) {
            log.log(Level.SEVERE, "Exception when handling JMX notification", ex);
        }
    }

    private void openEditor(final FileObject fileObject, final int lineNumber) {

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                try {
                    DataObject dob = DataObject.find(fileObject);
                    EditorCookie ed = dob.getCookie(EditorCookie.class);
                    if (ed != null && /* not true e.g. for *_ja.properties */
                            fileObject == dob.getPrimaryFile()) {
                        if (lineNumber == -1) {
                            // if line number is not specified
                            ed.open();
                        } else {
                            ed.openDocument();
                            try {
                                Line l = ed.getLineSet().getOriginal(lineNumber - 1);
                                if (!l.isDeleted()) {
                                    l.show(Line.SHOW_GOTO);
                                }
                            } catch (IndexOutOfBoundsException ioobe) {
                                // can not find line in the document
                                ed.open();
                            }
                        }
                    } else {
                        java.awt.Toolkit.getDefaultToolkit().beep();
                    }

                } catch (Exception ex) {
                    log.log(Level.SEVERE, "Unable to open source file " + fileObject.getPath(), ex);
                }
            }
        });
    }
}

