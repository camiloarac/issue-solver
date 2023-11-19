package org.jetbrains.plugins.template.toolWindow;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.warmup.util.ConsoleLog;
import org.jetbrains.plugins.template.MyBundle;
import org.jetbrains.plugins.template.services.MyProjectService;
import org.jetbrains.plugins.template.MyGitConnection;
import org.jetbrains.plugins.template.MyOpenAIRequest;
import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GHIssueState;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.Console;
import java.io.IOException;
import java.util.List;
public class MyToolWindowFactory implements ToolWindowFactory {

    private static final Logger LOG = Logger.getInstance(MyToolWindowFactory.class);

    public MyToolWindowFactory() {
        LOG.warn("Don't forget to remove all non-needed sample code files with their corresponding registration entries in `plugin.xml`.");
    }

    @Override
    public void createToolWindowContent(Project project, ToolWindow toolWindow) {
        MyToolWindow myToolWindow = new MyToolWindow(toolWindow);
        Content content = ContentFactory.SERVICE.getInstance().createContent(myToolWindow.getContent(), null, false);
        toolWindow.getContentManager().addContent(content);
    }

    @Override
    public boolean shouldBeAvailable(Project project) {
        return true;
    }

    public static class MyToolWindow {

        private final MyProjectService service;

        public MyToolWindow(ToolWindow toolWindow) {
            this.service = ServiceManager.getService(toolWindow.getProject(), MyProjectService.class);
        }

        public JBPanel<JBPanel<?>> getContent() {
            JBPanel<JBPanel<?>> panel = new JBPanel<>();
            panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

            MyGitConnection myGitConnection = new MyGitConnection();

            try {
                List<GHIssue> myIssues = myGitConnection.getRepo().getIssues(GHIssueState.OPEN);
                for(GHIssue issue : myIssues) {
                    String text = "#" + String.valueOf(issue.getNumber())
                                + ": " + issue.getTitle()
                                + ", " + issue.getBody().substring(4, 25) + "..."
                                + ", developer: " + issue.getAssignee().getLogin();
                    addPanel(text, panel);
                    if (issue.getNumber() == 4) {
                        String requestText = issue.getBody();
                        MyOpenAIRequest myOpenAIRequest = new MyOpenAIRequest(requestText);
                        // MyOpenAIRequest myOpenAIRequest = new MyOpenAIRequest("You are a dog and will speak as such.");
                    }

                }
            } catch (IOException e) {
                e.printStackTrace();
            }


            return panel;
        }

        private void addPanel(String text, Container container) {
            JBPanel panel = new JBPanel<>();
            panel.setMaximumSize(new Dimension(580, 50));
            JBLabel label = new JBLabel(text);
            panel.add(label);
            container.add(panel);
        }
    }
}
