'use client';

import { useState } from 'react';
import { marked } from 'marked';
import { Bot, User, FileText, CheckCircle, AlertCircle, Clock, Copy, ChevronDown, ChevronUp } from 'lucide-react';

const ChatMessage = ({ message, isBot = false, timestamp, sources = [], confidence, isLoading = false, isError = false, aiModel, processingTime, tokenUsage }) => {
  const [showSources, setShowSources] = useState(false);
  const [copied, setCopied] = useState(false);

  const handleCopy = async () => {
    try {
      await navigator.clipboard.writeText(message);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch (error) {
      console.error('Failed to copy text:', error);
    }
  };

  const formatTime = (timestamp) => {
    if (typeof window === 'undefined') {
      // Server-side: return a placeholder or the raw timestamp
      return '';
    }
    // Client-side: format the timestamp
    return new Date(timestamp).toLocaleTimeString([], { 
      hour: '2-digit', 
      minute: '2-digit' 
    });
  };

  return (
    <div className={`flex gap-4 mb-8 ${isBot ? '' : 'flex-row-reverse'}`}>
      {/* Avatar */}
      <div 
        className="flex-shrink-0 w-10 h-10 rounded-sm flex items-center justify-center"
        style={{
          backgroundColor: isBot 
            ? isError 
              ? 'var(--error)' 
              : 'var(--primary)'
            : 'var(--muted)'
        }}
      >
        {isBot ? (
          isError ? (
            <AlertCircle className="w-5 h-5" style={{ color: 'var(--primary-foreground)' }} />
          ) : (
            <Bot className="w-5 h-5" style={{ color: 'var(--primary-foreground)' }} />
          )
        ) : (
          <User className="w-5 h-5" style={{ color: 'var(--muted-foreground)' }} />
        )}
      </div>

      {/* Message Content */}
      <div className={`flex-1 max-w-4xl ${isBot ? '' : 'flex flex-col items-end'}`}>
        {/* Message Bubble */}
        <div 
          className="ms-card px-6 py-4"
          style={{
            backgroundColor: isBot
              ? isError
                ? 'var(--error)10'
                : 'var(--card)'
              : 'var(--primary)',
            borderColor: isBot
              ? isError
                ? 'var(--error)'
                : 'var(--border)'
              : 'var(--primary)',
            color: isBot ? 'var(--card-foreground)' : 'var(--primary-foreground)',
            boxShadow: 'var(--shadow)',
            borderRadius: '8px'
          }}
        >
          {isLoading ? (
            <div className="flex flex-col space-y-3 animate-slide-in">
              <div className="flex items-center space-x-3">
                <div className="flex space-x-1">
                  <div 
                    className="w-2 h-2 rounded-full"
                    style={{ 
                      backgroundColor: 'var(--primary)',
                      animation: 'wave 1s ease-in-out infinite',
                      animationDelay: '0s'
                    }}
                  ></div>
                  <div 
                    className="w-2 h-2 rounded-full"
                    style={{ 
                      backgroundColor: 'var(--primary)',
                      animation: 'wave 1s ease-in-out infinite',
                      animationDelay: '0.2s'
                    }}
                  ></div>
                  <div 
                    className="w-2 h-2 rounded-full"
                    style={{ 
                      backgroundColor: 'var(--primary)',
                      animation: 'wave 1s ease-in-out infinite',
                      animationDelay: '0.4s'
                    }}
                  ></div>
                </div>
                <span 
                  className="ms-text-body animate-typing" 
                  style={{ color: 'var(--muted-foreground)' }}
                >
                  Processing your request...
                </span>
              </div>
              <div 
                className="text-xs" 
                style={{ color: 'var(--muted-foreground)', opacity: 0.8 }}
              >
                Analyzing documents and compliance requirements
              </div>
            </div>
          ) : (
            <div>
              <div 
                className="ms-text-body"
                style={{
                  color: isBot 
                    ? isError
                      ? 'var(--error)'
                      : 'var(--card-foreground)'
                    : 'var(--primary-foreground)'
                }}
              >
                <div 
                  dangerouslySetInnerHTML={{ 
                    __html: marked(message, {
                      gfm: true,
                      breaks: true
                    }) 
                  }}
                  className="markdown-content"
                />
              </div>

              {/* AI Metadata */}
              {isBot && !isError && !isLoading && (
                <div className="mt-4 space-y-2">
                  {/* Confidence and AI Model */}
                  <div className="flex items-center space-x-4 flex-wrap">
                    {confidence && (
                      <div className="flex items-center ms-text-caption" style={{ color: 'var(--muted-foreground)' }}>
                        <CheckCircle className="w-3 h-3 mr-1" />
                        <span>{Math.round(confidence * 100)}% confidence</span>
                      </div>
                    )}
                    {aiModel && (
                      <div className="flex items-center ms-text-caption" style={{ color: 'var(--muted-foreground)' }}>
                        <Bot className="w-3 h-3 mr-1" />
                        <span>{aiModel}</span>
                      </div>
                    )}
                    {processingTime && (
                      <div className="flex items-center ms-text-caption" style={{ color: 'var(--muted-foreground)' }}>
                        <Clock className="w-3 h-3 mr-1" />
                        <span>{processingTime}ms</span>
                      </div>
                    )}
                  </div>
                  
                  {/* Token Usage (for debugging/development) */}
                  {tokenUsage && process.env.NODE_ENV === 'development' && (
                    <div className="ms-text-caption" style={{ color: 'var(--muted-foreground)' }}>
                      Tokens: {tokenUsage.prompt || 0} + {tokenUsage.completion || 0} = {(tokenUsage.prompt || 0) + (tokenUsage.completion || 0)}
                    </div>
                  )}
                </div>
              )}
            </div>
          )}
        </div>

        {/* Message Actions */}
        {!isLoading && (
          <div className="flex items-center space-x-2 mt-2">
            <button
              onClick={handleCopy}
              className="text-xs text-gray-400 hover:text-gray-600 dark:hover:text-gray-300 flex items-center space-x-1"
            >
              <Copy className="w-3 h-3" />
              <span>{copied ? 'Copied!' : 'Copy'}</span>
            </button>
            {timestamp && (
              <span className="text-xs text-gray-400">
                <Clock className="w-3 h-3 inline mr-1" />
                {formatTime(timestamp)}
              </span>
            )}
          </div>
        )}

        {/* Sources Section */}
        {isBot && sources && sources.length > 0 && !isLoading && (
          <div className="mt-4 w-full">
            <button
              onClick={() => setShowSources(!showSources)}
              className="flex items-center text-sm text-blue-600 dark:text-blue-400 hover:text-blue-800 dark:hover:text-blue-300 mb-2"
            >
              <FileText className="w-4 h-4 mr-1" />
              <span>{sources.length} source{sources.length > 1 ? 's' : ''}</span>
              {showSources ? (
                <ChevronUp className="w-4 h-4 ml-1" />
              ) : (
                <ChevronDown className="w-4 h-4 ml-1" />
              )}
            </button>

            {showSources && (
              <div className="space-y-2">
                {sources.map((source, index) => (
                  <div key={index} className="p-3 bg-gray-50 dark:bg-gray-700 rounded-lg border-l-4 border-blue-400">
                    <div className="flex items-start justify-between mb-2">
                      <h4 className="font-medium text-sm text-gray-900 dark:text-gray-100">
                        {source.document || source.filename || `Source ${index + 1}`}
                      </h4>
                      {source.confidence && (
                        <span className="text-xs px-2 py-1 bg-blue-100 text-blue-800 dark:bg-blue-900/30 dark:text-blue-400 rounded-full">
                          {Math.round(source.confidence * 100)}% match
                        </span>
                      )}
                    </div>
                    <p className="text-sm text-gray-600 dark:text-gray-400 mb-1">
                      {source.content || source.text || 'Content not available'}
                    </p>
                    {source.page && (
                      <p className="text-xs text-gray-500 dark:text-gray-400">
                        Page {source.page}
                      </p>
                    )}
                  </div>
                ))}
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  );
};

export default ChatMessage;