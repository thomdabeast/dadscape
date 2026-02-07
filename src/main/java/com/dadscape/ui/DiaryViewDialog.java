package com.dadscape.ui;

import com.dadscape.manager.TaskTracker;
import com.dadscape.model.ClanDiary;
import com.dadscape.model.DiaryTask;
import com.dadscape.model.DiaryTier;
import com.dadscape.model.TaskType;
import com.dadscape.model.UserProgress;
import net.runelite.client.ui.ColorScheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;

/**
 * Read-only dialog for viewing diary details and user progress
 */
public class DiaryViewDialog extends JDialog
{
	private static final int BORDER_OFFSET = 10;
	private static final int DIALOG_WIDTH = 600;
	private static final int DIALOG_HEIGHT = 700;

	private final ClanDiary diary;
	private final TaskTracker taskTracker;
	private final String playerName;
	private final UserProgress userProgress;

	public DiaryViewDialog(JFrame parent, ClanDiary diary, TaskTracker taskTracker, String playerName)
	{
		super(parent, diary.getName(), true);
		this.diary = diary;
		this.taskTracker = taskTracker;
		this.playerName = playerName;
		this.userProgress = taskTracker.loadProgress(playerName, diary.getId());

		setLayout(new BorderLayout());
		setPreferredSize(new Dimension(DIALOG_WIDTH, DIALOG_HEIGHT));
		setMinimumSize(new Dimension(DIALOG_WIDTH, 500));

		initComponents();
		pack();
		setLocationRelativeTo(parent);
	}

	private void initComponents()
	{
		// Header with diary info
		JPanel headerPanel = createHeaderPanel();
		add(headerPanel, BorderLayout.NORTH);

		// Scrollable content with tiers and tasks
		JPanel contentPanel = createContentPanel();
		JScrollPane scrollPane = new JScrollPane(contentPanel);
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.getVerticalScrollBar().setUnitIncrement(16);
		add(scrollPane, BorderLayout.CENTER);

		// Footer with close button
		JPanel footerPanel = createFooterPanel();
		add(footerPanel, BorderLayout.SOUTH);

		// Set background colors
		getContentPane().setBackground(ColorScheme.DARK_GRAY_COLOR);
	}

	private JPanel createHeaderPanel()
	{
		JPanel headerPanel = new JPanel();
		headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
		headerPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		headerPanel.setBorder(new EmptyBorder(BORDER_OFFSET, BORDER_OFFSET, BORDER_OFFSET, BORDER_OFFSET));

		// Diary name
		JLabel nameLabel = new JLabel(diary.getName());
		nameLabel.setFont(new Font("Arial", Font.BOLD, 20));
		nameLabel.setForeground(Color.WHITE);
		nameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
		headerPanel.add(nameLabel);

		headerPanel.add(Box.createRigidArea(new Dimension(0, 5)));

		// Category
		JLabel categoryLabel = new JLabel("Category: " + diary.getCategory());
		categoryLabel.setFont(new Font("Arial", Font.PLAIN, 12));
		categoryLabel.setForeground(Color.CYAN);
		categoryLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
		headerPanel.add(categoryLabel);

		// Description (if present)
		if (diary.getDescription() != null && !diary.getDescription().isEmpty())
		{
			headerPanel.add(Box.createRigidArea(new Dimension(0, 5)));
			JTextArea descArea = new JTextArea(diary.getDescription());
			descArea.setEditable(false);
			descArea.setLineWrap(true);
			descArea.setWrapStyleWord(true);
			descArea.setBackground(ColorScheme.DARKER_GRAY_COLOR);
			descArea.setForeground(Color.LIGHT_GRAY);
			descArea.setFont(new Font("Arial", Font.PLAIN, 12));
			descArea.setAlignmentX(Component.CENTER_ALIGNMENT);
			headerPanel.add(descArea);
		}

		headerPanel.add(Box.createRigidArea(new Dimension(0, 10)));

		// Overall progress
		int totalTasks = diary.getTotalTaskCount();
		int completion = userProgress.getCompletionPercentage(totalTasks);

		JLabel progressLabel = new JLabel("Overall Progress: " + completion + "%");
		progressLabel.setFont(new Font("Arial", Font.BOLD, 14));
		progressLabel.setForeground(Color.WHITE);
		progressLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
		headerPanel.add(progressLabel);

		headerPanel.add(Box.createRigidArea(new Dimension(0, 5)));

		JProgressBar overallProgress = new JProgressBar(0, 100);
		overallProgress.setValue(completion);
		overallProgress.setStringPainted(true);
		overallProgress.setForeground(completion == 100 ? Color.GREEN : ColorScheme.GRAND_EXCHANGE_PRICE);
		overallProgress.setMaximumSize(new Dimension(500, 25));
		overallProgress.setAlignmentX(Component.CENTER_ALIGNMENT);
		headerPanel.add(overallProgress);

		return headerPanel;
	}

	private JPanel createContentPanel()
	{
		JPanel contentPanel = new JPanel();
		contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
		contentPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		contentPanel.setBorder(new EmptyBorder(BORDER_OFFSET, BORDER_OFFSET, BORDER_OFFSET, BORDER_OFFSET));

		// Display each tier
		for (DiaryTier tier : diary.getTiers())
		{
			JPanel tierPanel = createTierPanel(tier);
			contentPanel.add(tierPanel);
			contentPanel.add(Box.createRigidArea(new Dimension(0, 10)));
		}

		return contentPanel;
	}

	private JPanel createTierPanel(DiaryTier tier)
	{
		JPanel tierPanel = new JPanel();
		tierPanel.setLayout(new BoxLayout(tierPanel, BoxLayout.Y_AXIS));
		tierPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);

