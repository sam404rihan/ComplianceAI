'use client';

import { useState } from 'react';
import { Search, MessageCircle, FileText, AlertCircle, Clock } from 'lucide-react';
import { complianceApi } from '../lib/api';
import toast from 'react-hot-toast';

const QueryForm = () => {
  const [query, setQuery] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [response, setResponse] = useState(null);
  const [queryHistory, setQueryHistory] = useState([]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    
    if (!query.trim()) {
      toast.error('Please enter a compliance question');
      return;
    }

    setIsLoading(true);
    const startTime = Date.now();

    try {
      const result = await complianceApi.queryCompliance(query.trim());
      const responseTime = Date.now() - startTime;
      
      if (result.success) {
        setResponse({
          ...result.data,
          responseTime,
          timestamp: new Date().toISOString()
        });
        
        // Add to query history
        setQueryHistory(prev => [{
          query: query.trim(),
          timestamp: new Date().toISOString(),
          success: true
        }, ...prev.slice(0, 4)]); // Keep last 5 queries
        
        toast.success('Query executed successfully');
      } else {
        toast.error(result.message);
        setResponse(null);
        
        // Add failed query to history
        setQueryHistory(prev => [{
          query: query.trim(),
          timestamp: new Date().toISOString(),
          success: false
        }, ...prev.slice(0, 4)]);
      }
    } catch (error) {
      toast.error('Failed to execute query');
      setResponse(null);
    } finally {
      setIsLoading(false);
    }
  };

  const handleHistoryClick = (historicQuery) => {
    setQuery(historicQuery);
  };

  const formatTimestamp = (timestamp) => {
    return new Date(timestamp).toLocaleString();
  };

  const formatResponseTime = (ms) => {
    return ms < 1000 ? `${ms}ms` : `${(ms / 1000).toFixed(1)}s`;
  };

  return (
    <div className="bg-white dark:bg-gray-800 rounded-lg shadow-md p-6">
      <div className="mb-6">
        <h2 className="text-2xl font-bold text-gray-900 dark:text-white mb-2">
          Compliance Query
        </h2>
        <p className="text-gray-600 dark:text-gray-400">
          Ask questions about your uploaded policy documents and get instant answers.
        </p>
      </div>

      {/* Query Form */}
      <form onSubmit={handleSubmit} className="space-y-4">
        <div>
          <label htmlFor="query" className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
            Your Question
          </label>
          <div className="relative">
            <textarea
              id="query"
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              placeholder="e.g., What are the data retention requirements for customer information?"
              className="w-full px-4 py-3 pl-12 border border-gray-300 dark:border-gray-600 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent dark:bg-gray-700 dark:text-white resize-none"
              rows="4"
              disabled={isLoading}
            />
            <MessageCircle className="absolute left-4 top-4 h-5 w-5 text-gray-400" />
          </div>
        </div>

        <button
          type="submit"
          disabled={isLoading || !query.trim()}
          className="w-full flex items-center justify-center px-4 py-3 border border-transparent text-sm font-medium rounded-lg text-white bg-blue-600 hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 disabled:bg-gray-400 disabled:cursor-not-allowed"
        >
          {isLoading ? (
            <>
              <div className="animate-spin rounded-full h-5 w-5 border-2 border-white border-t-transparent mr-2"></div>
              Searching...
            </>
          ) : (
            <>
              <Search className="w-5 h-5 mr-2" />
              Ask Question
            </>
          )}
        </button>
      </form>

      {/* Query History */}
      {queryHistory.length > 0 && (
        <div className="mt-6">
          <h3 className="text-sm font-medium text-gray-700 dark:text-gray-300 mb-3">
            Recent Queries
          </h3>
          <div className="space-y-2">
            {queryHistory.map((item, index) => (
              <button
                key={index}
                onClick={() => handleHistoryClick(item.query)}
                className="w-full text-left p-3 bg-gray-50 dark:bg-gray-700 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-600 transition-colors"
                disabled={isLoading}
              >
                <div className="flex items-start justify-between">
                  <p className="text-sm text-gray-900 dark:text-white truncate flex-1 mr-2">
                    {item.query}
                  </p>
                  <div className="flex items-center space-x-1">
                    {item.success ? (
                      <div className="w-2 h-2 bg-green-500 rounded-full"></div>
                    ) : (
                      <div className="w-2 h-2 bg-red-500 rounded-full"></div>
                    )}
                    <Clock className="w-3 h-3 text-gray-400" />
                  </div>
                </div>
                <p className="text-xs text-gray-500 dark:text-gray-400 mt-1">
                  {formatTimestamp(item.timestamp)}
                </p>
              </button>
            ))}
          </div>
        </div>
      )}

      {/* Response Display */}
      {response && (
        <div className="mt-8 space-y-6">
          <div className="border-t border-gray-200 dark:border-gray-600 pt-6">
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-lg font-semibold text-gray-900 dark:text-white">
                Answer
              </h3>
              <div className="flex items-center text-sm text-gray-500 dark:text-gray-400">
                <Clock className="w-4 h-4 mr-1" />
                {formatResponseTime(response.responseTime)}
              </div>
            </div>
            
            <div className="bg-blue-50 dark:bg-blue-900/20 border-l-4 border-blue-400 p-4 rounded-r-lg">
              <div className="flex">
                <MessageCircle className="flex-shrink-0 h-5 w-5 text-blue-400 mt-1" />
                <div className="ml-3">
                  <p className="text-blue-800 dark:text-blue-200 whitespace-pre-wrap">
                    {response.answer || response.response || 'No answer provided'}
                  </p>
                </div>
              </div>
            </div>
          </div>

          {/* Referenced Clauses/Sources */}
          {response.sources && response.sources.length > 0 && (
            <div>
              <h4 className="text-md font-semibold text-gray-900 dark:text-white mb-3">
                Referenced Sources
              </h4>
              <div className="space-y-3">
                {response.sources.map((source, index) => (
                  <div key={index} className="bg-gray-50 dark:bg-gray-700 p-4 rounded-lg">
                    <div className="flex items-start">
                      <FileText className="flex-shrink-0 h-5 w-5 text-gray-400 mt-1" />
                      <div className="ml-3 flex-1">
                        <div className="flex items-center justify-between mb-2">
                          <p className="text-sm font-medium text-gray-900 dark:text-white">
                            {source.document || source.filename || `Source ${index + 1}`}
                          </p>
                          {source.confidence && (
                            <span className="text-xs px-2 py-1 bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-400 rounded-full">
                              {Math.round(source.confidence * 100)}% match
                            </span>
                          )}
                        </div>
                        <p className="text-sm text-gray-600 dark:text-gray-400">
                          {source.content || source.text || 'Content not available'}
                        </p>
                        {source.page && (
                          <p className="text-xs text-gray-500 dark:text-gray-400 mt-1">
                            Page {source.page}
                          </p>
                        )}
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Metadata */}
          {(response.metadata || response.confidence) && (
            <div className="bg-gray-50 dark:bg-gray-700 p-4 rounded-lg">
              <h4 className="text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                Query Information
              </h4>
              <div className="grid grid-cols-2 gap-4 text-sm">
                {response.confidence && (
                  <div>
                    <span className="text-gray-500 dark:text-gray-400">Confidence:</span>
                    <span className="ml-2 font-medium text-gray-900 dark:text-white">
                      {Math.round(response.confidence * 100)}%
                    </span>
                  </div>
                )}
                <div>
                  <span className="text-gray-500 dark:text-gray-400">Timestamp:</span>
                  <span className="ml-2 font-medium text-gray-900 dark:text-white">
                    {formatTimestamp(response.timestamp)}
                  </span>
                </div>
              </div>
            </div>
          )}
        </div>
      )}

      {/* Empty State */}
      {!response && !isLoading && (
        <div className="mt-8 text-center py-8">
          <Search className="mx-auto h-12 w-12 text-gray-400 mb-4" />
          <p className="text-gray-500 dark:text-gray-400">
            Ask a compliance question to get started
          </p>
        </div>
      )}
    </div>
  );
};

export default QueryForm;