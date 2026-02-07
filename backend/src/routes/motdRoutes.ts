import { Router } from 'express';
import * as motdController from '../controllers/motdController';
import { authenticate } from '../middleware/auth';
import { requireAdmin } from '../middleware/authorize';

const router = Router();

/**
 * GET /api/motd
 * Get the message of the day
 * Auth: Required (any authenticated user)
 */
router.get('/', authenticate, motdController.getMotd);

/**
 * POST /api/motd
 * Set the message of the day
 * Body: { motd: string, rsn: string }
 * Auth: Required + Admin rank
 */
router.post('/', authenticate, requireAdmin, motdController.setMotd);

/**
 * PUT /api/motd (alias for POST)
 * Set the message of the day
 * Body: { motd: string, rsn: string }
 * Auth: Required + Admin rank
 */
router.put('/', authenticate, requireAdmin, motdController.setMotd);

export default router;
