package com.dadscape.ui;

import com.dadscape.DadScapeConfig;
import com.dadscape.manager.DadCredManager;
import com.dadscape.manager.DiaryManager;
import com.dadscape.manager.PermissionManager;
import com.dadscape.manager.TaskTracker;
import com.dadscape.model.ClanDiary;
import com.dadscape.model.DadCred;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;

import javax.inject.Inject;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.List;

/**
 * Main DadScape panel with two sections:
 * 1. DadCred - Shows player's custom ranking
 * 2. Clan Diary - Shows/manages clan achievement diaries
 */
@Slf4j
public class DadScapePanel extends PluginPanel
{
	private static final int BORDER_OFFSET = 6;

	private final DadScapeConfig config;
	private final DiaryManager diaryManager;
	private final PermissionManager permissionManager;
	private final DadCredManager dadCredManager;
	private final TaskTracker taskTracker;

	// DadCred UI Components
	private JLabel dadCredRankLabel;
	private JLabel dadCredScoreLabel;
	private JLabel dadCredLastUpdatedLabel;

	// Diary UI Components
	private JPanel diaryListPanel;

	private JButton createDiaryButton;
	private JButton setMotdButton;
	private JButton refreshButton;

	private JComboBox<String> categoryFilter;

	@Inject
	public DadScapePanel(
		DadScapeConfig config,
		DiaryManager diaryManager,
		PermissionManager permissionManager,
		DadCredManager dadCredManager,
		TaskTracker taskTracker
	)
	{
		this.config = config;
		this.diaryManager = diaryManager;
		this.permissionManager = permissionManager;
		this.dadCredManager = dadCredManager;
		this.taskTracker = taskTracker;

		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setBackground(ColorScheme.DARK_GRAY_COLOR);

		init();
		refreshDadCred();
		refreshPermissionStatus();
		refreshDiaryList();
	}

	/**
	 * Initialize the UI components
	 */
	private void init()
	{
		// Section 1: DadCred
		JPanel dadCredSection = createDadCredSection();
		add(dadCredSection);

		// Spacer
		add(Box.createRigidArea(new Dimension(0, 10)));

		// Section 2: Clan Diary
		JPanel diarySection = createDiarySection();
		add(diarySection);
	}

	/**
	 * Create the DadCred section (always visible)
	 */
	private JPanel createDadCredSection()
	{
		JPanel section = new JPanel(new BorderLayout());
		section.setBackground(ColorScheme.DARKER_GRAY_COLOR);

		TitledBorder border = BorderFactory.createTitledBorder(
			BorderFactory.createEtchedBorder(),
			"DadCred",
			TitledBorder.LEFT,
			TitledBorder.TOP,
			new Font("Arial", Font.BOLD, 14),
			Color.WHITE
		);
		section.setBorder(border);

		// Rank display
		dadCredRankLabel = new JLabel("Rank: Loading...");
		dadCredRankLabel.setForeground(Color.WHITE);
		dadCredRankLabel.setFont(new Font("Arial", Font.BOLD, 16));
		dadCredRankLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
		section.add(dadCredRankLabel);

		section.add(Box.createRigidArea(new Dimension(0, 5)));

		// Score display
		dadCredScoreLabel = new JLabel("Cred: 0");
		dadCredScoreLabel.setForeground(ColorScheme.GRAND_EXCHANGE_PRICE);
		dadCredScoreLabel.setFont(new Font("Arial", Font.PLAIN, 14));
		dadCredScoreLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
		section.add(dadCredScoreLabel);

		section.add(Box.createRigidArea(new Dimension(0, 5)));

		// Last updated
		dadCredLastUpdatedLabel = new JLabel("Last updated: Never");
		dadCredLastUpdatedLabel.setForeground(Color.LIGHT_GRAY);
		dadCredLastUpdatedLabel.setFont(new Font("Arial", Font.PLAIN, 10));
		dadCredLastUpdatedLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
		section.add(dadCredLastUpdatedLabel);

		return section;
	}

	/**
	 * Create the Clan Diary section
	 */
	private JPanel createDiarySection()
	{
		JPanel section = new JPanel(new BorderLayout());
		section.setBackground(ColorScheme.DARKER_GRAY_COLOR);

		TitledBorder border = BorderFactory.createTitledBorder(
			BorderFactory.createEtchedBorder(),
			"Clan Diary",
			TitledBorder.LEFT,
			TitledBorder.TOP,
			new Font("Arial", Font.BOLD, 14),
			Color.WHITE
		);
		section.setBorder(border);

		// Header with rank/permissions
		JPanel headerPanel = createDiaryHeaderPanel();
		section.add(headerPanel, BorderLayout.NORTH);

		// Content with diary list
		JPanel contentPanel = createContentPanel();
		section.add(contentPanel, BorderLayout.CENTER);

		// Footer with action buttons
		JPanel footerPanel = createFooterPanel();
		section.add(footerPanel, BorderLayout.SOUTH);

		return section;
	}

