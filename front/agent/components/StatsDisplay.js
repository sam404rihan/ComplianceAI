'use client';

import { useState, useEffect } from 'react';
import { BarChart3, Database, FileText, Clock, RefreshCw, TrendingUp, Server } from 'lucide-react';
import { complianceApi } from '../lib/api';
import toast from 'react-hot-toast';

const StatsDisplay = () => {
  const [stats, setStats] = useState(null);
  const [isLoading, setIsLoading] = useState(false);
  const [lastUpdated, setLastUpdated] = useState(null);
  const [autoRefresh, setAutoRefresh] = useState(false);

  const fetchStats = async (showToast = false) => {
    setIsLoading(true);
    try {
      const result = await complianceApi.getStats();
      
      if (result.success) {
        setStats(result.data);
        setLastUpdated(new Date().toISOString());
        if (showToast) {
          toast.success('Statistics updated');
        }
      } else {
        toast.error(result.message);
      }
    } catch (error) {
      toast.error('Failed to fetch statistics');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchStats();
  }, []);

  useEffect(() => {
    let interval;
    if (autoRefresh) {
      interval = setInterval(() => {
        fetchStats();
      }, 30000); // Refresh every 30 seconds
    }
    return () => {
      if (interval) {
        clearInterval(interval);
      }
    };
  }, [autoRefresh]);

  const formatNumber = (num) => {
    if (!num && num !== 0) return 'N/A';
    if (num >= 1000000) {
      return (num / 1000000).toFixed(1) + 'M';
    } else if (num >= 1000) {
      return (num / 1000).toFixed(1) + 'K';
    }
    return num.toLocaleString();
  };

  const formatBytes = (bytes) => {
    if (!bytes && bytes !== 0) return 'N/A';
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    if (bytes === 0) return '0 Bytes';
    const i = Math.floor(Math.log(bytes) / Math.log(1024));
    return Math.round(bytes / Math.pow(1024, i) * 100) / 100 + ' ' + sizes[i];
  };

  const formatTimestamp = (timestamp) => {
    return new Date(timestamp).toLocaleString();
  };

  const StatCard = ({ icon: Icon, title, value, subtitle, color = 'blue' }) => {
    const colorClasses = {
      blue: 'bg-blue-50 dark:bg-blue-900/20 border-blue-200 dark:border-blue-800',
      green: 'bg-green-50 dark:bg-green-900/20 border-green-200 dark:border-green-800',
      purple: 'bg-purple-50 dark:bg-purple-900/20 border-purple-200 dark:border-purple-800',
      orange: 'bg-orange-50 dark:bg-orange-900/20 border-orange-200 dark:border-orange-800',
      red: 'bg-red-50 dark:bg-red-900/20 border-red-200 dark:border-red-800'
    };

    const iconColors = {
      blue: 'text-blue-500',
      green: 'text-green-500',
      purple: 'text-purple-500',
      orange: 'text-orange-500',
      red: 'text-red-500'
    };

    return (
      <div className={`border rounded-lg p-6 ${colorClasses[color]}`}>
        <div className="flex items-center">
          <Icon className={`h-8 w-8 ${iconColors[color]} mr-4`} />
          <div>
            <p className="text-sm font-medium text-gray-600 dark:text-gray-400">
              {title}
            </p>
            <p className="text-2xl font-bold text-gray-900 dark:text-white">
              {value}
            </p>
            {subtitle && (
              <p className="text-sm text-gray-500 dark:text-gray-400">
                {subtitle}
              </p>
            )}
          </div>
        </div>
      </div>
    );
  };

  return (
    <div className="bg-white dark:bg-gray-800 rounded-lg shadow-md p-6">
      <div className="flex items-center justify-between mb-6">
        <div>
          <h2 className="text-2xl font-bold text-gray-900 dark:text-white mb-2">
            Vector Store Statistics
          </h2>
          <p className="text-gray-600 dark:text-gray-400">
            Overview of your compliance knowledge base
          </p>
        </div>
        <div className="flex items-center space-x-3">
          <label className="flex items-center">
            <input
              type="checkbox"
              checked={autoRefresh}
              onChange={(e) => setAutoRefresh(e.target.checked)}
              className="mr-2 h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300 rounded"
            />
            <span className="text-sm text-gray-600 dark:text-gray-400">
              Auto-refresh
            </span>
          </label>
          <button
            onClick={() => fetchStats(true)}
            disabled={isLoading}
            className="inline-flex items-center px-3 py-2 border border-transparent text-sm leading-4 font-medium rounded-md text-white bg-blue-600 hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 disabled:opacity-50"
          >
            <RefreshCw className={`w-4 h-4 mr-2 ${isLoading ? 'animate-spin' : ''}`} />
            Refresh
          </button>
        </div>
      </div>

      {isLoading && !stats ? (
        <div className="flex items-center justify-center py-12">
          <div className="animate-spin rounded-full h-8 w-8 border-2 border-blue-500 border-t-transparent"></div>
          <span className="ml-3 text-gray-600 dark:text-gray-400">Loading statistics...</span>
        </div>
      ) : stats ? (
        <div className="space-y-6">
          {/* Main Statistics Grid */}
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
            <StatCard
              icon={FileText}
              title="Total Documents"
              value={formatNumber(stats.totalDocuments || stats.documentCount || 0)}
              subtitle={stats.totalDocuments > 0 ? 'Policy documents indexed' : 'No documents yet'}
              color="blue"
            />
            <StatCard
              icon={Database}
              title="Vector Embeddings"
              value={formatNumber(stats.totalVectors || stats.vectorCount || 0)}
              subtitle="Searchable chunks"
              color="green"
            />
            <StatCard
              icon={BarChart3}
              title="Total Queries"
              value={formatNumber(stats.totalQueries || stats.queryCount || 0)}
              subtitle="Questions processed"
              color="purple"
            />
            <StatCard
              icon={Server}
              title="Storage Used"
              value={formatBytes(stats.storageUsed || stats.indexSize || 0)}
              subtitle="Vector database size"
              color="orange"
            />
          </div>

          {/* Detailed Information */}
          {(stats.recentActivity || stats.performance || stats.indexInfo) && (
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
              {/* Recent Activity */}
              {stats.recentActivity && (
                <div className="bg-gray-50 dark:bg-gray-700 rounded-lg p-6">
                  <h3 className="text-lg font-semibold text-gray-900 dark:text-white mb-4">
                    Recent Activity
                  </h3>
                  <div className="space-y-3">
                    {stats.recentActivity.map((activity, index) => (
                      <div key={index} className="flex items-center justify-between">
                        <span className="text-sm text-gray-600 dark:text-gray-400">
                          {activity.action || activity.type}
                        </span>
                        <span className="text-sm font-medium text-gray-900 dark:text-white">
                          {activity.count || activity.value}
                        </span>
                      </div>
                    ))}
                  </div>
                </div>
              )}

              {/* Performance Metrics */}
              {stats.performance && (
                <div className="bg-gray-50 dark:bg-gray-700 rounded-lg p-6">
                  <h3 className="text-lg font-semibold text-gray-900 dark:text-white mb-4">
                    Performance Metrics
                  </h3>
                  <div className="space-y-3">
                    <div className="flex items-center justify-between">
                      <span className="text-sm text-gray-600 dark:text-gray-400">
                        Average Query Time
                      </span>
                      <span className="text-sm font-medium text-gray-900 dark:text-white">
                        {stats.performance.avgQueryTime || 'N/A'}ms
                      </span>
                    </div>
                    <div className="flex items-center justify-between">
                      <span className="text-sm text-gray-600 dark:text-gray-400">
                        Index Health
                      </span>
                      <span className={`text-sm font-medium ${
                        stats.performance.health === 'healthy' 
                          ? 'text-green-600' 
                          : 'text-red-600'
                      }`}>
                        {stats.performance.health || 'Unknown'}
                      </span>
                    </div>
                  </div>
                </div>
              )}
            </div>
          )}

          {/* Raw Stats Display for Development */}
          {process.env.NODE_ENV === 'development' && (
            <details className="bg-gray-50 dark:bg-gray-700 rounded-lg p-4">
              <summary className="cursor-pointer text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                Raw Statistics (Development)
              </summary>
              <pre className="text-xs text-gray-600 dark:text-gray-400 whitespace-pre-wrap overflow-x-auto">
                {JSON.stringify(stats, null, 2)}
              </pre>
            </details>
          )}

          {/* Last Updated */}
          {lastUpdated && (
            <div className="flex items-center justify-center pt-4 border-t border-gray-200 dark:border-gray-600">
              <div className="flex items-center text-sm text-gray-500 dark:text-gray-400">
                <Clock className="w-4 h-4 mr-2" />
                Last updated: {formatTimestamp(lastUpdated)}
              </div>
            </div>
          )}
        </div>
      ) : (
        <div className="text-center py-12">
          <Database className="mx-auto h-12 w-12 text-gray-400 mb-4" />
          <p className="text-gray-500 dark:text-gray-400 mb-4">
            Unable to load statistics
          </p>
          <button
            onClick={() => fetchStats(true)}
            className="inline-flex items-center px-4 py-2 border border-transparent text-sm font-medium rounded-md text-white bg-blue-600 hover:bg-blue-700"
          >
            <RefreshCw className="w-4 h-4 mr-2" />
            Retry
          </button>
        </div>
      )}
    </div>
  );
};

export default StatsDisplay;