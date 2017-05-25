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

import java.util.Hashtable;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import org.apache.tools.ant.module.spi.AntEvent;
import org.apache.tools.ant.module.spi.AntLogger;
import org.apache.tools.ant.module.spi.AntSession;
import org.netbeans.api.project.Project;
import org.openide.windows.OutputWriter;

/**
 * The listener is used to initiate JMX listener when Swing Exlorer's
 * ANT target is launched
 * @author Maxim Zakharenkov
 */
public class SwingExplorerListener extends org.apache.tools.ant.module.spi.AntLogger {

    private static final Logger log = Logger.getLogger(SwingExplorerListener.class.getName());
    
    // all targets running swing explorer with their projects
    private static Hashtable<String,Project> targets = new Hashtable<String,Project>();
        
    OutputWriter writer;
    
    private void debug(String msg) {
//        if(writer == null) {
//            InputOutput io = IOProvider.getDefault().getIO("Swing Explorer", false);
//            writer = io.getOut();
//        }
//        writer.println(msg);
        log.log(Level.FINEST, msg);
    }
    
    public static void addNewLaunch(int _port, Project _project) {
        targets.put("swingexplorer-run_" + _port, _project);
    }
    
    @Override
    public boolean interestedInSession(AntSession session) {
        return true;
    }

    @Override
    public boolean interestedInAllScripts(AntSession session) {
        return true;
    }

    @Override
    public String[] interestedInTargets(AntSession session) {
        return AntLogger.ALL_TARGETS;
    }

    @Override
    public void targetStarted(AntEvent event) {
        final String targetName = event.getTargetName();
        debug("Target started: " + targetName);
        
        if(targets.containsKey(targetName)) {
            
            int idx = targetName.lastIndexOf("_");
            final String port = targetName.substring(idx + 1);
            final Project project = targets.get(targetName);
                    
            Thread thread = new Thread() {

                @Override
                public void run() {
                    while(targets.containsKey(targetName)) {
                        try {
                            // perform connection attempts every 3 seconds
                            // when Swing Explorer's ANT target is running
                            Thread.sleep(3000);
                            JMXServiceURL url = new JMXServiceURL("service:jmx:rmi:///jndi/rmi://:" + port + "/server");
                            JMXConnector jmxc = JMXConnectorFactory.connect(url, null);
                            MBeanServerConnection mbsc = jmxc.getMBeanServerConnection();

                            ObjectName name = new ObjectName("org.swingexplorer:name=IDESupport");

                            // notify MBean that IDE connected it
                            mbsc.invoke(name, "connect", new Object[0], new String[0]);

                            // create JMX listening action                            
                            mbsc.addNotificationListener(name, new ActOpenSourceLine(project), null, null);
                            targets.remove(targetName);
                            log.info("NetBeans plugin connected  to Swing Explorer on port " + port);
                        } catch (Exception ex) {
                            log.info("Failed to connect Swing Explorer from NetBeans on port " + port + " next attempt will be made after some delay");
                        }
                    }
                }
            };
            thread.start();
        }
    }

    @Override
    public void targetFinished(AntEvent event) {
        // remove target from active target list
        // to stop "while" loop if it still tries to connect to Swing Explorer
        final String targetName = event.getTargetName();
        targets.remove(targetName);
    }    
}
