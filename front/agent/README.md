# Legal/Policy Compliance Checker Frontend

A modern, responsive single-page application built with Next.js for legal and policy compliance checking. This frontend integrates with a Spring Boot backend to provide AI-powered document analysis and compliance querying.

## Features

- **Document Upload**: Upload PDF and DOCX policy documents with progress tracking
- **Compliance Query**: Ask questions about uploaded documents and get AI-powered answers
- **Statistics Dashboard**: View vector store statistics and document metrics
- **Health Monitor**: Real-time backend health monitoring
- **Responsive Design**: Works on desktop, tablet, and mobile devices
- **Dark Mode Support**: Automatic dark/light theme switching
- **Real-time Notifications**: Toast notifications for user feedback

## Technology Stack

- **Frontend**: Next.js 15, React 19, Tailwind CSS
- **HTTP Client**: Axios
- **Icons**: Lucide React
- **Notifications**: React Hot Toast
- **Styling**: Tailwind CSS with custom components

## Prerequisites

- Node.js 18+ and npm
- Spring Boot backend running on port 8080 (or configure different URL)

## Installation

1. **Clone and navigate to the project:**
   ```bash
   cd agent
   ```

2. **Install dependencies:**
   ```bash
   npm install
   ```

3. **Configure environment variables:**
   
   Create a `.env.local` file with:
   ```env
   NEXT_PUBLIC_API_URL=http://localhost:8080
   NEXT_PUBLIC_APP_NAME="Legal Compliance Checker"
   NEXT_PUBLIC_APP_VERSION="1.0.0"
   ```

4. **Start the development server:**
   ```bash
   npm run dev
   ```

5. **Open your browser:**
   Navigate to `http://localhost:3000`

## API Integration

The frontend integrates with the following Spring Boot endpoints:

### Upload Endpoint
- **POST** `/api/compliance/upload`
- Accepts: `multipart/form-data` with PDF/DOCX files
- Response: Upload confirmation and metadata

### Query Endpoint
- **POST** `/api/compliance/query`
- Body: `{ "query": "Your compliance question" }`
- Response: AI-generated answer with source references

### Health Check Endpoint
- **GET** `/api/compliance/health`
- Response: System health status and metrics

### Statistics Endpoint
- **GET** `/api/compliance/stats`
- Response: Vector store statistics and document counts

## Components

### UploadForm
- Multiple file selection with drag-and-drop support
- Upload progress tracking
- File type validation (PDF, DOCX only)
- Success/error feedback

### QueryForm
- Natural language query input
- Response display with source references
- Query history tracking
- Confidence scoring

### StatsDisplay
- Vector store statistics
- Document count and storage metrics
- Auto-refresh capabilities
- Visual metric cards

### HealthDisplay
- Real-time backend health monitoring
- Service status indicators
- System metrics (uptime, memory, disk)
- Connection status tracking

## Usage Guide

### 1. Upload Documents
- Navigate to the "Upload Documents" tab
- Drag and drop PDF or DOCX files, or click to browse
- Monitor upload progress
- View success confirmation

### 2. Query Compliance
- Switch to "Query Compliance" tab
- Type your compliance question (e.g., "What are the data retention requirements?")
- View AI-generated responses with source references
- Check query history for previous questions

### 3. View Statistics
- Access "Statistics" tab for system overview
- Monitor document counts and vector store metrics
- Enable auto-refresh for real-time updates

### 4. Check System Health
- Use "System Health" tab to monitor backend status
- View service availability and system metrics
- Set up auto-health checking

## Configuration Options

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `NEXT_PUBLIC_API_URL` | Backend API base URL | `http://localhost:8080` |
| `NEXT_PUBLIC_APP_NAME` | Application display name | `Legal Compliance Checker` |
| `NEXT_PUBLIC_APP_VERSION` | Application version | `1.0.0` |

### API Timeout Settings

The API client is configured with:
- **Timeout**: 30 seconds (suitable for file uploads)
- **Retry Logic**: Built into component error handling
- **Progress Tracking**: Real-time upload progress

## Responsive Design

The application is fully responsive:
- **Desktop**: Full tab navigation and expanded layouts
- **Tablet**: Optimized layouts with responsive grids
- **Mobile**: Bottom tab navigation and compact components

## Browser Support

- Chrome/Edge 90+
- Firefox 88+
- Safari 14+
- Mobile browsers (iOS Safari, Android Chrome)

## Development

### Build Commands

```bash
# Development server
npm run dev

# Production build
npm run build

# Start production server
npm start
```

### File Structure

```
app/
├── page.js              # Main application page
├── layout.js            # Root layout
├── globals.css          # Global styles
components/
├── UploadForm.js        # Document upload component
├── QueryForm.js         # Compliance query component
├── StatsDisplay.js      # Statistics dashboard
└── HealthDisplay.js     # Health monitoring
lib/
└── api.js               # API utility functions
```

## Error Handling

The application includes comprehensive error handling:
- **Network Errors**: Connection failure notifications
- **API Errors**: Server response error messages
- **Validation Errors**: Client-side input validation
- **File Upload Errors**: Size and type validation

## Security Considerations

- **File Type Validation**: Only PDF and DOCX files accepted
- **File Size Limits**: Configurable upload size restrictions
- **CORS Handling**: Proper cross-origin request configuration
- **Environment Variables**: Sensitive data in environment files

## Performance Features

- **Lazy Loading**: Components loaded on demand
- **Optimized Builds**: Next.js automatic optimization
- **Caching**: HTTP response caching where appropriate
- **Bundle Splitting**: Automatic code splitting

## Troubleshooting

### Backend Connection Issues
- Verify Spring Boot server is running on configured port
- Check `NEXT_PUBLIC_API_URL` environment variable
- Ensure CORS is properly configured on backend

### File Upload Issues
- Check file type (must be PDF or DOCX)
- Verify file size limits
- Ensure backend upload endpoint is accessible

### Styling Issues
- Verify Tailwind CSS is properly configured
- Check for conflicting CSS rules
- Ensure dark mode variables are set

## License

This project is part of the SustainEarth repository and follows the same licensing terms.

## Support

For issues and questions:
1. Check the troubleshooting section above
2. Verify backend API connectivity
3. Review browser console for error messages
4. Ensure all dependencies are properly installed
