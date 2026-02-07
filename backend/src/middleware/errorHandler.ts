import { Request, Response, NextFunction } from 'express';
import { ApiResponse } from '../models/types';

/**
 * Global error handler middleware
 */
export function errorHandler(
  err: any,
  req: Request,
  res: Response,
  next: NextFunction
) {
  console.error('Error:', err);

  const response: ApiResponse = {
    success: false,
    error: err.message || 'Internal server error'
  };

  const statusCode = err.statusCode || 500;
  res.status(statusCode).json(response);
}

/**
 * 404 handler for unknown routes
 */
export function notFoundHandler(req: Request, res: Response) {
  const response: ApiResponse = {
    success: false,
    error: `Route not found: ${req.method} ${req.path}`
  };

  res.status(404).json(response);
}
