import React, { useState, useEffect, useRef, useCallback } from 'react';
import { fetchOptionsEndpoint } from '../api/apiClient';

const DynamicDropdown = ({ endpoint, permissionCode, value, onChange }) => {
  const [isOpen, setIsOpen] = useState(false);
  const [options, setOptions] = useState([]);
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState('');
  const [hasMore, setHasMore] = useState(true);
  const [loading, setLoading] = useState(false);
  const [displayName, setDisplayName] = useState(''); // What to show when closed

  const dropdownRef = useRef(null);
  const observerRef = useRef(null);

  // Initial load or search change
  useEffect(() => {
    let active = true;
    const load = async () => {
      setLoading(true);
      const data = await fetchOptionsEndpoint(permissionCode, endpoint, 0, search);
      if (active) {
        setOptions(data.content || []);
        setHasMore(!data.last);
        setPage(0);
        setLoading(false);
      }
    };
    
    const timeoutId = setTimeout(load, 300); // debounce search
    return () => {
      active = false;
      clearTimeout(timeoutId);
    };
  }, [search, endpoint, permissionCode]);

  // Load next page
  const loadMore = useCallback(async () => {
    if (loading || !hasMore) return;
    setLoading(true);
    const nextPage = page + 1;
    const data = await fetchOptionsEndpoint(permissionCode, endpoint, nextPage, search);
    setOptions(prev => [...prev, ...(data.content || [])]);
    setHasMore(!data.last);
    setPage(nextPage);
    setLoading(false);
  }, [page, search, loading, hasMore, endpoint, permissionCode]);

  // Infinite scroll observer
  const lastElementRef = useCallback(node => {
    if (loading) return;
    if (observerRef.current) observerRef.current.disconnect();
    observerRef.current = new IntersectionObserver(entries => {
      if (entries[0].isIntersecting && hasMore) {
        loadMore();
      }
    });
    if (node) observerRef.current.observe(node);
  }, [loading, hasMore, loadMore]);

  // Handle outside click
  useEffect(() => {
    const handleClickOutside = (event) => {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target)) {
        setIsOpen(false);
      }
    };
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  // Update display name when value changes
  useEffect(() => {
    if (!value) {
      setDisplayName('');
    } else {
      // Find display name from options if it exists
      const opt = options.find(o => o.id === value);
      if (opt) setDisplayName(opt.displayName);
      else if (!displayName) setDisplayName(value); // fallback
    }
  }, [value, options]);

  return (
    <div ref={dropdownRef} style={{ position: 'relative', flex: 1, minWidth: '200px' }}>
      <div 
        onClick={() => setIsOpen(!isOpen)}
        style={{
          padding: '0.4rem',
          border: '1px solid var(--border-color)',
          borderRadius: '4px',
          background: 'rgba(0, 0, 0, 0.2)',
          cursor: 'pointer',
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          minHeight: '36px'
        }}
      >
        <span style={{ color: value ? 'var(--text-primary)' : 'var(--text-secondary)' }}>
          {value ? displayName || value : 'Select option...'}
        </span>
        <span style={{ fontSize: '0.8rem' }}>▼</span>
      </div>

      {isOpen && (
        <div style={{
          position: 'absolute',
          top: '100%',
          left: 0,
          right: 0,
          marginTop: '4px',
          background: '#1e1e1e', // adjust to your theme
          border: '1px solid var(--border-color)',
          borderRadius: '4px',
          zIndex: 1000,
          boxShadow: '0 4px 6px rgba(0,0,0,0.3)',
          maxHeight: '250px',
          display: 'flex',
          flexDirection: 'column'
        }}>
          <div style={{ padding: '0.4rem', borderBottom: '1px solid var(--border-color)' }}>
            <input 
              type="text" 
              placeholder="Search..." 
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              style={{ 
                width: '100%', 
                padding: '0.3rem', 
                background: 'rgba(255,255,255,0.1)',
                border: 'none',
                borderRadius: '2px',
                color: 'white',
                outline: 'none'
              }}
              autoFocus
            />
          </div>
          
          <div style={{ overflowY: 'auto', flex: 1 }}>
            {options.map((opt, index) => {
              const isLast = index === options.length - 1;
              return (
                <div 
                  key={opt.id}
                  ref={isLast ? lastElementRef : null}
                  onClick={() => {
                    onChange(opt.id);
                    setDisplayName(opt.displayName);
                    setIsOpen(false);
                    setSearch('');
                  }}
                  style={{
                    padding: '0.5rem',
                    cursor: 'pointer',
                    background: value === opt.id ? 'rgba(59, 130, 246, 0.2)' : 'transparent',
                    borderBottom: '1px solid rgba(255,255,255,0.05)'
                  }}
                  onMouseEnter={(e) => e.currentTarget.style.background = 'rgba(255,255,255,0.05)'}
                  onMouseLeave={(e) => e.currentTarget.style.background = value === opt.id ? 'rgba(59, 130, 246, 0.2)' : 'transparent'}
                >
                  {opt.displayName}
                </div>
              );
            })}
            
            {loading && (
              <div style={{ padding: '0.5rem', textAlign: 'center', color: 'var(--text-secondary)' }}>
                Loading...
              </div>
            )}
            
            {!loading && options.length === 0 && (
              <div style={{ padding: '0.5rem', textAlign: 'center', color: 'var(--text-secondary)' }}>
                No options found.
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
};

export default DynamicDropdown;
