package com.dadscape.ui;

import com.dadscape.manager.DiaryManager;
import com.dadscape.model.ClanDiary;
import com.dadscape.model.DiaryTask;
import com.dadscape.model.DiaryTier;
import com.dadscape.model.TaskType;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.ui.ColorScheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Dialog for editing a clan diary - add/remove tiers and tasks
 */
@Slf4j
public class DiaryEditorDialog extends JDialog
{
	private static final int DIALOG_WIDTH = 600;
	private static final int DIALOG_HEIGHT = 700;
	private static final int BORDER_OFFSET = 10;

	private final DiaryManager diaryManager;
	private final ClanDiary diary;
	private final String lastModifiedBy;
	private final Runnable onSaveCallback;

	// UI Components
	private JTextField nameField;
	private JTextField categoryField;
	private JTextArea descriptionArea;
	private JPanel tiersPanel;
	private List<TierPanel> tierPanels;

	public DiaryEditorDialog(
		JFrame parent,
		DiaryManager diaryManager,
		ClanDiary diary,
		String lastModifiedBy,
		Runnable onSaveCallback
	)
	{
		super(parent, "Edit Diary: " + diary.getName(), true);
		this.diaryManager = diaryManager;
		this.diary = diary;
		this.lastModifiedBy = lastModifiedBy;
		this.onSaveCallback = onSaveCallback;
		this.tierPanels = new ArrayList<>();

		initComponents();
		loadDiaryData();

		setSize(DIALOG_WIDTH, DIALOG_HEIGHT);
		setLocationRelativeTo(parent);
	}

	private void initComponents()
	{
		setLayout(new BorderLayout());
		getContentPane().setBackground(ColorScheme.DARK_GRAY_COLOR);

		// Top panel - Diary metadata
		JPanel topPanel = createMetadataPanel();
		add(topPanel, BorderLayout.NORTH);

		// Center panel - Tiers and tasks
		JPanel centerPanel = createTiersPanel();
		add(centerPanel, BorderLayout.CENTER);

		// Bottom panel - Action buttons
		JPanel bottomPanel = createButtonPanel();
		add(bottomPanel, BorderLayout.SOUTH);
	}

	private JPanel createMetadataPanel()
	{
		JPanel panel = new JPanel();
		panel.setLayout(new GridLayout(0, 1, 5, 5));
		panel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		panel.setBorder(new EmptyBorder(BORDER_OFFSET, BORDER_OFFSET, BORDER_OFFSET, BORDER_OFFSET));

		// Name
		JLabel nameLabel = new JLabel("Diary Name:");
		nameLabel.setForeground(Color.WHITE);
		panel.add(nameLabel);

		nameField = new JTextField();
		panel.add(nameField);

		// Category
		JLabel categoryLabel = new JLabel("Category:");
		categoryLabel.setForeground(Color.WHITE);
		panel.add(categoryLabel);

		categoryField = new JTextField();
		panel.add(categoryField);

		// Description
		JLabel descLabel = new JLabel("Description:");
		descLabel.setForeground(Color.WHITE);
		panel.add(descLabel);

		descriptionArea = new JTextArea(2, 40);
		descriptionArea.setLineWrap(true);
		descriptionArea.setWrapStyleWord(true);
		JScrollPane descScroll = new JScrollPane(descriptionArea);
		panel.add(descScroll);

		return panel;
	}

	private JPanel createTiersPanel()
	{
		JPanel wrapper = new JPanel(new BorderLayout());
		wrapper.setBackground(ColorScheme.DARK_GRAY_COLOR);

		// Header with "Add Tier" button
		JPanel headerPanel = new JPanel(new BorderLayout());
		headerPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		headerPanel.setBorder(new EmptyBorder(5, BORDER_OFFSET, 5, BORDER_OFFSET));

		JLabel tiersLabel = new JLabel("Tiers:");
		tiersLabel.setForeground(Color.WHITE);
		tiersLabel.setFont(new Font("Arial", Font.BOLD, 14));
		headerPanel.add(tiersLabel, BorderLayout.WEST);

		JButton addTierButton = new JButton("+ Add Tier");
		addTierButton.addActionListener(e -> addNewTier());
		headerPanel.add(addTierButton, BorderLayout.EAST);

		wrapper.add(headerPanel, BorderLayout.NORTH);

		// Tiers list (scrollable)
		tiersPanel = new JPanel();
		tiersPanel.setLayout(new BoxLayout(tiersPanel, BoxLayout.Y_AXIS));
		tiersPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);

