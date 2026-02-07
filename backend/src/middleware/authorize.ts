import { Request, Response, NextFunction } from 'express';
import { get } from '../db/database';

/**
 * Minimum rank required for admin actions
 * Matches RuneLite ClanRank values:
 * - GUEST = -1
 * - FRIEND = 0
 * - RECRUIT = 10
 * - CORPORAL = 20
 * - SERGEANT = 30
 * - LIEUTENANT = 40
 * - CAPTAIN = 50
 * - GENERAL = 60
 * - ADMIN = 100
 * - DEPUTY_OWNER = 125
 * - OWNER = 127
 */
const MIN_ADMIN_RANK = parseInt(process.env.MIN_ADMIN_RANK || '0');

/**
 * Authorization middleware - checks if user has sufficient clan rank for admin actions
 * Requires authentication middleware to run first
 */
export async function requireAdmin(req: Request, res: Response, next: NextFunction) {
  try {
    // Get RSN from request (check body, query, or params)
    const rsn = req.body.rsn || req.query.rsn || req.body.createdBy || req.body.lastModifiedBy;

    if (!rsn) {
      return res.status(400).json({
        success: false,
        error: 'RSN (RuneScape Name) is required for admin actions. Include "rsn" in request body or query params.'
      });
    }

    // Look up clan member rank
    const member = await get<{
      id: number;
      rsn: string;
      rank: number;
      joined_date: number;
      last_seen: number;
    }>('SELECT * FROM clan_members WHERE rsn = ? COLLATE NOCASE', [rsn]);

    // If member not found or rank insufficient, deny access
    if (!member) {
      return res.status(403).json({
        success: false,
        error: `User "${rsn}" is not registered in the clan members table. Contact an administrator.`
      });
    }

    if (member.rank < MIN_ADMIN_RANK) {
      return res.status(403).json({
        success: false,
        error: `Insufficient permissions. User "${rsn}" has rank ${member.rank}, but requires rank ${MIN_ADMIN_RANK} or higher for admin actions.`
      });
    }

    // User has sufficient permissions
    next();
  } catch (error) {
    console.error('Authorization error:', error);
    return res.status(500).json({
      success: false,
      error: 'Authorization check failed'
    });
  }
}

/**
 * Check if user is clan leader (Owner rank)
 */
export async function requireOwner(req: Request, res: Response, next: NextFunction) {
  try {
    const rsn = req.body.rsn || req.query.rsn || req.body.createdBy || req.body.lastModifiedBy;

    if (!rsn) {
      return res.status(400).json({
        success: false,
        error: 'RSN is required for owner-only actions'
      });
    }

    const member = await get<{
      rank: number;
    }>('SELECT rank FROM clan_members WHERE rsn = ? COLLATE NOCASE', [rsn]);

    if (!member || member.rank !== 127) { // 127 = OWNER
      return res.status(403).json({
        success: false,
        error: 'This action requires clan owner permissions'
      });
    }

    next();
  } catch (error) {
    console.error('Owner authorization error:', error);
    return res.status(500).json({
      success: false,
      error: 'Authorization check failed'
    });
  }
}

/**
 * Verify that the RSN in the request matches a clan member (any rank)
 * Used for progress tracking endpoints where any authenticated clan member should be allowed
 */
export async function requireClanMember(req: Request, res: Response, next: NextFunction) {
  try {
    const rsn = req.body.rsn || req.query.rsn || req.params.rsn;

    if (!rsn) {
      return res.status(400).json({
        success: false,
        error: 'RSN is required'
      });
    }

    const member = await get('SELECT id FROM clan_members WHERE rsn = ? COLLATE NOCASE', [rsn]);

    if (!member) {
      return res.status(403).json({
        success: false,
        error: `User "${rsn}" is not a registered clan member`
      });
    }

    next();
  } catch (error) {
    console.error('Clan member check error:', error);
    return res.status(500).json({
      success: false,
      error: 'Membership check failed'
    });
  }
}
