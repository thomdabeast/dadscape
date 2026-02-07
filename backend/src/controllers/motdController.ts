import { Request, Response } from 'express';
import { get, run } from '../db/database';
import { ApiResponse } from '../models/types';

/**
 * Get the message of the day
 */
export async function getMotd(req: Request, res: Response) {
  try {
    const config = await get<{ value: string }>(
      'SELECT value FROM config WHERE key = ?',
      ['motd']
    );

    const motd = config?.value || '';

    const response: ApiResponse<string> = {
      success: true,
      data: motd
    };

    res.json(response);
  } catch (error) {
    console.error('Error fetching MOTD:', error);
    const response: ApiResponse = {
      success: false,
      error: 'Failed to fetch message of the day'
    };
    res.status(500).json(response);
  }
}

/**
 * Set the message of the day (admin only)
 */
export async function setMotd(req: Request, res: Response) {
  try {
    const { motd, rsn } = req.body;

    if (motd === undefined) {
      const response: ApiResponse = {
        success: false,
        error: 'MOTD text is required in request body'
      };
      return res.status(400).json(response);
    }

    // Validate motd length (max 500 characters)
    const motdText = motd || '';
    if (motdText.length > 500) {
      const response: ApiResponse = {
        success: false,
        error: 'MOTD must be 500 characters or less'
      };
      return res.status(400).json(response);
    }

    // Check if MOTD exists, update or insert
    const existing = await get('SELECT key FROM config WHERE key = ?', ['motd']);

    if (existing) {
      await run(
        'UPDATE config SET value = ?, updated_at = CURRENT_TIMESTAMP, updated_by = ? WHERE key = ?',
        [motdText, rsn || 'unknown', 'motd']
      );
    } else {
      await run(
        'INSERT INTO config (key, value, updated_by) VALUES (?, ?, ?)',
        ['motd', motdText, rsn || 'unknown']
      );
    }

    const response: ApiResponse<string> = {
      success: true,
      data: motdText,
      message: 'Message of the day updated successfully'
    };

    res.json(response);
  } catch (error) {
    console.error('Error setting MOTD:', error);
    const response: ApiResponse = {
      success: false,
      error: 'Failed to update message of the day'
    };
    res.status(500).json(response);
  }
}
