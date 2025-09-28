'use client';

import { useState, useRef } from 'react';
import { Send, Paperclip, X, File, Loader2 } from 'lucide-react';

const ChatInput = ({ onSendMessage, onFileUpload, isLoading = false, disabled = false }) => {
  const [message, setMessage] = useState('');
  const [selectedFiles, setSelectedFiles] = useState([]);
  const [dragOver, setDragOver] = useState(false);
  const fileInputRef = useRef(null);
  const textareaRef = useRef(null);

  const handleSubmit = (e) => {
    e.preventDefault();
    
    if (selectedFiles.length > 0) {
      // Handle file upload
      onFileUpload(selectedFiles);
      setSelectedFiles([]);
    } else if (message.trim()) {
      // Handle text message
      onSendMessage(message.trim());
      setMessage('');
    }
    
    // Reset textarea height
    if (textareaRef.current) {
      textareaRef.current.style.height = 'auto';
    }
  };

  const handleKeyPress = (e) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSubmit(e);
    }
  };

  const handleTextareaChange = (e) => {
    setMessage(e.target.value);
    
    // Auto-resize textarea
    if (textareaRef.current) {
      textareaRef.current.style.height = 'auto';
      textareaRef.current.style.height = `${Math.min(textareaRef.current.scrollHeight, 120)}px`;
    }
  };

  const handleFileSelect = (files) => {
    const fileArray = Array.from(files);
    const validFiles = fileArray.filter(file => 
      file.type === 'application/pdf' || 
      file.type === 'application/vnd.openxmlformats-officedocument.wordprocessingml.document' ||
      file.type === 'application/msword'
    );

    if (validFiles.length !== fileArray.length) {
      // Could show a toast here for invalid files
      console.warn('Some files were filtered out - only PDF and DOCX files are allowed');
    }

    setSelectedFiles(prev => [...prev, ...validFiles]);
  };

  const handleFileInputChange = (e) => {
    handleFileSelect(e.target.files);
    // Reset input
    if (fileInputRef.current) {
      fileInputRef.current.value = '';
    }
  };

  const handleDrop = (e) => {
    e.preventDefault();
    setDragOver(false);
    handleFileSelect(e.dataTransfer.files);
  };

  const handleDragOver = (e) => {
    e.preventDefault();
    setDragOver(true);
  };

  const handleDragLeave = (e) => {
    e.preventDefault();
    setDragOver(false);
  };

  const removeFile = (index) => {
    setSelectedFiles(files => files.filter((_, i) => i !== index));
  };

  const formatFileSize = (bytes) => {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
  };

  const canSend = (message.trim() || selectedFiles.length > 0) && !isLoading && !disabled;

  return (
    <div 
      className="p-6"
      style={{ 
        borderTop: '1px solid var(--border)', 
        backgroundColor: 'var(--card)' 
      }}
    >
      {/* Selected Files Display */}
      {selectedFiles.length > 0 && (
        <div className="mb-4 space-y-3">
          <div className="ms-text-caption" style={{ color: 'var(--muted-foreground)' }}>
            Files to upload ({selectedFiles.length}):
          </div>
          <div className="flex flex-wrap gap-3">
            {selectedFiles.map((file, index) => (
              <div
                key={index}
                className="ms-card flex items-center space-x-3 px-4 py-3"
                style={{
                  backgroundColor: 'var(--accent)',
                  borderColor: 'var(--primary)',
                  borderRadius: '4px'
                }}
              >
                <File className="w-4 h-4" style={{ color: 'var(--primary)' }} />
                <div className="min-w-0 flex-1">
                  <p className="ms-text-body truncate" style={{ color: 'var(--card-foreground)' }}>
                    {file.name}
                  </p>
                  <p className="ms-text-caption" style={{ color: 'var(--muted-foreground)' }}>
                    {formatFileSize(file.size)}
                  </p>
                </div>
                <button
                  onClick={() => removeFile(index)}
                  disabled={isLoading}
                  className="hover:bg-black hover:bg-opacity-5 p-1 rounded-sm disabled:opacity-50"
                  style={{ color: 'var(--muted-foreground)' }}
                >
                  <X className="w-4 h-4" />
                </button>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Chat Input Form */}
      <form onSubmit={handleSubmit} className="flex items-end space-x-4">
        {/* File Upload Button */}
        <button
          type="button"
          onClick={() => fileInputRef.current?.click()}
          disabled={isLoading || disabled}
          className="flex-shrink-0 p-3 hover:bg-black hover:bg-opacity-5 rounded-sm disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
          style={{ color: 'var(--muted-foreground)' }}
          title="Upload documents"
        >
          <Paperclip className="w-5 h-5" />
        </button>

        <input
          ref={fileInputRef}
          type="file"
          multiple
          accept=".pdf,.docx,.doc"
          onChange={handleFileInputChange}
          className="hidden"
        />

        {/* Message Input */}
        <div 
          className="flex-1 relative transition-all duration-200"
          style={{
            border: `1px solid ${dragOver ? 'var(--primary)' : 'var(--border)'}`,
            backgroundColor: dragOver ? 'var(--accent)' : 'var(--input)',
            borderRadius: '4px'
          }}
          onDrop={handleDrop}
          onDragOver={handleDragOver}
          onDragLeave={handleDragLeave}
        >
          <textarea
            ref={textareaRef}
            value={message}
            onChange={handleTextareaChange}
            onKeyPress={handleKeyPress}
            placeholder={
              selectedFiles.length > 0 
                ? "Add a compliance-related message (optional) or press Enter to upload..." 
                : "Ask about compliance, regulations, policies, legal requirements, or upload documents..."
            }
            disabled={isLoading || disabled}
            className="w-full px-4 py-3 bg-transparent border-none outline-none resize-none max-h-[120px] ms-text-body"
            style={{ 
              color: 'var(--card-foreground)',
              '::placeholder': { color: 'var(--muted-foreground)' }
            }}
            rows={1}
          />
          
          {dragOver && (
            <div 
              className="absolute inset-0 flex items-center justify-center border-2 border-dashed"
              style={{
                backgroundColor: 'var(--accent)',
                borderColor: 'var(--primary)',
                borderRadius: '4px'
              }}
            >
              <p className="ms-text-body" style={{ color: 'var(--primary)' }}>
                Drop files here to upload
              </p>
            </div>
          )}
        </div>

        {/* Send Button */}
        <button
          type="submit"
          disabled={!canSend}
          className="ms-button flex-shrink-0 p-3 rounded-sm disabled:opacity-50 disabled:cursor-not-allowed transition-all duration-150"
          style={{
            backgroundColor: canSend ? 'var(--primary)' : 'var(--muted)',
            color: canSend ? 'var(--primary-foreground)' : 'var(--muted-foreground)'
          }}
        >
          {isLoading ? (
            <Loader2 className="w-5 h-5 animate-spin" />
          ) : (
            <Send className="w-5 h-5" />
          )}
        </button>
      </form>

      {/* AI-Powered Quick Actions */}
      <div className="mt-4 flex flex-wrap gap-2">
        <button
          type="button"
          onClick={() => setMessage("Analyze all my uploaded documents for compliance gaps and provide detailed recommendations")}
          disabled={isLoading || disabled}
          className="ms-text-caption px-3 py-2 rounded-sm hover:bg-black hover:bg-opacity-5 disabled:opacity-50 transition-colors"
          style={{
            backgroundColor: 'var(--primary)',
            color: 'var(--primary-foreground)',
            border: '1px solid var(--primary)'
          }}
        >
          ✨ Smart Analysis
        </button>
        <button
          type="button"
          onClick={() => setMessage("Create a comprehensive compliance summary with all findings and recommendations")}
          disabled={isLoading || disabled}
          className="ms-text-caption px-3 py-2 rounded-sm hover:bg-black hover:bg-opacity-5 disabled:opacity-50 transition-colors"
          style={{
            backgroundColor: 'var(--primary)',
            color: 'var(--primary-foreground)',
            border: '1px solid var(--primary)'
          }}
        >
          ✨ Smart Summary
        </button>
        <button
          type="button"
          onClick={() => setMessage("What are the data retention requirements?")}
          disabled={isLoading || disabled}
          className="ms-text-caption px-3 py-2 rounded-sm hover:bg-black hover:bg-opacity-5 disabled:opacity-50 transition-colors"
          style={{
            backgroundColor: 'var(--secondary)',
            color: 'var(--secondary-foreground)',
            border: '1px solid var(--border)'
          }}
        >
          Data Retention
        </button>
        <button
          type="button"
          onClick={() => setMessage("What are the privacy policy requirements?")}
          disabled={isLoading || disabled}
          className="ms-text-caption px-3 py-2 rounded-sm hover:bg-black hover:bg-opacity-5 disabled:opacity-50 transition-colors"
          style={{
            backgroundColor: 'var(--secondary)',
            color: 'var(--secondary-foreground)',
            border: '1px solid var(--border)'
          }}
        >
          Privacy Policy
        </button>
        <button
          type="button"
          onClick={() => setMessage("Create a compliance checklist for my industry")}
          disabled={isLoading || disabled}
          className="ms-text-caption px-3 py-2 rounded-sm hover:bg-black hover:bg-opacity-5 disabled:opacity-50 transition-colors"
          style={{
            backgroundColor: 'var(--secondary)',
            color: 'var(--secondary-foreground)',
            border: '1px solid var(--border)'
          }}
        >
          Compliance Checklist
        </button>
        <button
          type="button"
          onClick={() => setMessage("Explain regulatory changes in plain language")}
          disabled={isLoading || disabled}
          className="ms-text-caption px-3 py-2 rounded-sm hover:bg-black hover:bg-opacity-5 disabled:opacity-50 transition-colors"
          style={{
            backgroundColor: 'var(--secondary)',
            color: 'var(--secondary-foreground)',
            border: '1px solid var(--border)'
          }}
        >
          Explain Regulations
        </button>
      </div>
    </div>
  );
};

export default ChatInput;