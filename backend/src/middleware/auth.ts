import { Request, Response, NextFunction } from 'express';
import { get, run } from '../db/database';

/**
 * Extend Express Request type to include custom properties
 */
declare global {
  namespace Express {
    interface Request {
      apiKey?: string;
      createdBy?: string;
    }
  }
}

/**
 * Authentication middleware - validates API key
 * Checks for Bearer token in Authorization header and validates against api_keys table
 */
export async function authenticate(req: Request, res: Response, next: NextFunction) {
  try {
    const authHeader = req.headers.authorization;

    if (!authHeader || !authHeader.startsWith('Bearer ')) {
      return res.status(401).json({
        success: false,
        error: 'Missing or invalid authorization header. Expected: Authorization: Bearer <api-key>'
      });
    }

    const apiKey = authHeader.substring(7); // Remove "Bearer " prefix

    if (!apiKey || apiKey.trim().length === 0) {
      return res.status(401).json({
        success: false,
        error: 'API key is empty'
      });
    }

    // Validate API key in database
    const keyRecord = await get<{
      id: number;
      key: string;
      created_by: string;
      active: number;
      description: string;
    }>('SELECT * FROM api_keys WHERE key = ? AND active = 1', [apiKey]);

    if (!keyRecord) {
      return res.status(401).json({
        success: false,
        error: 'Invalid or inactive API key'
      });
    }

    // Update last_used timestamp
    await run('UPDATE api_keys SET last_used = CURRENT_TIMESTAMP WHERE id = ?', [keyRecord.id]);

    // Attach API key info to request
    req.apiKey = keyRecord.key;
    req.createdBy = keyRecord.created_by;

    next();
  } catch (error) {
    console.error('Authentication error:', error);
    return res.status(500).json({
      success: false,
      error: 'Authentication failed'
    });
  }
}

/**
 * Optional middleware - doesn't fail if authentication fails, but attaches user info if present
 * Useful for endpoints that work with or without authentication
 */
export async function optionalAuthenticate(req: Request, res: Response, next: NextFunction) {
  try {
    const authHeader = req.headers.authorization;

    if (authHeader && authHeader.startsWith('Bearer ')) {
      const apiKey = authHeader.substring(7);

      if (apiKey && apiKey.trim().length > 0) {
        const keyRecord = await get<{
          id: number;
          key: string;
          created_by: string;
          active: number;
        }>('SELECT * FROM api_keys WHERE key = ? AND active = 1', [apiKey]);

        if (keyRecord) {
          await run('UPDATE api_keys SET last_used = CURRENT_TIMESTAMP WHERE id = ?', [keyRecord.id]);
          req.apiKey = keyRecord.key;
          req.createdBy = keyRecord.created_by;
        }
      }
    }

    next();
  } catch (error) {
    console.error('Optional authentication error:', error);
    // Don't fail - just continue without authentication
    next();
  }
}
