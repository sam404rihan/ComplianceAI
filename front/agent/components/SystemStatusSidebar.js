'use client';

import { useState, useEffect } from 'react';
import { 
  Activity, 
  CheckCircle, 
  XCircle, 
  AlertTriangle, 
  Database, 
  FileText, 
  BarChart3,
  RefreshCw,
  Minimize2,
  Maximize2
} from 'lucide-react';
import { complianceApi } from '../lib/api';

const SystemStatusSidebar = ({ isCollapsed, onToggleCollapse }) => {
  const [healthData, setHealthData] = useState(null);
  const [statsData, setStatsData] = useState(null);
  const [isLoading, setIsLoading] = useState(false);
  const [lastUpdated, setLastUpdated] = useState(null);

  const fetchStatusData = async () => {
    setIsLoading(true);
    
    try {
      const [healthResult, statsResult] = await Promise.all([
        complianceApi.getHealthStatus(),
        complianceApi.getStats()
      ]);

      setHealthData(healthResult.success ? healthResult.data : null);
      setStatsData(statsResult.success ? statsResult.data : null);
      setLastUpdated(new Date().toISOString());
    } catch (error) {
      console.error('Failed to fetch status data:', error);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchStatusData();
    
    // Auto-refresh every 30 seconds
    const interval = setInterval(fetchStatusData, 30000);
    return () => clearInterval(interval);
  }, []);

  const getHealthStatus = () => {
    if (!healthData) return { status: 'unknown', color: 'gray', icon: AlertTriangle };
    
    const status = healthData.status?.toLowerCase();
    if (status === 'healthy' || status === 'up') {
      return { status: 'healthy', color: 'green', icon: CheckCircle };
    } else if (status === 'degraded' || status === 'warning') {
      return { status: 'degraded', color: 'yellow', icon: AlertTriangle };
    } else {
      return { status: 'unhealthy', color: 'red', icon: XCircle };
    }
  };

  const formatNumber = (num) => {
    if (!num && num !== 0) return '0';
    if (num >= 1000000) {
      return (num / 1000000).toFixed(1) + 'M';
    } else if (num >= 1000) {
      return (num / 1000).toFixed(1) + 'K';
    }
    return num.toLocaleString();
  };

  const formatTime = (timestamp) => {
    return new Date(timestamp).toLocaleTimeString([], { 
      hour: '2-digit', 
      minute: '2-digit' 
    });
  };

  const health = getHealthStatus();
  const HealthIcon = health.icon;

  if (isCollapsed) {
    return (
      <div className="w-16 bg-gray-50 dark:bg-gray-900 border-l border-gray-200 dark:border-gray-700 flex flex-col items-center py-4">
        <button
          onClick={onToggleCollapse}
          className="p-2 text-gray-500 hover:text-gray-700 dark:text-gray-400 dark:hover:text-gray-300 mb-4"
        >
          <Maximize2 className="w-5 h-5" />
        </button>
        
        <div className="flex flex-col items-center space-y-4">
          {/* Health Status Icon */}
          <div className={`p-2 rounded-full ${
            health.color === 'green' ? 'bg-green-100 dark:bg-green-900/30' :
            health.color === 'yellow' ? 'bg-yellow-100 dark:bg-yellow-900/30' :
            health.color === 'red' ? 'bg-red-100 dark:bg-red-900/30' :
            'bg-gray-100 dark:bg-gray-700'
          }`}>
            <HealthIcon className={`w-4 h-4 ${
              health.color === 'green' ? 'text-green-600 dark:text-green-400' :
              health.color === 'yellow' ? 'text-yellow-600 dark:text-yellow-400' :
              health.color === 'red' ? 'text-red-600 dark:text-red-400' :
              'text-gray-600 dark:text-gray-400'
            }`} />
          </div>

          {/* Quick Stats */}
          <div className="text-center">
            <div className="text-xs font-medium text-gray-900 dark:text-gray-100">
              {formatNumber(statsData?.totalDocuments || statsData?.documentCount || 0)}
            </div>
            <div className="text-xs text-gray-500 dark:text-gray-400">docs</div>
          </div>

          <button
            onClick={fetchStatusData}
            disabled={isLoading}
            className="p-1 text-gray-400 hover:text-gray-600 dark:hover:text-gray-300"
          >
            <RefreshCw className={`w-4 h-4 ${isLoading ? 'animate-spin' : ''}`} />
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="w-80 bg-gray-50 dark:bg-gray-900 border-l border-gray-200 dark:border-gray-700 flex flex-col">
      {/* Header */}
      <div className="flex items-center justify-between p-4 border-b border-gray-200 dark:border-gray-700">
        <h3 className="font-semibold text-gray-900 dark:text-gray-100">System Status</h3>
        <button
          onClick={onToggleCollapse}
          className="p-1 text-gray-500 hover:text-gray-700 dark:text-gray-400 dark:hover:text-gray-300"
        >
          <Minimize2 className="w-4 h-4" />
        </button>
      </div>

      {/* Content */}
      <div className="flex-1 overflow-y-auto p-4 space-y-6">
        {/* Health Status */}
        <div>
          <div className="flex items-center justify-between mb-3">
            <h4 className="text-sm font-medium text-gray-700 dark:text-gray-300">Health Status</h4>
            <button
              onClick={fetchStatusData}
              disabled={isLoading}
              className="p-1 text-gray-400 hover:text-gray-600 dark:hover:text-gray-300 disabled:opacity-50"
            >
              <RefreshCw className={`w-4 h-4 ${isLoading ? 'animate-spin' : ''}`} />
            </button>
          </div>
          
          <div className={`p-3 rounded-lg border-l-4 ${
            health.color === 'green' ? 'bg-green-50 dark:bg-green-900/20 border-green-400' :
            health.color === 'yellow' ? 'bg-yellow-50 dark:bg-yellow-900/20 border-yellow-400' :
            health.color === 'red' ? 'bg-red-50 dark:bg-red-900/20 border-red-400' :
            'bg-gray-50 dark:bg-gray-700 border-gray-400'
          }`}>
            <div className="flex items-center">
              <HealthIcon className={`w-5 h-5 mr-2 ${
                health.color === 'green' ? 'text-green-600 dark:text-green-400' :
                health.color === 'yellow' ? 'text-yellow-600 dark:text-yellow-400' :
                health.color === 'red' ? 'text-red-600 dark:text-red-400' :
                'text-gray-600 dark:text-gray-400'
              }`} />
              <div>
                <p className={`font-medium ${
                  health.color === 'green' ? 'text-green-800 dark:text-green-400' :
                  health.color === 'yellow' ? 'text-yellow-800 dark:text-yellow-400' :
                  health.color === 'red' ? 'text-red-800 dark:text-red-400' :
                  'text-gray-800 dark:text-gray-400'
                }`}>
                  {health.status === 'healthy' ? 'System Operational' :
                   health.status === 'degraded' ? 'Degraded Performance' :
                   health.status === 'unhealthy' ? 'System Down' :
                   'Status Unknown'}
                </p>
                <p className="text-xs text-gray-600 dark:text-gray-400">
                  Backend services {health.status}
                </p>
              </div>
            </div>
          </div>
        </div>

        {/* Quick Statistics */}
        <div>
          <h4 className="text-sm font-medium text-gray-700 dark:text-gray-300 mb-3">Quick Stats</h4>
          <div className="grid grid-cols-2 gap-3">
            <div className="bg-white dark:bg-gray-800 p-3 rounded-lg border border-gray-200 dark:border-gray-700">
              <div className="flex items-center">
                <FileText className="w-5 h-5 text-blue-500 mr-2" />
                <div>
                  <p className="text-lg font-semibold text-gray-900 dark:text-gray-100">
                    {formatNumber(statsData?.totalDocuments || statsData?.documentCount || 0)}
                  </p>
                  <p className="text-xs text-gray-500 dark:text-gray-400">Documents</p>
                </div>
              </div>
            </div>
            
            <div className="bg-white dark:bg-gray-800 p-3 rounded-lg border border-gray-200 dark:border-gray-700">
              <div className="flex items-center">
                <Database className="w-5 h-5 text-green-500 mr-2" />
                <div>
                  <p className="text-lg font-semibold text-gray-900 dark:text-gray-100">
                    {formatNumber(statsData?.totalVectors || statsData?.vectorCount || statsData?.chunksStored || 0)}
                  </p>
                  <p className="text-xs text-gray-500 dark:text-gray-400">Vectors</p>
                </div>
              </div>
            </div>
            
            <div className="bg-white dark:bg-gray-800 p-3 rounded-lg border border-gray-200 dark:border-gray-700">
              <div className="flex items-center">
                <BarChart3 className="w-5 h-5 text-purple-500 mr-2" />
                <div>
                  <p className="text-lg font-semibold text-gray-900 dark:text-gray-100">
                    {formatNumber(statsData?.totalQueries || statsData?.queryCount || statsData?.documentsProcessed || 0)}
                  </p>
                  <p className="text-xs text-gray-500 dark:text-gray-400">Queries</p>
                </div>
              </div>
            </div>
            
            <div className="bg-white dark:bg-gray-800 p-3 rounded-lg border border-gray-200 dark:border-gray-700">
              <div className="flex items-center">
                <Activity className="w-5 h-5 text-orange-500 mr-2" />
                <div>
                  <p className="text-lg font-semibold text-gray-900 dark:text-gray-100">
                    {healthData?.uptime ? `${Math.floor(healthData.uptime / 60)}m` : 'N/A'}
                  </p>
                  <p className="text-xs text-gray-500 dark:text-gray-400">Uptime</p>
                </div>
              </div>
            </div>
          </div>
        </div>

        {/* System Services */}
        {healthData && (
          <div>
            <h4 className="text-sm font-medium text-gray-700 dark:text-gray-300 mb-3">Services</h4>
            <div className="space-y-2">
              <div className="flex items-center justify-between p-2 bg-white dark:bg-gray-800 rounded border border-gray-200 dark:border-gray-700">
                <span className="text-sm text-gray-700 dark:text-gray-300">API Server</span>
                <CheckCircle className="w-4 h-4 text-green-500" />
              </div>
              
              {healthData.database && (
                <div className="flex items-center justify-between p-2 bg-white dark:bg-gray-800 rounded border border-gray-200 dark:border-gray-700">
                  <span className="text-sm text-gray-700 dark:text-gray-300">Database</span>
                  <CheckCircle className="w-4 h-4 text-green-500" />
                </div>
              )}
              
              {healthData.vectorStore && (
                <div className="flex items-center justify-between p-2 bg-white dark:bg-gray-800 rounded border border-gray-200 dark:border-gray-700">
                  <span className="text-sm text-gray-700 dark:text-gray-300">Vector Store</span>
                  <CheckCircle className="w-4 h-4 text-green-500" />
                </div>
              )}
            </div>
          </div>
        )}
      </div>

      {/* Footer */}
      <div className="p-4 border-t border-gray-200 dark:border-gray-700">
        <p className="text-xs text-gray-500 dark:text-gray-400 text-center">
          Last updated: {lastUpdated ? formatTime(lastUpdated) : 'Never'}
        </p>
      </div>
    </div>
  );
};

export default SystemStatusSidebar;