	/**
	 * Create the diary header panel showing clan rank and permissions
	 */
	private JPanel createDiaryHeaderPanel()
	{
		JPanel headerPanel = new JPanel();
		headerPanel.setLayout(new GridLayout(0, 1, 0, 5));
		headerPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		headerPanel.setBorder(new EmptyBorder(BORDER_OFFSET, BORDER_OFFSET, BORDER_OFFSET, BORDER_OFFSET));


		// Category filter
		JLabel filterLabel = new JLabel("Filter by Category:");
		filterLabel.setForeground(Color.LIGHT_GRAY);
		headerPanel.add(filterLabel);

		categoryFilter = new JComboBox<>();
		categoryFilter.addItem("All Categories");
		categoryFilter.addActionListener(e -> refreshDiaryList());
		headerPanel.add(categoryFilter);

		return headerPanel;
	}

	/**
	 * Create the main content panel with scrollable diary list
	 */
	private JPanel createContentPanel()
	{
		JPanel contentWrapper = new JPanel(new BorderLayout());
		contentWrapper.setBackground(ColorScheme.DARK_GRAY_COLOR);

		diaryListPanel = new JPanel();
		diaryListPanel.setLayout(new GridLayout(0, 1, 0, 5));
		diaryListPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		diaryListPanel.setBorder(new EmptyBorder(BORDER_OFFSET, BORDER_OFFSET, BORDER_OFFSET, BORDER_OFFSET));

		JScrollPane scrollPane = new JScrollPane(diaryListPanel);
		scrollPane.setBackground(ColorScheme.DARK_GRAY_COLOR);
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

		contentWrapper.add(scrollPane, BorderLayout.CENTER);

		return contentWrapper;
	}

	/**
	 * Create the footer panel with action buttons
	 */
	private JPanel createFooterPanel()
	{
		JPanel footerPanel = new JPanel();
		footerPanel.setLayout(new GridLayout(0, 1, 0, 5));
		footerPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		footerPanel.setBorder(new EmptyBorder(BORDER_OFFSET, BORDER_OFFSET, BORDER_OFFSET, BORDER_OFFSET));

		// Create new diary button
		createDiaryButton = new JButton("Create New Diary");
		createDiaryButton.addActionListener(e -> createNewDiary());
		createDiaryButton.setVisible(permissionManager.canEditDiary());
		footerPanel.add(createDiaryButton);

		// Set message of the day button (admin only)
		setMotdButton = new JButton("Set Message of the Day");
		setMotdButton.addActionListener(e -> setMessageOfTheDay());
		setMotdButton.setVisible(permissionManager.canEditDiary());
		footerPanel.add(setMotdButton);

		// Refresh button
//		refreshButton = new JButton("Refresh");
//		refreshButton.addActionListener(e -> {
//			refreshPermissionStatus();
//			refreshDiaryList();
//		});
//		footerPanel.add(refreshButton);

		return footerPanel;
	}

	/**
	 * Refresh the DadCred display
	 */
	public void refreshDadCred()
	{
		String playerName = permissionManager.getPlayerName();
		DadCred dadCred = dadCredManager.loadDadCred(playerName);

		if (dadCred != null)
		{
			dadCredRankLabel.setText(dadCred.getRank());
			dadCredRankLabel.setForeground(Color.decode(dadCred.getRankColor()));

			dadCredScoreLabel.setText("Cred: " + dadCred.getCredScore());

			// Format last updated time
			long timeSince = System.currentTimeMillis() - dadCred.getLastUpdated();
			String timeStr = formatTimeSince(timeSince);
			dadCredLastUpdatedLabel.setText("Last updated: " + timeStr);
		}
	}

	/**
	 * Format a time duration into a readable string
	 */
	private String formatTimeSince(long milliseconds)
	{
		long seconds = milliseconds / 1000;
		long minutes = seconds / 60;
		long hours = minutes / 60;
		long days = hours / 24;

		if (days > 0)
		{
			return days + " day" + (days == 1 ? "" : "s") + " ago";
		}
		else if (hours > 0)
		{
			return hours + " hour" + (hours == 1 ? "" : "s") + " ago";
		}
		else if (minutes > 0)
		{
			return minutes + " minute" + (minutes == 1 ? "" : "s") + " ago";
		}
		else
		{
			return "Just now";
		}
	}

	/**
	 * Refresh the permission status display
	 */
	public void refreshPermissionStatus()
	{
		boolean canEdit = permissionManager.canEditDiary();
		if (canEdit)
		{
			createDiaryButton.setEnabled(true);
			createDiaryButton.setVisible(true);
			setMotdButton.setEnabled(true);
			setMotdButton.setVisible(true);
		}
		else
		{
			createDiaryButton.setEnabled(false);
			createDiaryButton.setVisible(false);
			setMotdButton.setEnabled(false);
			setMotdButton.setVisible(false);
		}
	}

