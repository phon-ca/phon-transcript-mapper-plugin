package ca.phon.transcriptMapper;

import ca.phon.alignedTypesDatabase.AlignedTypesDatabase;
import ca.phon.app.log.*;
import ca.phon.app.project.*;
import ca.phon.app.session.SessionSelector;
import ca.phon.project.Project;
import ca.phon.project.exceptions.ProjectConfigurationException;
import ca.phon.session.*;
import ca.phon.session.alignedMorphemes.AlignedMorphemesScanner;
import ca.phon.ui.*;
import ca.phon.ui.action.*;
import ca.phon.ui.decorations.DialogHeader;
import ca.phon.ui.fonts.FontPreferences;
import ca.phon.ui.jbreadcrumb.BreadcrumbButton;
import ca.phon.ui.menu.MenuBuilder;
import ca.phon.ui.nativedialogs.*;
import ca.phon.ui.wizard.*;
import ca.phon.util.icons.*;
import ca.phon.worker.*;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.List;

public class ScanProjectWizard extends BreadcrumbWizardFrame {

	private BreadcrumbButton btnStop;

	private WizardStep selectSessionStep;
	private MultiActionButton projectButton;
	private SessionSelector sessionSelector;

	private WizardStep reportStep;
	private BufferPanel reportPanel;

	private Project project;

	private AlignedTypesDatabase db;

	public ScanProjectWizard(Project project, AlignedTypesDatabase db, String title) {
		super(title);
		setWindowName(title);

		this.project = project;
		this.db = db;

		btnStop = new BreadcrumbButton();
		btnStop.setFont(FontPreferences.getTitleFont().deriveFont(Font.BOLD));
		btnStop.setText("Stop");
		btnStop.setBackground(Color.red);
		btnStop.setForeground(Color.white);
		btnStop.addActionListener( (e) -> close() );

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
		reportPanel = bufferPanel;

		bufferPanel.getLogBuffer().getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void insertUpdate(DocumentEvent e) {
				bufferPanel.getLogBuffer().setCaretPosition(e.getOffset());
			}

			@Override
			public void removeUpdate(DocumentEvent e) {

			}

			@Override
			public void changedUpdate(DocumentEvent e) {

			}
		});

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

	@Override
	protected void updateBreadcrumbButtons() {
		super.updateBreadcrumbButtons();

		if(this.getCurrentStep() == reportStep) {
			if(scanProjectTask.getStatus() == PhonTask.TaskStatus.RUNNING) {
				btnStop.setBackground(Color.red);
				btnStop.setForeground(Color.white);
				btnStop.setText("Stop");
			} else {
				btnStop.setText("Close window");
				btnStop.setBackground(nextButton.getBackground());
				btnStop.setForeground(Color.black);
			}
			breadCrumbViewer.add(btnStop);
			setBounds(btnStop);
		} else {
			breadCrumbViewer.remove(btnStop);
		}
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

	public void selectProject(File projectFolder) {
		try {
			this.project = (new DesktopProjectFactory()).openProject(projectFolder);
			updateProjectButton();
			sessionSelector.setProject(this.project);
			updateSessionSelector();
		} catch (IOException | ProjectConfigurationException e) {
			Toolkit.getDefaultToolkit().beep();
			LogUtil.severe(e);
		}
	}

	public void onBrowseForProject(PhonActionEvent pae) {
		final OpenDialogProperties props = new OpenDialogProperties();
		props.setCanChooseDirectories(true);
		props.setCanChooseFiles(false);
		props.setAllowMultipleSelection(false);
		props.setCanCreateDirectories(true);
		props.setParentWindow(CommonModuleFrame.getCurrentFrame());
		props.setTitle("Open Project");
		props.setListener((e) -> {
			final String projectLocal = e.getDialogData() != null ? e.getDialogData().toString() : "";
			SwingUtilities.invokeLater(() -> {
				selectProject(new File(projectLocal));
			});
		});

		NativeDialogs.showOpenDialog(props);
	}

	private void beginProjectScan() {
		PhonWorker.invokeOnNewWorker(scanProjectTask, this::onFinishScan);
	}

	public void onFinishScan() {
		SwingUtilities.invokeLater(() -> {
			updateBreadcrumbButtons();
		});
	}

	@Override
	public void close() {
		if(scanProjectTask.getStatus() == PhonTask.TaskStatus.RUNNING) {
			scanProjectTask.shutdown();
		} else {
			super.close();
		}
	}

	@Override
	protected void next() {
		if(super.getCurrentStep() == this.selectSessionStep) {
			if(this.sessionSelector.getSelectedSessions().size() == 0) {
				showMessageDialog("Scan project", "Please select at least one session", MessageDialogProperties.okOptions);
				return;
			} else {
				beginProjectScan();
			}
		}
		super.next();
	}

	private final PhonTask scanProjectTask = new PhonTask() {
		@Override
		public void performTask() {
			super.setStatus(TaskStatus.RUNNING);

			reportPanel.getLogBuffer().append(String.format("Scanning project %s (%s)\n", project.getName(), project.getLocation()));

			final AlignedMorphemesScanner scanner = new AlignedMorphemesScanner(db);
			for(SessionPath sessionPath:sessionSelector.getSelectedSessions()) {
				if(isShutdown()) {
					reportPanel.getLogBuffer().append("Project scan canceled by user");
					return;
				}
				reportPanel.getLogBuffer().append(String.format("Scanning %s.%s...\n", sessionPath.getCorpus(), sessionPath.getSession()));
				try {
					final Session session = project.openSession(sessionPath.getCorpus(), sessionPath.getSession());
					scanner.scanSession(session);
				} catch (IOException e) {
					super.err = e;
					LogUtil.severe(e);
					super.setStatus(TaskStatus.ERROR);
					return;
				}
			}
			reportPanel.getLogBuffer().append("Scan complete, you may close the window.");
			super.setStatus(TaskStatus.FINISHED);
		}
	};

}
