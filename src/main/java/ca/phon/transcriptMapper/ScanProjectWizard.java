/*
 * Copyright (C) 2005-2022 Gregory Hedlund & Yvan Rose
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *    http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ca.phon.transcriptMapper;

import ca.phon.alignedTypesDatabase.AlignedTypesDatabase;
import ca.phon.app.log.*;
import ca.phon.app.project.*;
import ca.phon.app.session.SessionSelector;
import ca.phon.project.Project;
import ca.phon.project.exceptions.ProjectConfigurationException;
import ca.phon.session.*;
import ca.phon.ui.*;
import ca.phon.ui.action.*;
import ca.phon.ui.decorations.DialogHeader;
import ca.phon.ui.fonts.FontPreferences;
import ca.phon.ui.jbreadcrumb.BreadcrumbButton;
import ca.phon.ui.menu.MenuBuilder;
import ca.phon.ui.nativedialogs.*;
import ca.phon.ui.wizard.*;
import ca.phon.util.*;
import ca.phon.util.icons.*;
import ca.phon.worker.*;
import org.jdesktop.swingx.VerticalLayout;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.io.*;
import java.util.List;
import java.util.*;

public class ScanProjectWizard extends BreadcrumbWizardFrame {

	private final static String projectLangProp(Project project) {
		return String.format("%s.scanProject.language", project.getUUID());
	}

	private BreadcrumbButton btnStop;

	private WizardStep selectSessionStep;
	private MultiActionButton projectButton;
	private SessionSelector sessionSelector;

	private JComboBox<LanguageEntry> languageSelectionBox;

	private WizardStep reportStep;
	private BufferPanel reportPanel;

	private Project project;

	private TranscriptMapperEditorView view;

	private AlignedTypesDatabase db;

	public ScanProjectWizard(Project project, TranscriptMapperEditorView view, String title) {
		super(title);
		setWindowName(title);
		setParentFrame(view.getEditor());

		this.project = project;
		this.view = view;
		this.db = view.getUserDb();

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

	private LanguageEntry getPreviouslySelectedLanguage() {
		final String langProp = projectLangProp(project);
		final Properties props = TranscriptMapperEditorView.getSharedProps();

		final String langTxt = props.getProperty(langProp, "");
		if(langTxt.length() > 0) {
			LanguageEntry entry = LanguageParser.getInstance().getEntryById(langTxt);
			return entry;
		}
		return null;
	}

	private WizardStep createSelectSessionStep() {
		final WizardStep wizardStep = new WizardStep();
		wizardStep.setTitle("Select sessions");

		final DialogHeader header = new DialogHeader("Scan project", "Select project and sessions to scan.");

		final List<LanguageEntry> allLangs = new ArrayList<>(LanguageParser.getInstance().getLanguages());
		Collections.sort(allLangs, Comparator.comparing(LanguageEntry::getName));
		this.languageSelectionBox = new JComboBox<>(allLangs.toArray(new LanguageEntry[0]));
		this.languageSelectionBox.setSelectedItem(getPreviouslySelectedLanguage());
		this.languageSelectionBox.setRenderer(languageEntryListCellRenderer);
//		this.languageSelectionBox.setSelectedItem(SyllabifierLibrary.getInstance().defaultSyllabifierLanguage().getPrimaryLanguage());
		this.languageSelectionBox.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));

		projectButton = new MultiActionButton();
		projectButton.setToolTipText("Click to select project");

		final PhonUIAction<Void> openProjectAct = PhonUIAction.eventConsumer(this::onOpenProject);
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

		final JPanel langPanel = new JPanel(new VerticalLayout());
		langPanel.add(new JLabel("Value for 'Language' tier:"));
		langPanel.add(this.languageSelectionBox);
		final JPanel topPanel = new JPanel(new VerticalLayout());
		topPanel.add(langPanel);
		topPanel.add(projectButton);
		topPanel.setBackground(Color.white);

		final JPanel contentPanel = new JPanel(new BorderLayout());
		contentPanel.add(topPanel, BorderLayout.NORTH);
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

	public void onOpenProject(PhonActionEvent<Void> pae) {
		showSelectProjectMenu();
	}

	private void showSelectProjectMenu() {
		final JPopupMenu selectProjectMenu = new JPopupMenu();
		final MenuBuilder builder = new MenuBuilder(selectProjectMenu);

		final RecentProjects recentProjects = new RecentProjects();
		for(File projectFolder:recentProjects) {
			final PhonUIAction<File> selectProjectAct = PhonUIAction.consumer(this::selectProject, projectFolder);
			selectProjectAct.putValue(PhonUIAction.NAME, projectFolder.getAbsolutePath());
			selectProjectAct.putValue(PhonUIAction.SHORT_DESCRIPTION, "Select project folder: " + projectFolder.getAbsolutePath());
			builder.addItem(".", selectProjectAct);
		}
		builder.addSeparator(".", "browse");

		final PhonUIAction<Void> browseProjectAct = PhonUIAction.eventConsumer(this::onBrowseForProject);
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

	public void onBrowseForProject(PhonActionEvent<Void> pae) {
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
				final Properties props = TranscriptMapperEditorView.getSharedProps();
				if(languageSelectionBox.getSelectedItem() != null) {
					props.setProperty(projectLangProp(project), ((LanguageEntry) languageSelectionBox.getSelectedItem()).getId());
					try {
						TranscriptMapperEditorView.saveSharedProps();
					} catch (IOException e) {
						LogUtil.warning(e);
					}
				}
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

			final AlignedMorphemesScanner scanner = new AlignedMorphemesScanner(db, (LanguageEntry) languageSelectionBox.getSelectedItem());
			for(SessionPath sessionPath:sessionSelector.getSelectedSessions()) {
				if(isShutdown()) {
					reportPanel.getLogBuffer().append("Project scan canceled by user");
					return;
				}
				reportPanel.getLogBuffer().append(String.format("Scanning %s.%s...\n", sessionPath.getCorpus(), sessionPath.getSession()));
				try {
					final Session session = project.openSession(sessionPath.getCorpus(), sessionPath.getSession());
					scanner.scanSession(project.getUUID(), view.getEditor().getProject().getUUID(), session);
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

	private final ListCellRenderer<LanguageEntry> languageEntryListCellRenderer = new ListCellRenderer<LanguageEntry>() {

		private final DefaultListCellRenderer internalRenderer = new DefaultListCellRenderer();

		@Override
		public Component getListCellRendererComponent(JList<? extends LanguageEntry> list, LanguageEntry value, int index, boolean isSelected, boolean cellHasFocus) {
			JLabel retVal = (JLabel) internalRenderer.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

			if(value != null) {
				retVal.setText(String.format("%s (%s)", value.getName(), value.getId()));
			} else {
				retVal.setText("no language selected");
			}

			return retVal;
		}

	};

}