	/**
	 * Refresh the diary list display
	 */
	public void refreshDiaryList()
	{
		diaryListPanel.removeAll();

		// Update category filter
		updateCategoryFilter();

		// Get filtered diaries
		List<ClanDiary> diaries;
		String selectedCategory = (String) categoryFilter.getSelectedItem();
		if (selectedCategory == null || selectedCategory.equals("All Categories"))
		{
			diaries = diaryManager.getAllDiaries();
		}
		else
		{
			diaries = diaryManager.getDiariesByCategory(selectedCategory);
		}

		// Display diaries
		if (diaries.isEmpty())
		{
			JLabel emptyLabel = new JLabel("No diaries found. Create one to get started!");
			emptyLabel.setForeground(Color.LIGHT_GRAY);
			emptyLabel.setHorizontalAlignment(SwingConstants.CENTER);
			diaryListPanel.add(emptyLabel);
		}
		else
		{
			log.debug("{} diaries found.", diaries.size());

			for (ClanDiary diary : diaries)
			{
				diaryListPanel.add(createDiaryCard(diary));
			}
		}

		diaryListPanel.revalidate();
		diaryListPanel.repaint();
	}

	/**
	 * Update the category filter dropdown with available categories
	 */
	private void updateCategoryFilter()
	{
		String selectedItem = (String) categoryFilter.getSelectedItem();

		// Temporarily remove all action listeners to prevent triggering refreshDiaryList
		java.awt.event.ActionListener[] listeners = categoryFilter.getActionListeners();
		for (java.awt.event.ActionListener listener : listeners)
		{
			categoryFilter.removeActionListener(listener);
		}

		// Update the items
		categoryFilter.removeAllItems();
		categoryFilter.addItem("All Categories");

		List<String> categories = diaryManager.getAllCategories();
		for (String category : categories)
		{
			categoryFilter.addItem(category);
		}

		// Restore selection if it still exists
		if (selectedItem != null)
		{
			for (int i = 0; i < categoryFilter.getItemCount(); i++)
			{
				if (categoryFilter.getItemAt(i).equals(selectedItem))
				{
					categoryFilter.setSelectedIndex(i);
					break;
				}
			}
		}

		// Re-add all action listeners
		for (java.awt.event.ActionListener listener : listeners)
		{
			categoryFilter.addActionListener(listener);
		}
	}