		// Create title with tier name and color
		Color tierColor = Color.WHITE;
		if (tier.getTierColor() != null && !tier.getTierColor().isEmpty())
		{
			try
			{
				tierColor = Color.decode(tier.getTierColor());
			}
			catch (NumberFormatException e)
			{
				// Use default color if invalid
			}
		}

		TitledBorder border = BorderFactory.createTitledBorder(
			BorderFactory.createLineBorder(tierColor, 2),
			tier.getTierName(),
			TitledBorder.LEFT,
			TitledBorder.TOP,
			new Font("Arial", Font.BOLD, 14),
			tierColor
		);
		tierPanel.setBorder(BorderFactory.createCompoundBorder(
			border,
			new EmptyBorder(5, 5, 5, 5)
		));

		// Display tasks
		if (tier.getTasks().isEmpty())
		{
			JLabel emptyLabel = new JLabel("No tasks in this tier");
			emptyLabel.setForeground(Color.LIGHT_GRAY);
			tierPanel.add(emptyLabel);
		}
		else
		{
			for (DiaryTask task : tier.getTasks())
			{
				JPanel taskPanel = createTaskPanel(task);
				tierPanel.add(taskPanel);
				tierPanel.add(Box.createRigidArea(new Dimension(0, 5)));
			}
		}

		// Reward description (if present)
		if (tier.getRewardDescription() != null && !tier.getRewardDescription().isEmpty())
		{
			JLabel rewardLabel = new JLabel("Reward: " + tier.getRewardDescription());
			rewardLabel.setForeground(ColorScheme.GRAND_EXCHANGE_PRICE);
			rewardLabel.setFont(new Font("Arial", Font.ITALIC, 11));
			tierPanel.add(rewardLabel);
		}

		return tierPanel;
	}

	private JPanel createTaskPanel(DiaryTask task)
	{
		JPanel taskPanel = new JPanel();
		taskPanel.setLayout(new BorderLayout(5, 5));
		taskPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);

		// Left: Checkbox showing completion status
		boolean isCompleted = userProgress.isTaskCompleted(task.getId());
		JCheckBox completionCheckbox = new JCheckBox();
		completionCheckbox.setSelected(isCompleted);
		completionCheckbox.setEnabled(false); // Read-only
		completionCheckbox.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		taskPanel.add(completionCheckbox, BorderLayout.WEST);

		// Center: Task info
		JPanel infoPanel = new JPanel();
		infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
		infoPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);

		// Task description with type badge
		JPanel descriptionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
		descriptionPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);

		JLabel typeLabel = new JLabel(task.getType().getDisplayName());
		typeLabel.setForeground(Color.decode(task.getType().getColorHex()));
		typeLabel.setFont(new Font("Arial", Font.BOLD, 10));
		typeLabel.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(Color.decode(task.getType().getColorHex()), 1),
			new EmptyBorder(2, 4, 2, 4)
		));
		descriptionPanel.add(typeLabel);

		JLabel descLabel = new JLabel(task.getDescription());
		descLabel.setForeground(isCompleted ? Color.GREEN : Color.WHITE);
		descLabel.setFont(new Font("Arial", Font.PLAIN, 12));
		if (isCompleted)
		{
			descLabel.setText("<html><strike>" + task.getDescription() + "</strike></html>");
		}
		descriptionPanel.add(descLabel);

		infoPanel.add(descriptionPanel);

		// Progress bar for tasks with progress (like KILL and CUSTOM tasks)
		if ((task.getType() == TaskType.KILL || task.getType() == TaskType.CUSTOM) && !isCompleted)
		{
			int currentProgress = userProgress.getTaskProgress(task.getId());
			String countStr = task.getRequirement("count");
			if (countStr != null)
			{
				try
				{
					int targetCount = Integer.parseInt(countStr);
					String progressText;

					if (task.getType() == TaskType.KILL)
					{
						String npcName = task.getRequirement("npc");
						progressText = currentProgress + " / " + targetCount + " " + npcName + " killed";
					}
					else // CUSTOM
					{
						progressText = currentProgress + " / " + targetCount + " completed";
					}

					JLabel progressLabel = new JLabel(progressText);
					progressLabel.setForeground(Color.LIGHT_GRAY);
					progressLabel.setFont(new Font("Arial", Font.PLAIN, 10));
					infoPanel.add(progressLabel);

					JProgressBar progressBar = new JProgressBar(0, targetCount);
					progressBar.setValue(currentProgress);
					progressBar.setStringPainted(true);
					progressBar.setString(currentProgress + " / " + targetCount);
					progressBar.setMaximumSize(new Dimension(400, 20));
					infoPanel.add(progressBar);
				}
				catch (NumberFormatException e)
				{
					// Skip progress bar if count is invalid
				}
			}
		}

		// Hint (if present)
		if (task.getHint() != null && !task.getHint().isEmpty())
		{
			JLabel hintLabel = new JLabel("💡 " + task.getHint());
			hintLabel.setForeground(Color.YELLOW);
			hintLabel.setFont(new Font("Arial", Font.ITALIC, 10));
			infoPanel.add(hintLabel);
		}

		taskPanel.add(infoPanel, BorderLayout.CENTER);

		return taskPanel;
	}

	private JPanel createFooterPanel()
	{
		JPanel footerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
		footerPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		footerPanel.setBorder(new EmptyBorder(BORDER_OFFSET, BORDER_OFFSET, BORDER_OFFSET, BORDER_OFFSET));

		JButton closeButton = new JButton("Close");
		closeButton.addActionListener(e -> dispose());
		footerPanel.add(closeButton);

		return footerPanel;
	}
}
