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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.tools.ant.module.api.support.ActionUtils;
import org.netbeans.api.project.Project;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.nodes.Node;
import org.openide.util.HelpCtx;
import org.openide.util.actions.NodeAction;
import org.swingexplorer.install.Installer;

/**
 *
 * @author  Maxim Zakharenkov
 */
public class ActDebug extends NodeAction {

    private static final Logger log = Logger.getLogger(ActDebug.class.getName());

    public ActDebug() {
    }

    @Override
    protected void performAction(Node[] activatedNodes) {

        // obtaining reference to project from some parent node
        Project project = null;
        Node curnode = activatedNodes[0];
        while (curnode.getParentNode() != null && project == null) {
            project = curnode.getLookup().lookup(Project.class);
            curnode = curnode.getParentNode();
        }

        // parameters
        String homeDir = Installer.getHomeDirectory(false);
        final int port = SocketUtil.findFreePort();

        // adding project and port to Swing Explirer listener to
        // be able to connect to it through JMX when Swing Explorer is launched
        SwingExplorerListener.addNewLaunch(port, project);
        log.info("Running Swing Explorer listening JMX port " + port);

        // create ant script on the fly
        String script = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<project name=\"SwingExplorerRun\" default=\"swingexplorer-run_" + port + "\" basedir=\"../..\">" +
                "<import file=\"../build-impl.xml\"/>" +
                "<target name=\"swingexplorer-run_" + port + "\" description=\"swingexplorer-run\" depends=\"jar\">" +

                "<nbjpdastart name=\"NetBeans\" addressproperty=\"debug.port\" transport=\"dt_socket\">" +
                "<classpath>" +
                    "<fileset dir=\"${nbplatform.default.netbeans.dest.dir}\">" +
                        "<include name=\"**/*.jar\"/>" +
                    "</fileset>" +
                    "<fileset dir=\"${cluster}\">" +
                        "<include name=\"**/*.jar\"/>" +
                    "</fileset>" +
                "</classpath>" +
                "</nbjpdastart>" +
                "<property name=\"debug.pause\" value=\"n\"/>" +
                "<property name=\"debug.args\" value=\"-J-Xdebug -J-Xnoagent -J-Xrunjdwp:transport=dt_socket,suspend=${debug.pause},server=n,address=${debug.port}\"/>" +
                "<property file=\"nbproject/project.properties\"/>" +
                "<java classpath=\"" + homeDir + "/swexpl.jar;${run.classpath}\" classname=\"org.swingexplorer.Launcher\" fork=\"true\">" +
                "<arg value=\"${main.class}\"/>" +
                "<jvmarg line=\"${debug.args} -Dswex.mport=" + port + " -Dcom.sun.management.jmxremote -javaagent:&#34;" + homeDir + "/swag.jar&#34; -Xbootclasspath/a:&#34;" + homeDir + "/swag.jar&#34; \"/>" +
                "</java>" +
                "</target>" +
                "</project>";
        try {

            FileObject fo = project.getProjectDirectory().getFileObject("/nbproject/private");
            FileObject zfO = FileUtil.createData(fo, "ant-swe.xml");
            File zf = FileUtil.toFile(zfO);
            BufferedWriter out = new BufferedWriter(new FileWriter(zf.getAbsoluteFile()));
            out.write(script);
            out.close();
            FileObject zfo = FileUtil.toFileObject(FileUtil.normalizeFile(zf));

            // run ant target
            ActionUtils.runTarget(zfo, new String[]{"swingexplorer-run_" + port}, null);

            zf.deleteOnExit();
        } catch (IOException ex) {
            log.log(Level.SEVERE, "Can not execute Swing Explorer", ex);
        }
    }

    @Override
    protected boolean enable(Node[] arg0) {
        return true;
    }

    @Override
    public String getName() {
        return "Debug with Swing Explorer";
    }

    @Override
    public HelpCtx getHelpCtx() {
        return null;
    }
}
