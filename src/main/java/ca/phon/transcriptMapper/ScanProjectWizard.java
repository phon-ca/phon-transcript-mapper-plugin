package ca.phon.transcriptMapper;

import ca.phon.app.log.BufferPanel;
import ca.phon.app.project.RecentProjects;
import ca.phon.app.session.SessionSelector;
import ca.phon.project.Project;
import ca.phon.session.SessionPath;
import ca.phon.ui.*;
import ca.phon.ui.action.*;
import ca.phon.ui.decorations.DialogHeader;
import ca.phon.ui.fonts.FontPreferences;
import ca.phon.ui.menu.MenuBuilder;
import ca.phon.ui.wizard.*;
import ca.phon.util.icons.*;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.*;
import java.util.List;

public class ScanProjectWizard extends BreadcrumbWizardFrame {

	private WizardStep selectSessionStep;
	private MultiActionButton projectButton;
	private SessionSelector sessionSelector;

	private WizardStep reportStep;
	private BufferPanel reportPanel;

	private Project project;

	public ScanProjectWizard(Project project, String title) {
		super(title);

		this.project = project;

		this.selectSessionStep = createSelectSessionStep();
		this.selectSessionStep.setNextStep(1);
		addWizardStep(this.selectSessionStep);

		this.reportStep = createReportStep();
		this.reportStep.setPrevStep(0);
		this.reportStep.setNextStep(-1);
		addWizardStep(this.reportStep);

	}

	private WizardStep createSelectSessionStep() {
		final WizardStep wizardStep = new WizardStep();
		wizardStep.setTitle("Select sessions");

		final DialogHeader header = new DialogHeader("Scan project", "Select project and sessions to scan.");

		projectButton = new MultiActionButton();
		projectButton.setToolTipText("Click to select project");

		final PhonUIAction openProjectAct = new PhonUIAction(this, "onOpenProject");
		openProjectAct.putValue(PhonUIAction.NAME, "Select project...");
		openProjectAct.putValue(PhonUIAction.SHORT_DESCRIPTION, "Select project to scan");
		openProjectAct.putValue(PhonUIAction.SMALL_ICON, IconManager.getInstance().getIcon("actions/document-open", IconSize.SMALL));
		openProjectAct.putValue(PhonUIAction.LARGE_ICON_KEY, IconManager.getInstance().getIcon("actions/document-open", IconSize.MEDIUM));
		projectButton.setDefaultAction(openProjectAct);
		projectButton.setBackground(PhonGuiConstants.PHON_UI_STRIP_COLOR);
		projectButton.getTopLabel().setFont(FontPreferences.getTitleFont());
		projectButton.setBorder(BorderFactory.createEtchedBorder());
		projectButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		updateProjectButton();

		sessionSelector = new SessionSelector(project);
		updateSessionSelector();

		wizardStep.setLayout(new BorderLayout());
		wizardStep.add(header, BorderLayout.NORTH);

		final JPanel contentPanel = new JPanel(new BorderLayout());
		contentPanel.add(projectButton, BorderLayout.NORTH);
		contentPanel.add(new JScrollPane(sessionSelector), BorderLayout.CENTER);
		wizardStep.add(contentPanel, BorderLayout.CENTER);

		return wizardStep;
	}

	private WizardStep createReportStep() {
		final WizardStep wizardStep = new WizardStep();
		wizardStep.setTitle("Scan project");

		final DialogHeader header = new DialogHeader("Scan project", "Performing session scan...");

		final BufferPanel bufferPanel = new BufferPanel("Scan project");
		bufferPanel.showBuffer();

		wizardStep.setLayout(new BorderLayout());
		wizardStep.add(header, BorderLayout.NORTH);
		wizardStep.add(bufferPanel, BorderLayout.CENTER);

		return wizardStep;
	}

	private void updateProjectButton() {
		projectButton.setTopLabelText(project.getName());
		projectButton.setBottomLabelText(project.getLocation());

		final ImageIcon icon = IconManager.getInstance().getSystemIconForPath(project.getLocation(), "actions/document-open", IconSize.SMALL);
		projectButton.getBottomLabel().setIcon(icon);
	}

	private void updateSessionSelector() {
		sessionSelector.expandAll();
		// select all
		List<SessionPath> sessionPaths = new ArrayList<>();
		for(String corpus:this.project.getCorpora()) {
			for(String sessionName:this.project.getCorpusSessions(corpus)) {
				sessionPaths.add(new SessionPath(corpus, sessionName));
			}
		}
		sessionSelector.setSelectedSessions(sessionPaths);
	}

	public void onOpenProject(PhonActionEvent pae) {
		showSelectProjectMenu();
	}

	private void showSelectProjectMenu() {
		final JPopupMenu selectProjectMenu = new JPopupMenu();
		final MenuBuilder builder = new MenuBuilder(selectProjectMenu);

		final RecentProjects recentProjects = new RecentProjects();
		for(File projectFolder:recentProjects) {
			final PhonUIAction selectProjectAct = new PhonUIAction(this, "selectProject", projectFolder);
			selectProjectAct.putValue(PhonUIAction.NAME, projectFolder.getAbsolutePath());
			selectProjectAct.putValue(PhonUIAction.SHORT_DESCRIPTION, "Select project folder: " + projectFolder.getAbsolutePath());
			builder.addItem(".", selectProjectAct);
		}
		builder.addSeparator(".", "browse");

		final PhonUIAction browseProjectAct = new PhonUIAction(this, "onBrowseForProject");
		browseProjectAct.putValue(PhonUIAction.NAME, "Select project folder...");
		browseProjectAct.putValue(PhonUIAction.SHORT_DESCRIPTION, "Browse for project on disk");
		builder.addItem(".", browseProjectAct);

		selectProjectMenu.show(this.projectButton, 0, this.projectButton.getHeight());
	}

	public void selectProject(Project project) {
		this.project = project;
		updateProjectButton();
		updateSessionSelector();
	}

	public void onBrowseForProject(PhonActionEvent pae) {

	}

}