	/**
	 * Create a card UI element for a single diary
	 */
	private JPanel createDiaryCard(ClanDiary diary)
	{
		JPanel card = new JPanel();
		card.setLayout(new BorderLayout());
		card.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		card.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(ColorScheme.MEDIUM_GRAY_COLOR),
			new EmptyBorder(5, 5, 5, 5)
		));

		// Info panel
		JPanel infoPanel = new JPanel(new GridLayout(0, 1));
		infoPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);

		JLabel nameLabel = new JLabel(diary.getName());
		nameLabel.setForeground(Color.WHITE);
		nameLabel.setFont(new Font("Arial", Font.BOLD, 14));
		infoPanel.add(nameLabel);

		JLabel categoryLabel = new JLabel("Category: " + diary.getCategory());
		categoryLabel.setForeground(Color.CYAN);
		infoPanel.add(categoryLabel);

		// Get user's progress
		String playerName = permissionManager.getPlayerName();
		int completion = taskTracker.getDiaryCompletion(playerName, diary.getId());

		JLabel taskCountLabel = new JLabel(
			diary.getTotalTaskCount() + " tasks across " + diary.getTiers().size() + " tiers • " +
			completion + "% complete"
		);
		taskCountLabel.setForeground(Color.LIGHT_GRAY);
		infoPanel.add(taskCountLabel);

		// Progress bar
		JProgressBar progressBar = new JProgressBar(0, 100);
		progressBar.setValue(completion);
		progressBar.setStringPainted(true);
		progressBar.setForeground(completion == 100 ? Color.GREEN : ColorScheme.GRAND_EXCHANGE_PRICE);
		infoPanel.add(progressBar);

		card.add(infoPanel, BorderLayout.CENTER);

		boolean canEdit = permissionManager.canEditDiary();

		// Button panel - show different buttons based on permissions
		JPanel buttonPanel = new JPanel(new GridLayout(canEdit ? 3 : 1, 1, 5, 0));
		buttonPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);

		if (canEdit)
		{
			// Admin buttons
			JButton editButton = new JButton("Edit");
			editButton.addActionListener(e -> editDiary(diary));
			buttonPanel.add(editButton);

			JButton deleteButton = new JButton("Delete");
			deleteButton.addActionListener(e -> deleteDiary(diary));
			buttonPanel.add(deleteButton);
		}

		// User button - view details
		JButton viewButton = new JButton("View Details");
		viewButton.addActionListener(e -> viewDiary(diary));
		buttonPanel.add(viewButton);

		card.add(buttonPanel, BorderLayout.SOUTH);

		return card;
	}

	/**
	 * Create a new diary
	 */
	private void createNewDiary()
	{
		if (!permissionManager.canEditDiary())
		{
			JOptionPane.showMessageDialog(this,
				"You do not have permission to create diaries.\nRequired rank: " + permissionManager.getMinEditRank(),
				"Permission Denied",
				JOptionPane.ERROR_MESSAGE);
			return;
		}

		// Prompt for diary name
		String name = JOptionPane.showInputDialog(this, "Enter diary name:", "Create Diary", JOptionPane.PLAIN_MESSAGE);
		if (name == null || name.trim().isEmpty())
		{
			return;
		}

		// Prompt for category
		String category = JOptionPane.showInputDialog(this, "Enter category (e.g., PvM, Skilling, Collection Log):", "Create Diary", JOptionPane.PLAIN_MESSAGE);
		if (category == null || category.trim().isEmpty())
		{
			category = "General";
		}

		// Create the diary
		String creatorName = permissionManager.getPlayerName();
		ClanDiary diary = diaryManager.createDiary(name.trim(), category.trim(), creatorName);

		JOptionPane.showMessageDialog(this,
			"Diary '" + diary.getName() + "' created successfully!",
			"Success",
			JOptionPane.INFORMATION_MESSAGE);

		refreshDiaryList();
	}

	/**
	 * Edit an existing diary
	 */
	private void editDiary(ClanDiary diary)
	{
		if (!permissionManager.canEditDiary())
		{
			JOptionPane.showMessageDialog(this,
				"You do not have permission to edit diaries.",
				"Permission Denied",
				JOptionPane.ERROR_MESSAGE);
			return;
		}

		// Open diary editor dialog
		String playerName = permissionManager.getPlayerName();
		DiaryEditorDialog editor = new DiaryEditorDialog(
			(JFrame) SwingUtilities.getWindowAncestor(this),
			diaryManager,
			diary,
			playerName,
			this::refreshDiaryList
		);
		editor.setVisible(true);
	}

	/**
	 * Delete a diary
	 */
	private void deleteDiary(ClanDiary diary)
	{
		int confirm = JOptionPane.showConfirmDialog(this,
			"Are you sure you want to delete '" + diary.getName() + "'?\nThis cannot be undone.",
			"Confirm Delete",
			JOptionPane.YES_NO_OPTION,
			JOptionPane.WARNING_MESSAGE);

		if (confirm == JOptionPane.YES_OPTION)
		{
			diaryManager.deleteDiary(diary.getId());
			JOptionPane.showMessageDialog(this,
				"Diary deleted successfully.",
				"Deleted",
				JOptionPane.INFORMATION_MESSAGE);
			refreshDiaryList();
		}
	}

	/**
	 * View a diary (read-only for non-admins)
	 */
	private void viewDiary(ClanDiary diary)
	{
		DiaryViewDialog viewDialog = new DiaryViewDialog(
			(JFrame) SwingUtilities.getWindowAncestor(this),
			diary,
			taskTracker,
			permissionManager.getPlayerName()
		);
		viewDialog.setVisible(true);
	}

	/**
	 * Set the message of the day (admin only)
	 */
	private void setMessageOfTheDay()
	{
		if (!permissionManager.canEditDiary())
		{
			JOptionPane.showMessageDialog(this,
				"You do not have permission to set the message of the day.",
				"Permission Denied",
				JOptionPane.ERROR_MESSAGE);
			return;
		}

		// Get current MOTD
		String currentMotd = diaryManager.getMessageOfTheDay();

		// Show dialog with text area for multi-line input
		JTextArea textArea = new JTextArea(currentMotd, 5, 40);
		textArea.setLineWrap(true);
		textArea.setWrapStyleWord(true);
		JScrollPane scrollPane = new JScrollPane(textArea);

		int result = JOptionPane.showConfirmDialog(this,
			scrollPane,
			"Set Message of the Day",
			JOptionPane.OK_CANCEL_OPTION,
			JOptionPane.PLAIN_MESSAGE);

		if (result == JOptionPane.OK_OPTION)
		{
			String newMotd = textArea.getText();
			diaryManager.setMessageOfTheDay(newMotd);

			JOptionPane.showMessageDialog(this,
				"Message of the day updated successfully!\nPlayers will see this when they log in.",
				"Success",
				JOptionPane.INFORMATION_MESSAGE);
		}
	}
}