		JScrollPane scrollPane = new JScrollPane(tiersPanel);
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		wrapper.add(scrollPane, BorderLayout.CENTER);

		return wrapper;
	}

	private JPanel createButtonPanel()
	{
		JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		panel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		panel.setBorder(new EmptyBorder(BORDER_OFFSET, BORDER_OFFSET, BORDER_OFFSET, BORDER_OFFSET));

		JButton saveButton = new JButton("Save");
		saveButton.addActionListener(e -> saveDiary());
		panel.add(saveButton);

		JButton cancelButton = new JButton("Cancel");
		cancelButton.addActionListener(e -> dispose());
		panel.add(cancelButton);

		return panel;
	}

	private void loadDiaryData()
	{
		// Load metadata
		nameField.setText(diary.getName());
		categoryField.setText(diary.getCategory());
		descriptionArea.setText(diary.getDescription());

		// Load tiers
		for (DiaryTier tier : diary.getTiers())
		{
			addTierPanel(tier);
		}
	}

	private void addNewTier()
	{
		String tierName = JOptionPane.showInputDialog(
			this,
			"Enter tier name (e.g., Easy, Medium, Hard, Elite):",
			"New Tier",
			JOptionPane.PLAIN_MESSAGE
		);

		if (tierName != null && !tierName.trim().isEmpty())
		{
			DiaryTier tier = DiaryTier.create(tierName.trim(), "#00FF00", tierPanels.size());
			addTierPanel(tier);
		}
	}

	private void addTierPanel(DiaryTier tier)
	{
		TierPanel tierPanel = new TierPanel(tier, () -> removeTierPanel(tier));
		tierPanels.add(tierPanel);
		tiersPanel.add(tierPanel);
		tiersPanel.add(Box.createRigidArea(new Dimension(0, 5)));
		tiersPanel.revalidate();
		tiersPanel.repaint();
	}

	private void removeTierPanel(DiaryTier tier)
	{
		tierPanels.removeIf(tp -> tp.getTier() == tier);
		tiersPanel.removeAll();

		for (TierPanel tp : tierPanels)
		{
			tiersPanel.add(tp);
			tiersPanel.add(Box.createRigidArea(new Dimension(0, 5)));
		}

		tiersPanel.revalidate();
		tiersPanel.repaint();
	}

	private void saveDiary()
	{
		// Update diary metadata
		diary.setName(nameField.getText().trim());
		diary.setCategory(categoryField.getText().trim());
		diary.setDescription(descriptionArea.getText().trim());

		// Update tiers
		diary.getTiers().clear();
		for (int i = 0; i < tierPanels.size(); i++)
		{
			DiaryTier tier = tierPanels.get(i).getTier();
			tier.setOrder(i);
			diary.getTiers().add(tier);
		}

		// Increment version and update modified info
		diary.incrementVersion();
		diary.setLastModifiedBy(lastModifiedBy);
		diary.setLastModified(System.currentTimeMillis());

		// Save to manager
		boolean success = diaryManager.updateDiary(diary);

		if (success)
		{
			JOptionPane.showMessageDialog(
				this,
				"Diary saved successfully!",
				"Success",
				JOptionPane.INFORMATION_MESSAGE
			);

			// Call callback to refresh UI
			if (onSaveCallback != null)
			{
				onSaveCallback.run();
			}

			dispose();
		}
		else
		{
			JOptionPane.showMessageDialog(
				this,
				"Failed to save diary.",
				"Error",
				JOptionPane.ERROR_MESSAGE
			);
		}
	}

	/**
	 * Inner class representing a single tier panel
	 */
	private class TierPanel extends JPanel
	{
		private final DiaryTier tier;
		private final JPanel tasksPanel;
		private final List<TaskPanel> taskPanels;

		public TierPanel(DiaryTier tier, Runnable onDelete)
		{
			this.tier = tier;
			this.taskPanels = new ArrayList<>();

			setLayout(new BorderLayout());
			setBackground(ColorScheme.DARKER_GRAY_COLOR);
			setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createLineBorder(Color.GRAY),
				new EmptyBorder(5, 5, 5, 5)
			));

			// Header
			JPanel headerPanel = new JPanel(new BorderLayout());
			headerPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);

			JLabel tierLabel = new JLabel(tier.getTierName() + " (" + tier.getTaskCount() + " tasks)");
			tierLabel.setForeground(Color.WHITE);
			tierLabel.setFont(new Font("Arial", Font.BOLD, 12));
			headerPanel.add(tierLabel, BorderLayout.WEST);

			JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
			buttonPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);

			JButton addTaskButton = new JButton("+ Task");
			addTaskButton.addActionListener(e -> addNewTask());
			buttonPanel.add(addTaskButton);

			JButton deleteButton = new JButton("Delete Tier");
			deleteButton.addActionListener(e -> {
				int confirm = JOptionPane.showConfirmDialog(
					this,
					"Delete tier '" + tier.getTierName() + "'?",
					"Confirm Delete",
					JOptionPane.YES_NO_OPTION
				);
				if (confirm == JOptionPane.YES_OPTION)
				{
					onDelete.run();
				}
			});
			buttonPanel.add(deleteButton);

			headerPanel.add(buttonPanel, BorderLayout.EAST);
			add(headerPanel, BorderLayout.NORTH);

			// Tasks panel
			tasksPanel = new JPanel();
			tasksPanel.setLayout(new BoxLayout(tasksPanel, BoxLayout.Y_AXIS));
			tasksPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
			tasksPanel.setBorder(new EmptyBorder(5, 10, 5, 5));

			// Load existing tasks
			for (DiaryTask task : tier.getTasks())
			{
				addTaskPanel(task);
			}

			JScrollPane scrollPane = new JScrollPane(tasksPanel);
			scrollPane.setPreferredSize(new Dimension(550, 150));
			scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
			scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
			add(scrollPane, BorderLayout.CENTER);
		}

		private void addNewTask()
		{
			DiaryTask task = DiaryTask.create("New task description", TaskType.CUSTOM);
			addTaskPanel(task);
			tier.addTask(task);
		}

		private void addTaskPanel(DiaryTask task)
		{
			TaskPanel taskPanel = new TaskPanel(task, () -> removeTaskPanel(task));
			taskPanels.add(taskPanel);
			tasksPanel.add(taskPanel);
			tasksPanel.add(Box.createRigidArea(new Dimension(0, 3)));
			tasksPanel.revalidate();
			tasksPanel.repaint();
		}

		private void removeTaskPanel(DiaryTask task)
		{
			tier.removeTask(task.getId());
			taskPanels.removeIf(tp -> tp.getTask() == task);
			tasksPanel.removeAll();

			for (TaskPanel tp : taskPanels)
			{
				tasksPanel.add(tp);
				tasksPanel.add(Box.createRigidArea(new Dimension(0, 3)));
			}

			tasksPanel.revalidate();
			tasksPanel.repaint();
		}

		public DiaryTier getTier()
		{
			return tier;
		}
	}

	/**
	 * Inner class representing a single task panel
	 */
	private class TaskPanel extends JPanel
	{
		private final DiaryTask task;
		private JTextField descriptionField;
		private JComboBox<TaskType> typeComboBox;
		private JPanel requirementsPanel;

		public TaskPanel(DiaryTask task, Runnable onDelete)
		{
			this.task = task;

			setLayout(new BorderLayout(5, 0));
			setBackground(ColorScheme.DARKER_GRAY_COLOR);
			setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createLineBorder(Color.DARK_GRAY),
				new EmptyBorder(3, 5, 3, 5)
			));

			// Left: Description, type, and dynamic requirements
			JPanel leftPanel = new JPanel();
			leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
			leftPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);

			// Description field
			descriptionField = new JTextField(task.getDescription());
			descriptionField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));
			descriptionField.addActionListener(e -> updateDescription());
			descriptionField.addFocusListener(new java.awt.event.FocusAdapter()
			{
				public void focusLost(java.awt.event.FocusEvent evt)
				{
					updateDescription();
				}
			});
			leftPanel.add(descriptionField);

			leftPanel.add(Box.createRigidArea(new Dimension(0, 2)));

			// Type selector
			typeComboBox = new JComboBox<>(TaskType.values());
			typeComboBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));
			typeComboBox.setSelectedItem(task.getType());
			typeComboBox.addActionListener(e -> {
				task.setType((TaskType) typeComboBox.getSelectedItem());
				updateRequirementsPanel();
			});
			leftPanel.add(typeComboBox);

			leftPanel.add(Box.createRigidArea(new Dimension(0, 2)));

			// Dynamic requirements panel
			requirementsPanel = new JPanel();
			requirementsPanel.setLayout(new BoxLayout(requirementsPanel, BoxLayout.Y_AXIS));
			requirementsPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
			updateRequirementsPanel();
			leftPanel.add(requirementsPanel);

			add(leftPanel, BorderLayout.CENTER);

			// Right: Delete button
			JButton deleteButton = new JButton("X");
			deleteButton.setPreferredSize(new Dimension(45, 50));
			deleteButton.addActionListener(e -> onDelete.run());
			add(deleteButton, BorderLayout.EAST);
		}

		private void updateDescription()
		{
			String desc = descriptionField.getText();
			task.setDescription(desc);
		}

		private void updateRequirementsPanel()
		{
			requirementsPanel.removeAll();

			TaskType type = task.getType();

			if (type == TaskType.KILL)
			{
				// NPC name field
				JPanel npcPanel = new JPanel(new BorderLayout(5, 0));
				npcPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
				npcPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));

				JLabel npcLabel = new JLabel("NPC:");
				npcLabel.setForeground(Color.LIGHT_GRAY);
				npcLabel.setPreferredSize(new Dimension(60, 20));
				npcPanel.add(npcLabel, BorderLayout.WEST);

				JTextField npcField = new JTextField(task.getRequirement("npc"));
				npcField.addActionListener(e -> {
					task.addRequirement("npc", npcField.getText());
					updateDescription();
				});
				npcField.addFocusListener(new java.awt.event.FocusAdapter()
				{
					public void focusLost(java.awt.event.FocusEvent evt)
					{
						task.addRequirement("npc", npcField.getText());
						updateDescription();
					}
				});
				npcPanel.add(npcField, BorderLayout.CENTER);

				requirementsPanel.add(npcPanel);
				requirementsPanel.add(Box.createRigidArea(new Dimension(0, 2)));

				// Kill count field
				JPanel countPanel = new JPanel(new BorderLayout(5, 0));
				countPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
				countPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));

				JLabel countLabel = new JLabel("Count:");
				countLabel.setForeground(Color.LIGHT_GRAY);
				countLabel.setPreferredSize(new Dimension(60, 20));
				countPanel.add(countLabel, BorderLayout.WEST);

				JTextField countField = new JTextField(task.getRequirement("count"));
				countField.addActionListener(e -> {
					task.addRequirement("count", countField.getText());
					updateDescription();
				});
				countField.addFocusListener(new java.awt.event.FocusAdapter()
				{
					public void focusLost(java.awt.event.FocusEvent evt)
					{
						task.addRequirement("count", countField.getText());
						updateDescription();
					}
				});
				countPanel.add(countField, BorderLayout.CENTER);

				requirementsPanel.add(countPanel);
			}
			else if (type == TaskType.SKILL)
			{
				// Skill name field
				JPanel skillPanel = new JPanel(new BorderLayout(5, 0));
				skillPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
				skillPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));

				JLabel skillLabel = new JLabel("Skill:");
				skillLabel.setForeground(Color.LIGHT_GRAY);
				skillLabel.setPreferredSize(new Dimension(60, 20));
				skillPanel.add(skillLabel, BorderLayout.WEST);

				JTextField skillField = new JTextField(task.getRequirement("skill"));
				skillField.addFocusListener(new java.awt.event.FocusAdapter()
				{
					public void focusLost(java.awt.event.FocusEvent evt)
					{
						task.addRequirement("skill", skillField.getText());
					}
				});
				skillPanel.add(skillField, BorderLayout.CENTER);

				requirementsPanel.add(skillPanel);
				requirementsPanel.add(Box.createRigidArea(new Dimension(0, 2)));

				// Level field
				JPanel levelPanel = new JPanel(new BorderLayout(5, 0));
				levelPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
				levelPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));

				JLabel levelLabel = new JLabel("Level:");
				levelLabel.setForeground(Color.LIGHT_GRAY);
				levelLabel.setPreferredSize(new Dimension(60, 20));
				levelPanel.add(levelLabel, BorderLayout.WEST);

				JTextField levelField = new JTextField(task.getRequirement("level"));
				levelField.addFocusListener(new java.awt.event.FocusAdapter()
				{
					public void focusLost(java.awt.event.FocusEvent evt)
					{
						task.addRequirement("level", levelField.getText());
					}
				});
				levelPanel.add(levelField, BorderLayout.CENTER);

				requirementsPanel.add(levelPanel);
			}
			else if (type == TaskType.CUSTOM)
			{
				// Chat pattern field (for tracking via chat messages)
				JPanel patternPanel = new JPanel(new BorderLayout(5, 0));
				patternPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
				patternPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));

				JLabel patternLabel = new JLabel("Chat:");
				patternLabel.setForeground(Color.LIGHT_GRAY);
				patternLabel.setPreferredSize(new Dimension(60, 20));
				patternLabel.setToolTipText("Text to match in chat (e.g., 'You drink the beer')");
				patternPanel.add(patternLabel, BorderLayout.WEST);

				JTextField patternField = new JTextField(task.getRequirement("chatPattern"));
				patternField.setToolTipText("Text to match in chat messages (e.g., 'You drink the beer')");
				patternField.addFocusListener(new java.awt.event.FocusAdapter()
				{
					public void focusLost(java.awt.event.FocusEvent evt)
					{
						task.addRequirement("chatPattern", patternField.getText());
					}
				});
				patternPanel.add(patternField, BorderLayout.CENTER);

				requirementsPanel.add(patternPanel);
				requirementsPanel.add(Box.createRigidArea(new Dimension(0, 2)));

				// Count field
				JPanel countPanel = new JPanel(new BorderLayout(5, 0));
				countPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
				countPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));

				JLabel countLabel = new JLabel("Count:");
				countLabel.setForeground(Color.LIGHT_GRAY);
				countLabel.setPreferredSize(new Dimension(60, 20));
				countPanel.add(countLabel, BorderLayout.WEST);

				JTextField countField = new JTextField(task.getRequirement("count"));
				countField.addFocusListener(new java.awt.event.FocusAdapter()
				{
					public void focusLost(java.awt.event.FocusEvent evt)
					{
						task.addRequirement("count", countField.getText());
					}
				});
				countPanel.add(countField, BorderLayout.CENTER);

				requirementsPanel.add(countPanel);
			}

			requirementsPanel.revalidate();
			requirementsPanel.repaint();
		}

		public DiaryTask getTask()
		{
			return task;
		}
	}
}
