import { Request, Response } from 'express';
import { get, all, run } from '../db/database';
import { ClanDiary, DiaryCreateRequest, DiaryUpdateRequest, ApiResponse } from '../models/types';
import { v4 as uuidv4 } from 'uuid';

/**
 * Get all diaries, optionally filtered by category or active status
 */
export async function getAllDiaries(req: Request, res: Response) {
  try {
    const { category, active } = req.query;

    let sql = 'SELECT * FROM diaries WHERE 1=1';
    const params: any[] = [];

    if (category) {
      sql += ' AND category = ?';
      params.push(category);
    }

    if (active !== undefined) {
      sql += ' AND active = ?';
      params.push(active === 'true' ? 1 : 0);
    }

    sql += ' ORDER BY created_date DESC';

    const rows = await all(sql, params);

    // Parse tiers_json for each diary
    const diaries = rows.map((row: any) => ({
      id: row.id,
      name: row.name,
      description: row.description,
      category: row.category,
      version: row.version,
      createdDate: row.created_date,
      createdBy: row.created_by,
      lastModified: row.last_modified,
      lastModifiedBy: row.last_modified_by,
      tiers: JSON.parse(row.tiers_json),
      active: row.active === 1
    }));

    const response: ApiResponse<ClanDiary[]> = {
      success: true,
      data: diaries
    };

    res.json(response);
  } catch (error) {
    console.error('Error fetching diaries:', error);
    const response: ApiResponse = {
      success: false,
      error: 'Failed to fetch diaries'
    };
    res.status(500).json(response);
  }
}

/**
 * Get a single diary by ID
 */
export async function getDiaryById(req: Request, res: Response) {
  try {
    const { id } = req.params;

    const row = await get('SELECT * FROM diaries WHERE id = ?', [id]);

    if (!row) {
      const response: ApiResponse = {
        success: false,
        error: 'Diary not found'
      };
      return res.status(404).json(response);
    }

    const diary: ClanDiary = {
      id: (row as any).id,
      name: (row as any).name,
      description: (row as any).description,
      category: (row as any).category,
      version: (row as any).version,
      createdDate: (row as any).created_date,
      createdBy: (row as any).created_by,
      lastModified: (row as any).last_modified,
      lastModifiedBy: (row as any).last_modified_by,
      tiers: JSON.parse((row as any).tiers_json),
      active: (row as any).active === 1
    };

    const response: ApiResponse<ClanDiary> = {
      success: true,
      data: diary
    };

    res.json(response);
  } catch (error) {
    console.error('Error fetching diary:', error);
    const response: ApiResponse = {
      success: false,
      error: 'Failed to fetch diary'
    };
    res.status(500).json(response);
  }
}

/**
 * Create a new diary
 */
export async function createDiary(req: Request, res: Response) {
  try {
    const data: DiaryCreateRequest = req.body;

    // Validate required fields
    if (!data.name || !data.category || !data.createdBy) {
      const response: ApiResponse = {
        success: false,
        error: 'Missing required fields: name, category, createdBy'
      };
      return res.status(400).json(response);
    }

    const now = Date.now();
    const diary: ClanDiary = {
      id: uuidv4(),
      name: data.name,
      description: data.description || '',
      category: data.category,
      version: '1.0',
      createdDate: now,
      createdBy: data.createdBy,
      lastModified: now,
      lastModifiedBy: data.createdBy,
      tiers: [],
      active: true
    };

    await run(
      `INSERT INTO diaries (
        id, name, description, category, version,
        created_date, created_by, last_modified, last_modified_by,
        active, tiers_json
      ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`,
      [
        diary.id,
        diary.name,
        diary.description,
        diary.category,
        diary.version,
        diary.createdDate,
        diary.createdBy,
        diary.lastModified,
        diary.lastModifiedBy,
        diary.active ? 1 : 0,
        JSON.stringify(diary.tiers)
      ]
    );

    const response: ApiResponse<ClanDiary> = {
      success: true,
      data: diary,
      message: 'Diary created successfully'
    };

    res.status(201).json(response);
  } catch (error) {
    console.error('Error creating diary:', error);
    const response: ApiResponse = {
      success: false,
      error: 'Failed to create diary'
    };
    res.status(500).json(response);
  }
}

