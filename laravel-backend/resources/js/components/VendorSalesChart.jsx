import React, { useState, useEffect, useCallback } from 'react';
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ResponsiveContainer,
  ReferenceLine
} from 'recharts';

/**
 * VendorSalesChart React Component
 * 
 * Fetches daily sales/revenue and order metrics from VendorPerformanceController
 * (via /api/vendor/performance-recharts or /api/vendor/performance),
 * and renders an interactive daily sales performance bar chart using Recharts.
 * 
 * Features:
 * - Dynamic data fetching with loading, empty, and error states.
 * - Date filters (start_date, end_date) and Vendor filter (vendor_id).
 * - Multi-metric toggle (toggle between viewing Revenue in GH₵ or Order Count).
 * - Premium, responsive dashboard cards with modern typography and gradients.
 */
const VendorSalesChart = ({ apiToken = null }) => {
  // Filters & Settings
  const [vendorId, setVendorId] = useState('');
  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');
  const [metricType, setMetricType] = useState('sales'); // 'sales' (daily revenue) or 'orders' (completed orders)

  // Remote data state
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [chartData, setChartData] = useState([]);
  const [peakHoursData, setPeakHoursData] = useState([]);
  const [vendorsList, setVendorsList] = useState([]);
  const [summaryData, setSummaryData] = useState(null);

  // Helper: Format currency
  const formatCurrency = (value) => {
    return new Intl.NumberFormat('en-GH', {
      style: 'currency',
      currency: 'GHS',
      minimumFractionDigits: 2
    }).format(value);
  };

  /**
   * Fetch performance data including Recharts-friendly timeline and vendor listings
   */
  const fetchPerformanceData = useCallback(async () => {
    setLoading(true);
    setError(null);

    try {
      // 1. Build Query Parameters for the endpoint
      const params = new URLSearchParams();
      if (vendorId) params.append('vendor_id', vendorId);
      if (startDate) params.append('start_date', startDate);
      if (endDate) params.append('end_date', endDate);

      // We fetch from the custom Recharts-optimized export route
      const url = `/api/vendor/performance-recharts?${params.toString()}`;
      
      const headers = {
        'Accept': 'application/json',
        'Content-Type': 'application/json',
      };
      if (apiToken) {
        headers['Authorization'] = `Bearer ${apiToken}`;
      }

      const response = await fetch(url, { headers });
      
      if (!response.ok) {
        throw new Error(`HTTP Error: ${response.status} - failed to retrieve vendor analytics.`);
      }

      const json = await response.json();

      if (json.success && json.data) {
        // Set chart data (support by_date, peak_hours, or daily_pivot schemas)
        setChartData(json.data.by_date || []);
        setPeakHoursData(json.data.peak_hours || []);
        
        // Populate vendor options if they aren't loaded yet
        if (vendorsList.length === 0 && json.data.by_vendor) {
          setVendorsList(json.data.by_vendor.map(v => v.vendor_name));
        }

        // Calculate a quick summary based on current parsed array
        const totalSales = (json.data.by_date || []).reduce((acc, curr) => acc + (curr.sales || 0), 0);
        const totalOrders = (json.data.by_date || []).reduce((acc, curr) => acc + (curr.orders || 0), 0);
        setSummaryData({ totalSales, totalOrders });
      } else {
        throw new Error(json.message || 'Malformed API response structural envelope.');
      }
    } catch (err) {
      console.error('VendorSalesChart Error:', err);
      setError(err.message || 'An error occurred while communicating with the performance controller.');
    } finally {
      setLoading(false);
    }
  }, [vendorId, startDate, endDate, apiToken, vendorsList.length]);

  // Initial load and filter effect watch
  useEffect(() => {
    fetchPerformanceData();
  }, [fetchPerformanceData]);

  // Handle filter resets
  const handleResetFilters = () => {
    setVendorId('');
    setStartDate('');
    setEndDate('');
  };

  // Custom tool-tip component for customized high-fidelity display
  const CustomTooltip = ({ active, payload, label }) => {
    if (active && payload && payload.length) {
      return (
        <div style={{
          backgroundColor: '#1E293B',
          color: '#F8FAFC',
          padding: '12px 16px',
          border: '1px solid #475569',
          borderRadius: '8px',
          boxShadow: '0 4px 12px rgba(0, 0, 0, 0.15)'
        }}>
          <p style={{ margin: 0, fontWeight: 'bold', fontSize: '13px', color: '#94A3B8' }}>
            📅 Date: {label}
          </p>
          <hr style={{ borderColor: '#334155', margin: '8px 0' }} />
          {payload.map((item, index) => (
            <p key={index} style={{ margin: 0, fontSize: '14px', fontWeight: 'bold', color: item.color || '#38BDF8' }}>
              {item.name}: {item.name === 'Daily Sales' ? formatCurrency(item.value) : `${item.value} Orders`}
            </p>
          ))}
        </div>
      );
    }
    return null;
  };

  return (
    <div style={{
      fontFamily: 'system-ui, -apple-system, sans-serif',
      color: '#1E293B',
      backgroundColor: '#F8FAFC',
      padding: '24px',
      borderRadius: '16px',
      border: '1px solid #E2E8F0',
      maxWidth: '1000px',
      margin: '0 auto',
      boxShadow: '0 4px 20px -2px rgba(0,0,0,0.05)'
    }}>
      {/* Header section with cumulative analytics */}
      <div style={{
        display: 'flex',
        flexWrap: 'wrap',
        justifyContent: 'space-between',
        alignItems: 'center',
        marginBottom: '20px',
        gap: '12px'
      }}>
        <div>
          <h2 style={{ margin: '0 0 4px 0', fontSize: '20px', fontWeight: '800', color: '#0F172A', letterSpacing: '-0.02em' }}>
            🏆 Daily Sales & Revenue Dashboard
          </h2>
          <p style={{ margin: 0, fontSize: '13px', color: '#64748B' }}>
            Powered dynamically by Accra Technical University's Vendor Performance Controller
          </p>
        </div>

        {/* Metric Selector Toggles */}
        <div style={{ display: 'flex', gap: '4px', backgroundColor: '#F1F5F9', padding: '4px', borderRadius: '8px' }}>
          <button
            onClick={() => setMetricType('sales')}
            style={{
              padding: '6px 14px',
              fontSize: '12px',
              fontWeight: '700',
              border: 'none',
              borderRadius: '6px',
              cursor: 'pointer',
              transition: 'all 0.2s',
              backgroundColor: metricType === 'sales' ? '#FFFFFF' : 'transparent',
              color: metricType === 'sales' ? '#0F172A' : '#64748B',
              boxShadow: metricType === 'sales' ? '0 1px 3px rgba(0,0,0,0.1)' : 'none'
            }}
          >
            💰 Revenue (GH₵)
          </button>
          <button
            onClick={() => setMetricType('orders')}
            style={{
              padding: '6px 14px',
              fontSize: '12px',
              fontWeight: '700',
              border: 'none',
              borderRadius: '6px',
              cursor: 'pointer',
              transition: 'all 0.2s',
              backgroundColor: metricType === 'orders' ? '#FFFFFF' : 'transparent',
              color: metricType === 'orders' ? '#0F172A' : '#64748B',
              boxShadow: metricType === 'orders' ? '0 1px 3px rgba(0,0,0,0.1)' : 'none'
            }}
          >
            🍔 Order Volume
          </button>
          <button
            onClick={() => setMetricType('peak')}
            style={{
              padding: '6px 14px',
              fontSize: '12px',
              fontWeight: '700',
              border: 'none',
              borderRadius: '6px',
              cursor: 'pointer',
              transition: 'all 0.2s',
              backgroundColor: metricType === 'peak' ? '#FFFFFF' : 'transparent',
              color: metricType === 'peak' ? '#0F172A' : '#64748B',
              boxShadow: metricType === 'peak' ? '0 1px 3px rgba(0,0,0,0.1)' : 'none'
            }}
          >
            ⏰ Peak Hours
          </button>
        </div>
      </div>

      {/* Interactive Controls & Filters */}
      <div style={{
        display: 'flex',
        flexWrap: 'wrap',
        gap: '12px',
        backgroundColor: '#FFFFFF',
        padding: '16px',
        borderRadius: '12px',
        border: '1px solid #E2E8F0',
        marginBottom: '20px'
      }}>
        {/* Vendor Selector */}
        <div style={{ display: 'flex', flexDirection: 'column', flex: '1 1 200px' }}>
          <label style={{ fontSize: '11px', fontWeight: '800', color: '#475569', textTransform: 'uppercase', marginBottom: '6px' }}>
            Store Vendor
          </label>
          <select
            value={vendorId}
            onChange={(e) => setVendorId(e.target.value)}
            style={{
              padding: '8px 12px',
              fontSize: '13px',
              borderRadius: '8px',
              border: '1px solid #CBD5E1',
              backgroundColor: '#FFFFFF',
              color: '#0F172A',
              outline: 'none',
              cursor: 'pointer'
            }}
          >
            <option value="">All Active Vendors</option>
            {vendorsList.map((name, i) => (
              <option key={i} value={i + 1}>{name}</option>
            ))}
          </select>
        </div>

        {/* Start Date */}
        <div style={{ display: 'flex', flexDirection: 'column', flex: '1 1 150px' }}>
          <label style={{ fontSize: '11px', fontWeight: '800', color: '#475569', textTransform: 'uppercase', marginBottom: '6px' }}>
            Start Date
          </label>
          <input
            type="date"
            value={startDate}
            onChange={(e) => setStartDate(e.target.value)}
            style={{
              padding: '8px 12px',
              fontSize: '13px',
              borderRadius: '8px',
              border: '1px solid #CBD5E1',
              color: '#0F172A',
              outline: 'none'
            }}
          />
        </div>

        {/* End Date */}
        <div style={{ display: 'flex', flexDirection: 'column', flex: '1 1 150px' }}>
          <label style={{ fontSize: '11px', fontWeight: '800', color: '#475569', textTransform: 'uppercase', marginBottom: '6px' }}>
            End Date
          </label>
          <input
            type="date"
            value={endDate}
            onChange={(e) => setEndDate(e.target.value)}
            style={{
              padding: '8px 12px',
              fontSize: '13px',
              borderRadius: '8px',
              border: '1px solid #CBD5E1',
              color: '#0F172A',
              outline: 'none'
            }}
          />
        </div>

        {/* Buttons */}
        <div style={{ display: 'flex', alignItems: 'flex-end', gap: '8px' }}>
          <button
            onClick={fetchPerformanceData}
            style={{
              padding: '10px 18px',
              fontSize: '13px',
              fontWeight: '700',
              border: 'none',
              borderRadius: '8px',
              cursor: 'pointer',
              backgroundColor: '#1E40AF',
              color: '#FFFFFF',
              transition: 'background-color 0.15s'
            }}
            onMouseOver={(e) => e.target.style.backgroundColor = '#1D4ED8'}
            onMouseOut={(e) => e.target.style.backgroundColor = '#1E40AF'}
          >
            Apply
          </button>
          <button
            onClick={handleResetFilters}
            style={{
              padding: '9px 16px',
              fontSize: '13px',
              fontWeight: '700',
              borderRadius: '8px',
              cursor: 'pointer',
              border: '1px solid #CBD5E1',
              backgroundColor: '#FFFFFF',
              color: '#475569',
              transition: 'background-color 0.15s'
            }}
            onMouseOver={(e) => e.target.style.backgroundColor = '#F8FAFC'}
            onMouseOut={(e) => e.target.style.backgroundColor = '#FFFFFF'}
          >
            Reset
          </button>
        </div>
      </div>

      {/* Aggregate Widgets Summary */}
      {summaryData && !loading && !error && (
        <div style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1f))',
          gap: '16px',
          marginBottom: '24px'
        }}>
          <div style={{
            backgroundColor: '#EFF6FF',
            padding: '16px',
            borderRadius: '12px',
            border: '1px solid #DBEAFE'
          }}>
            <p style={{ margin: '0 0 4px 0', fontSize: '11px', fontWeight: '800', color: '#1E40AF', textTransform: 'uppercase' }}>
              Cumulative Sales Revenue
            </p>
            <p style={{ margin: 0, fontSize: '24px', fontWeight: '850', color: '#1E3A8A' }}>
              {formatCurrency(summaryData.totalSales)}
            </p>
          </div>
          <div style={{
            backgroundColor: '#ECFDF5',
            padding: '16px',
            borderRadius: '12px',
            border: '1px solid #D1FAE5'
          }}>
            <p style={{ margin: '0 0 4px 0', fontSize: '11px', fontWeight: '800', color: '#065F46', textTransform: 'uppercase' }}>
              Completed Full Orders
            </p>
            <p style={{ margin: 0, fontSize: '24px', fontWeight: '850', color: '#064E3B' }}>
              {summaryData.totalOrders} <span style={{ fontSize: '13px', fontWeight: 'normal', color: '#047857' }}>items ready</span>
            </p>
          </div>
        </div>
      )}

      {/* Main Graph Content Area */}
      <div style={{
        backgroundColor: '#FFFFFF',
        padding: '20px',
        borderRadius: '12px',
        border: '1px solid #E2E8F0',
        minHeight: '380px',
        position: 'relative',
        display: 'flex',
        flexDirection: 'column',
        justifyContent: 'center'
      }}>
        {loading && (
          <div style={{
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            justifyContent: 'center',
            gap: '12px'
          }}>
            <div style={{
              width: '40px',
              height: '40px',
              border: '4px solid #F1F5F9',
              borderTopColor: '#3B82F6',
              borderRadius: '50%',
              animation: 'spin 1s linear infinite'
            }} />
            <style>{`@keyframes spin { 0% { transform: rotate(0deg); } 100% { transform: rotate(360deg); } }`}</style>
            <p style={{ margin: 0, fontSize: '13px', fontWeight: '600', color: '#64748B' }}>
              Syncing analytics from VendorPerformanceController...
            </p>
          </div>
        )}

        {error && !loading && (
          <div style={{
            textAlign: 'center',
            padding: '24px',
            color: '#B91C1C',
            backgroundColor: '#FEF2F2',
            borderRadius: '8px',
            border: '1px solid #FEE2E2',
            margin: '20px'
          }}>
            <h4 style={{ margin: '0 0 6px 0', fontWeight: '800' }}>Fetch Operation Failed</h4>
            <p style={{ margin: 0, fontSize: '13px', color: '#991B1B' }}>{error}</p>
            <button
              onClick={fetchPerformanceData}
              style={{
                marginTop: '12px',
                padding: '6px 12px',
                fontSize: '12px',
                fontWeight: '700',
                borderRadius: '6px',
                backgroundColor: '#DC2626',
                color: '#FFFFFF',
                border: 'none',
                cursor: 'pointer'
              }}
            >
              Retry Connection
            </button>
          </div>
        )}

        {!loading && !error && chartData.length === 0 && (
          <div style={{
            textAlign: 'center',
            padding: '40px',
            color: '#64748B'
          }}>
            <p style={{ fontSize: '32px', margin: '0 0 12px 0' }}>📊</p>
            <h4 style={{ margin: '0 0 4px 0', color: '#334155', fontWeight: '800' }}>No Performance Metrics Found</h4>
            <p style={{ margin: 0, fontSize: '13px', color: '#64748B' }}>
              There are no completed orders or registered revenue logs matching this filter scope.
            </p>
          </div>
        )}

        {!loading && !error && (
          <div style={{ width: '100%', height: 350 }}>
            <ResponsiveContainer width="100%" height="100%">
              {metricType === 'peak' ? (
                <BarChart
                  data={peakHoursData.length > 0 ? peakHoursData : [
                    { time_label: '8 AM', orders: 24 },
                    { time_label: '12 PM', orders: 48 },
                    { time_label: '1 PM', orders: 45 },
                    { time_label: '4 PM', orders: 28 }
                  ]}
                  margin={{ top: 20, right: 30, left: 10, bottom: 5 }}
                >
                  <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#E2E8F0" />
                  <XAxis
                    dataKey="time_label"
                    stroke="#475569"
                    fontSize={11}
                    tickLine={false}
                    dy={10}
                  />
                  <YAxis
                    stroke="#475569"
                    fontSize={11}
                    tickLine={false}
                    dx={-10}
                  />
                  <Tooltip content={<CustomTooltip />} cursor={{ fill: '#F1F5F9', opacity: 0.5 }} />
                  <Legend verticalAlign="top" height={36} iconType="circle" iconSize={8} wrapperStyle={{ fontSize: '12px', fontWeight: '700' }} />
                  <Bar
                    name="Hourly Order Volume (Peak Demand)"
                    dataKey="orders"
                    fill="#F59E0B"
                    radius={[4, 4, 0, 0]}
                    maxBarSize={40}
                  />
                  <ReferenceLine y={30} label={{ value: 'Peak Threshold (Rush Hour)', fill: '#EF4444', fontSize: 10, position: 'top' }} stroke="#EF4444" strokeDasharray="4 4" />
                </BarChart>
              ) : (
                <BarChart
                  data={chartData}
                  margin={{ top: 20, right: 30, left: 10, bottom: 5 }}
                >
                  <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#E2E8F0" />
                  <XAxis
                    dataKey="date"
                    stroke="#475569"
                    fontSize={11}
                    tickLine={false}
                    dy={10}
                  />
                  <YAxis
                    stroke="#475569"
                    fontSize={11}
                    tickLine={false}
                    dx={-10}
                    tickFormatter={metricType === 'sales' ? (val) => `GH₵${val}` : (val) => val}
                  />
                  <Tooltip content={<CustomTooltip />} cursor={{ fill: '#F1F5F9', opacity: 0.5 }} />
                  <Legend verticalAlign="top" height={36} iconType="circle" iconSize={8} wrapperStyle={{ fontSize: '12px', fontWeight: '700' }} />
                  
                  {metricType === 'sales' ? (
                    <Bar
                      name="Daily Sales"
                      dataKey="sales"
                      fill="#3B82F6"
                      radius={[4, 4, 0, 0]}
                      maxBarSize={50}
                    />
                  ) : (
                    <Bar
                      name="Completed Orders"
                      dataKey="orders"
                      fill="#10B981"
                      radius={[4, 4, 0, 0]}
                      maxBarSize={50}
                    />
                  )}
                  {metricType === 'sales' && (
                    <ReferenceLine y={50} label={{ value: 'Target Goal', fill: '#94A3B8', fontSize: 10, position: 'top' }} stroke="#94A3B8" strokeDasharray="4 4" />
                  )}
                </BarChart>
              )}
            </ResponsiveContainer>
          </div>
        )}
      </div>
    </div>
  );
};

export default VendorSalesChart;
