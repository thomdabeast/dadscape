import { Router } from 'express';
import * as diaryController from '../controllers/diaryController';
import { authenticate } from '../middleware/auth';
import { requireAdmin } from '../middleware/authorize';

const router = Router();

/**
 * GET /api/diaries
 * Get all diaries (with optional filters)
 * Query params: ?category=PvM&active=true
 * Auth: Required (any authenticated user)
 */
router.get('/', authenticate, diaryController.getAllDiaries);

/**
 * GET /api/diaries/categories
 * Get all unique categories
 * Auth: Required (any authenticated user)
 */
router.get('/categories', authenticate, diaryController.getCategories);

/**
 * GET /api/diaries/:id
 * Get a single diary by ID
 * Auth: Required (any authenticated user)
 */
router.get('/:id', authenticate, diaryController.getDiaryById);

/**
 * POST /api/diaries
 * Create a new diary
 * Body: { name, category, createdBy, description?, rsn }
 * Auth: Required + Admin rank
 */
router.post('/', authenticate, requireAdmin, diaryController.createDiary);

/**
 * PUT /api/diaries/:id
 * Update an existing diary
 * Body: { name?, description?, category?, version?, tiers?, active?, lastModifiedBy, rsn }
 * Auth: Required + Admin rank
 */
router.put('/:id', authenticate, requireAdmin, diaryController.updateDiary);

/**
 * DELETE /api/diaries/:id
 * Delete a diary
 * Query params: ?rsn=<username>
 * Auth: Required + Admin rank
 */
router.delete('/:id', authenticate, requireAdmin, diaryController.deleteDiary);

export default router;
