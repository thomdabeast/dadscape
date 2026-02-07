package com.dadscape.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a player's DadCred ranking - a custom ranking system
 * outside of RuneScape that tracks player reputation and activity.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DadCred
{
	/**
	 * Player's RuneScape name
	 */
	private String rsn;

	/**
	 * DadCred rank/title (e.g., "Dad Legend", "Super Dad", "New Dad")
	 */
	private String rank;

	/**
	 * Numerical cred score
	 */
	private int credScore;

	/**
	 * Rank color for display (hex)
	 */
	private String rankColor;

	/**
	 * Last time cred was updated
	 */
	private long lastUpdated;

	/**
	 * Create a default/unranked DadCred
	 */
	public static DadCred createUnranked(String rsn)
	{
		DadCred dadCred = new DadCred();
		dadCred.setRsn(rsn);
		dadCred.setRank("New Dad");
		dadCred.setCredScore(0);
		dadCred.setRankColor("#AAAAAA");
		dadCred.setLastUpdated(System.currentTimeMillis());
		return dadCred;
	}

	/**
	 * Get a display string for this cred
	 */
	public String getDisplayString()
	{
		return rank + " (" + credScore + " cred)";
	}
}