/**
 * Update an existing diary
 */
export async function updateDiary(req: Request, res: Response) {
  try {
    const { id } = req.params;
    const data: DiaryUpdateRequest = req.body;

    // Check if diary exists
    const existing = await get('SELECT * FROM diaries WHERE id = ?', [id]);
    if (!existing) {
      const response: ApiResponse = {
        success: false,
        error: 'Diary not found'
      };
      return res.status(404).json(response);
    }

    // Build update query dynamically
    const updates: string[] = [];
    const params: any[] = [];

    if (data.name !== undefined) {
      updates.push('name = ?');
      params.push(data.name);
    }
    if (data.description !== undefined) {
      updates.push('description = ?');
      params.push(data.description);
    }
    if (data.category !== undefined) {
      updates.push('category = ?');
      params.push(data.category);
    }
    if (data.version !== undefined) {
      updates.push('version = ?');
      params.push(data.version);
    }
    if (data.tiers !== undefined) {
      updates.push('tiers_json = ?');
      params.push(JSON.stringify(data.tiers));
    }
    if (data.active !== undefined) {
      updates.push('active = ?');
      params.push(data.active ? 1 : 0);
    }

    // Always update last_modified
    updates.push('last_modified = ?');
    params.push(Date.now());

    updates.push('last_modified_by = ?');
    params.push(data.lastModifiedBy);

    updates.push('updated_at = CURRENT_TIMESTAMP');

    // Add id to params for WHERE clause
    params.push(id);

    await run(
      `UPDATE diaries SET ${updates.join(', ')} WHERE id = ?`,
      params
    );

    // Fetch updated diary
    const updated = await get('SELECT * FROM diaries WHERE id = ?', [id]);
    const diary: ClanDiary = {
      id: (updated as any).id,
      name: (updated as any).name,
      description: (updated as any).description,
      category: (updated as any).category,
      version: (updated as any).version,
      createdDate: (updated as any).created_date,
      createdBy: (updated as any).created_by,
      lastModified: (updated as any).last_modified,
      lastModifiedBy: (updated as any).last_modified_by,
      tiers: JSON.parse((updated as any).tiers_json),
      active: (updated as any).active === 1
    };

    const response: ApiResponse<ClanDiary> = {
      success: true,
      data: diary,
      message: 'Diary updated successfully'
    };

    res.json(response);
  } catch (error) {
    console.error('Error updating diary:', error);
    const response: ApiResponse = {
      success: false,
      error: 'Failed to update diary'
    };
    res.status(500).json(response);
  }
}

/**
 * Delete a diary
 */
export async function deleteDiary(req: Request, res: Response) {
  try {
    const { id } = req.params;

    // Check if diary exists
    const existing = await get('SELECT * FROM diaries WHERE id = ?', [id]);
    if (!existing) {
      const response: ApiResponse = {
        success: false,
        error: 'Diary not found'
      };
      return res.status(404).json(response);
    }

    await run('DELETE FROM diaries WHERE id = ?', [id]);

    const response: ApiResponse = {
      success: true,
      message: 'Diary deleted successfully'
    };

    res.json(response);
  } catch (error) {
    console.error('Error deleting diary:', error);
    const response: ApiResponse = {
      success: false,
      error: 'Failed to delete diary'
    };
    res.status(500).json(response);
  }
}

/**
 * Get all unique categories
 */
export async function getCategories(req: Request, res: Response) {
  try {
    const rows = await all('SELECT DISTINCT category FROM diaries ORDER BY category');
    const categories = rows.map((row: any) => row.category);

    const response: ApiResponse<string[]> = {
      success: true,
      data: categories
    };

    res.json(response);
  } catch (error) {
    console.error('Error fetching categories:', error);
    const response: ApiResponse = {
      success: false,
      error: 'Failed to fetch categories'
    };
    res.status(500).json(response);
  }
}
