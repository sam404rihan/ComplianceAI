'use client';

import { useState, useEffect } from 'react';
import { Activity, CheckCircle, XCircle, AlertTriangle, RefreshCw, Clock, Wifi, WifiOff } from 'lucide-react';
import { complianceApi } from '../lib/api';
import toast from 'react-hot-toast';

const HealthDisplay = () => {
  const [healthData, setHealthData] = useState(null);
  const [isLoading, setIsLoading] = useState(false);
  const [lastChecked, setLastChecked] = useState(null);
  const [autoCheck, setAutoCheck] = useState(true);
  const [connectionStatus, setConnectionStatus] = useState('checking');

  const checkHealth = async (showToast = false) => {
    setIsLoading(true);
    setConnectionStatus('checking');
    
    try {
      const result = await complianceApi.getHealthStatus();
      
      if (result.success) {
        setHealthData(result.data);
        setConnectionStatus('connected');
        setLastChecked(new Date().toISOString());
        if (showToast) {
          toast.success('Health check completed');
        }
      } else {
        setConnectionStatus('error');
        toast.error(result.message);
        setHealthData(null);
      }
    } catch (error) {
      setConnectionStatus('disconnected');
      toast.error('Backend is not available');
      setHealthData(null);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    checkHealth();
  }, []);

  useEffect(() => {
    let interval;
    if (autoCheck) {
      interval = setInterval(() => {
        checkHealth();
      }, 15000); // Check every 15 seconds
    }
    return () => {
      if (interval) {
        clearInterval(interval);
      }
    };
  }, [autoCheck]);

  const formatTimestamp = (timestamp) => {
    return new Date(timestamp).toLocaleString();
  };

  const formatUptime = (seconds) => {
    if (!seconds) return 'N/A';
    
    const days = Math.floor(seconds / 86400);
    const hours = Math.floor((seconds % 86400) / 3600);
    const minutes = Math.floor((seconds % 3600) / 60);
    
    if (days > 0) {
      return `${days}d ${hours}h ${minutes}m`;
    } else if (hours > 0) {
      return `${hours}h ${minutes}m`;
    } else {
      return `${minutes}m`;
    }
  };

  const getStatusColor = (status) => {
    switch (status?.toLowerCase()) {
      case 'up':
      case 'healthy':
      case 'ok':
      case 'running':
        return 'green';
      case 'down':
      case 'unhealthy':
      case 'error':
      case 'failed':
        return 'red';
      case 'warning':
      case 'degraded':
        return 'yellow';
      default:
        return 'gray';
    }
  };

  const getStatusIcon = (status) => {
    const color = getStatusColor(status);
    switch (color) {
      case 'green':
        return <CheckCircle className="w-5 h-5 text-green-500" />;
      case 'red':
        return <XCircle className="w-5 h-5 text-red-500" />;
      case 'yellow':
        return <AlertTriangle className="w-5 h-5 text-yellow-500" />;
      default:
        return <Activity className="w-5 h-5 text-gray-400" />;
    }
  };

  const ConnectionIndicator = () => {
    switch (connectionStatus) {
      case 'connected':
        return (
          <div className="flex items-center text-green-600">
            <Wifi className="w-4 h-4 mr-2" />
            Connected
          </div>
        );
      case 'disconnected':
        return (
          <div className="flex items-center text-red-600">
            <WifiOff className="w-4 h-4 mr-2" />
            Disconnected
          </div>
        );
      case 'error':
        return (
          <div className="flex items-center text-red-600">
            <XCircle className="w-4 h-4 mr-2" />
            Error
          </div>
        );
      default:
        return (
          <div className="flex items-center text-gray-600">
            <div className="animate-spin rounded-full h-4 w-4 border-2 border-gray-600 border-t-transparent mr-2"></div>
            Checking...
          </div>
        );
    }
  };

  const ServiceStatus = ({ name, status, details }) => (
    <div className="flex items-center justify-between p-3 bg-gray-50 dark:bg-gray-700 rounded-lg">
      <div className="flex items-center">
        {getStatusIcon(status)}
        <div className="ml-3">
          <p className="font-medium text-gray-900 dark:text-white">
            {name}
          </p>
          {details && (
            <p className="text-sm text-gray-500 dark:text-gray-400">
              {details}
            </p>
          )}
        </div>
      </div>
      <span className={`px-2 py-1 text-xs font-medium rounded-full ${
        getStatusColor(status) === 'green' 
          ? 'bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-400'
          : getStatusColor(status) === 'red'
          ? 'bg-red-100 text-red-800 dark:bg-red-900/30 dark:text-red-400'
          : getStatusColor(status) === 'yellow'
          ? 'bg-yellow-100 text-yellow-800 dark:bg-yellow-900/30 dark:text-yellow-400'
          : 'bg-gray-100 text-gray-800 dark:bg-gray-900/30 dark:text-gray-400'
      }`}>
        {status || 'Unknown'}
      </span>
    </div>
  );

  return (
    <div className="bg-white dark:bg-gray-800 rounded-lg shadow-md p-6">
      <div className="flex items-center justify-between mb-6">
        <div>
          <h2 className="text-2xl font-bold text-gray-900 dark:text-white mb-2">
            System Health
          </h2>
          <p className="text-gray-600 dark:text-gray-400">
            Backend service status and monitoring
          </p>
        </div>
        <div className="flex items-center space-x-4">
          <ConnectionIndicator />
          <label className="flex items-center">
            <input
              type="checkbox"
              checked={autoCheck}
              onChange={(e) => setAutoCheck(e.target.checked)}
              className="mr-2 h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300 rounded"
            />
            <span className="text-sm text-gray-600 dark:text-gray-400">
              Auto-check
            </span>
          </label>
          <button
            onClick={() => checkHealth(true)}
            disabled={isLoading}
            className="inline-flex items-center px-3 py-2 border border-transparent text-sm leading-4 font-medium rounded-md text-white bg-blue-600 hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 disabled:opacity-50"
          >
            <RefreshCw className={`w-4 h-4 mr-2 ${isLoading ? 'animate-spin' : ''}`} />
            Check Now
          </button>
        </div>
      </div>

      {isLoading && !healthData ? (
        <div className="flex items-center justify-center py-12">
          <div className="animate-spin rounded-full h-8 w-8 border-2 border-blue-500 border-t-transparent"></div>
          <span className="ml-3 text-gray-600 dark:text-gray-400">Checking system health...</span>
        </div>
      ) : connectionStatus === 'connected' && healthData ? (
        <div className="space-y-6">
          {/* Overall Status */}
          <div className="flex items-center justify-center p-6 bg-green-50 dark:bg-green-900/20 border border-green-200 dark:border-green-800 rounded-lg">
            <CheckCircle className="w-8 h-8 text-green-500 mr-3" />
            <div>
              <p className="text-lg font-semibold text-green-800 dark:text-green-400">
                System Operational
              </p>
              <p className="text-sm text-green-600 dark:text-green-500">
                All services are running normally
              </p>
            </div>
          </div>

          {/* Service Status */}
          <div>
            <h3 className="text-lg font-semibold text-gray-900 dark:text-white mb-4">
              Service Status
            </h3>
            <div className="space-y-3">
              <ServiceStatus
                name="API Server"
                status={healthData.status || 'UP'}
                details="REST API endpoints"
              />
              
              {healthData.database && (
                <ServiceStatus
                  name="Database"
                  status={healthData.database.status || 'UP'}
                  details={`${healthData.database.type || 'Database'} connection`}
                />
              )}
              
              {healthData.vectorStore && (
                <ServiceStatus
                  name="Vector Store"
                  status={healthData.vectorStore.status || 'UP'}
                  details="Embedding storage and search"
                />
              )}
              
              {healthData.fileStorage && (
                <ServiceStatus
                  name="File Storage"
                  status={healthData.fileStorage.status || 'UP'}
                  details="Document storage system"
                />
              )}
            </div>
          </div>

          {/* System Metrics */}
          {(healthData.metrics || healthData.memory || healthData.disk || healthData.uptime) && (
            <div>
              <h3 className="text-lg font-semibold text-gray-900 dark:text-white mb-4">
                System Metrics
              </h3>
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
                {healthData.uptime && (
                  <div className="p-4 bg-blue-50 dark:bg-blue-900/20 border border-blue-200 dark:border-blue-800 rounded-lg">
                    <p className="text-sm font-medium text-blue-800 dark:text-blue-400">Uptime</p>
                    <p className="text-lg font-bold text-blue-900 dark:text-blue-300">
                      {formatUptime(healthData.uptime)}
                    </p>
                  </div>
                )}
                
                {healthData.memory && (
                  <div className="p-4 bg-purple-50 dark:bg-purple-900/20 border border-purple-200 dark:border-purple-800 rounded-lg">
                    <p className="text-sm font-medium text-purple-800 dark:text-purple-400">Memory Usage</p>
                    <p className="text-lg font-bold text-purple-900 dark:text-purple-300">
                      {healthData.memory.used && healthData.memory.total 
                        ? `${Math.round((healthData.memory.used / healthData.memory.total) * 100)}%`
                        : 'N/A'}
                    </p>
                  </div>
                )}
                
                {healthData.disk && (
                  <div className="p-4 bg-green-50 dark:bg-green-900/20 border border-green-200 dark:border-green-800 rounded-lg">
                    <p className="text-sm font-medium text-green-800 dark:text-green-400">Disk Usage</p>
                    <p className="text-lg font-bold text-green-900 dark:text-green-300">
                      {healthData.disk.used && healthData.disk.total
                        ? `${Math.round((healthData.disk.used / healthData.disk.total) * 100)}%`
                        : 'N/A'}
                    </p>
                  </div>
                )}
                
                {healthData.version && (
                  <div className="p-4 bg-gray-50 dark:bg-gray-700 border border-gray-200 dark:border-gray-600 rounded-lg">
                    <p className="text-sm font-medium text-gray-700 dark:text-gray-300">Version</p>
                    <p className="text-lg font-bold text-gray-900 dark:text-white">
                      {healthData.version}
                    </p>
                  </div>
                )}
              </div>
            </div>
          )}

          {/* Raw Health Data for Development */}
          {process.env.NODE_ENV === 'development' && (
            <details className="bg-gray-50 dark:bg-gray-700 rounded-lg p-4">
              <summary className="cursor-pointer text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                Raw Health Data (Development)
              </summary>
              <pre className="text-xs text-gray-600 dark:text-gray-400 whitespace-pre-wrap overflow-x-auto">
                {JSON.stringify(healthData, null, 2)}
              </pre>
            </details>
          )}
        </div>
      ) : (
        <div className="text-center py-12">
          <div className="flex items-center justify-center p-6 bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 rounded-lg mb-6">
            <XCircle className="w-8 h-8 text-red-500 mr-3" />
            <div>
              <p className="text-lg font-semibold text-red-800 dark:text-red-400">
                System Unavailable
              </p>
              <p className="text-sm text-red-600 dark:text-red-500">
                Unable to connect to backend services
              </p>
            </div>
          </div>
          
          <div className="space-y-4">
            <p className="text-gray-600 dark:text-gray-400">
              Please check that the Spring Boot backend is running and accessible.
            </p>
            <button
              onClick={() => checkHealth(true)}
              disabled={isLoading}
              className="inline-flex items-center px-4 py-2 border border-transparent text-sm font-medium rounded-md text-white bg-blue-600 hover:bg-blue-700 disabled:opacity-50"
            >
              <RefreshCw className={`w-4 h-4 mr-2 ${isLoading ? 'animate-spin' : ''}`} />
              Retry Connection
            </button>
          </div>
        </div>
      )}

      {/* Last Checked */}
      {lastChecked && (
        <div className="flex items-center justify-center pt-4 border-t border-gray-200 dark:border-gray-600">
          <div className="flex items-center text-sm text-gray-500 dark:text-gray-400">
            <Clock className="w-4 h-4 mr-2" />
            Last checked: {formatTimestamp(lastChecked)}
          </div>
        </div>
      )}
    </div>
  );
};

export default HealthDisplay;