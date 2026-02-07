import express from 'express';
import cors from 'cors';
import helmet from 'helmet';
import rateLimit from 'express-rate-limit';
import dotenv from 'dotenv';
import diaryRoutes from './routes/diaryRoutes';
import motdRoutes from './routes/motdRoutes';
import { errorHandler, notFoundHandler } from './middleware/errorHandler';
import { logger } from './middleware/logger';
import { migrate } from './db/migrate';

// Load environment variables
dotenv.config();

const app = express();
const PORT = process.env.PORT || 3000;

// Security middleware
app.use(helmet());

// CORS configuration
app.use(cors({
  origin: process.env.CORS_ORIGIN || '*',
  credentials: true
}));

// Rate limiting
const limiter = rateLimit({
  windowMs: parseInt(process.env.RATE_LIMIT_WINDOW_MS || '900000'), // 15 minutes
  max: parseInt(process.env.RATE_LIMIT_MAX_REQUESTS || '100'), // limit each IP to 100 requests per windowMs
  message: 'Too many requests from this IP, please try again later.'
});
app.use('/api/', limiter);

// Body parsing middleware
app.use(express.json());
app.use(express.urlencoded({ extended: true }));

// Custom logger middleware
app.use(logger);

// Health check endpoint
app.get('/health', (_, res) => {
  res.json({
    success: true,
    message: 'DadScape Diary API is running',
    timestamp: new Date().toISOString()
  });
});

// API routes
app.use('/api/diaries', diaryRoutes);
app.use('/api/motd', motdRoutes);

// 404 handler
app.use(notFoundHandler);

// Error handler (must be last)
app.use(errorHandler);

/**
 * Start the server
 */
async function startServer() {
  try {
    // Run database migrations
    console.log('Running database migrations...');
    await migrate();

    // Start listening
    app.listen(PORT, () => {
      console.log(`\n🚀 DadScape Diary API server running on port ${PORT}`);
      console.log(`📊 Health check: http://localhost:${PORT}/health`);
      console.log(`📖 Diaries API: http://localhost:${PORT}/api/diaries`);
      console.log(`📢 MOTD API: http://localhost:${PORT}/api/motd`);
      console.log(`\nEnvironment: ${process.env.NODE_ENV || 'development'}`);
    });
  } catch (error) {
    console.error('Failed to start server:', error);
    process.exit(1);
  }
}

// Start the server
startServer();

export default app;
