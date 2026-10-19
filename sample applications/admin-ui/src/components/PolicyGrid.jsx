import React, { useState, useEffect } from 'react';
import { fetchPolicies, savePolicies } from '../api/apiClient';
import ConditionBuilder from './ConditionBuilder';

const PolicyGrid = ({ subjectType, subjectId, moduleName }) => {
  const [policies, setPolicies] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [activeConditionPermission, setActiveConditionPermission] = useState(null); // The permissionCode being edited
  const [validationErrors, setValidationErrors] = useState(null);

  useEffect(() => {
    if (subjectId) {
      loadPolicies();
    }
  }, [subjectType, subjectId, moduleName]);

  const loadPolicies = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await fetchPolicies(subjectType, subjectId, moduleName);
      setPolicies(data);
    } catch (err) {
      setPolicies([]);
      setError('Service Not Available');
    } finally {
      setLoading(false);
    }
  };

  const handleToggle = (permissionCode) => {
    setPolicies(prev => prev.map(p => {
      if (p.permissionCode === permissionCode) {
        const newEnabled = !p.enabled;
        return { 
          ...p, 
          enabled: newEnabled,
          effect: (newEnabled && !p.effect) ? 'ALLOW' : p.effect 
        };
      }
      return p;
    }));
  };

  const handleSave = async () => {
    setError(null);
    setValidationErrors(null);
    try {
      const payloadPolicies = policies
        .filter(p => p.enabled)
        .map(p => ({
          permissionCode: p.permissionCode,
          effect: p.effect || 'ALLOW',
          expressionJson: p.expressionJson,
          enabled: p.enabled,
          isDeleted: p.isDeleted || false,
          deletedReason: p.deletedReason,
          disabledReason: p.disabledReason,
          useCustomRego: p.useCustomRego || false,
          customRegoSnippet: p.customRegoSnippet,
          denialMessage: p.denialMessage
        }));

      await savePolicies(subjectType, subjectId, moduleName, payloadPolicies);
      alert('Policies updated successfully.');
    } catch (err) {
      if (err.response && err.response.data && err.response.data.errors) {
        const errInfo = err.response.data;
        setValidationErrors({
            permissionCode: errInfo.permissionCode,
            errors: errInfo.errors
        });
        setError(errInfo.message);
        if (errInfo.permissionCode) {
            setActiveConditionPermission(errInfo.permissionCode);
        }
      } else {
        setError('Failed to save policies. Please check for syntax errors.');
      }
    }
  };

  const handleConditionsSaved = (permissionCode, newExpression, useCustomRego, customRegoSnippet, denialMessage) => {
    setPolicies(prev => prev.map(p => 
      p.permissionCode === permissionCode 
        ? { ...p, expressionJson: newExpression, useCustomRego, customRegoSnippet, denialMessage, enabled: true, effect: p.effect || 'ALLOW' } 
        : p
    ));
    setActiveConditionPermission(null);
  };

  if (loading) return <div>Loading policies...</div>;

  // Group by resourceName
  const grouped = policies.reduce((acc, p) => {
    acc[p.resourceName] = acc[p.resourceName] || [];
    acc[p.resourceName].push(p);
    return acc;
  }, {});

  return (
    <div>
      {error && (
        <div className="glass-panel" style={{ padding: '1rem', color: '#fca5a5', textAlign: 'center', marginBottom: '1rem', whiteSpace: 'pre-wrap', background: 'rgba(239, 68, 68, 0.1)', border: '1px solid rgba(239, 68, 68, 0.3)' }}>
          ⚠️ {error}
        </div>
      )}

      {Object.keys(grouped).map(resource => (
        <div key={resource} className="resource-group">
          <div className="resource-header">
            📁 {resource}
          </div>
          <div className="glass-panel" style={{ padding: '0.5rem' }}>
            {grouped[resource].map(p => (
              <div key={p.permissionCode} className="policy-card">
                <div className="policy-info">
                  <input 
                    type="checkbox" 
                    className="checkbox"
                    checked={p.enabled}
                    onChange={() => handleToggle(p.permissionCode)}
                  />
                  <span style={{ fontWeight: 500, minWidth: '80px' }}>{p.action}</span>
                  {(p.expressionJson || (p.useCustomRego && p.customRegoSnippet)) && (
                    <span className="badge">Has Conditions</span>
                  )}
                  {p.disabledReason && (
                    <span className="badge" style={{ background: 'rgba(239, 68, 68, 0.2)', color: '#fca5a5' }}>
                      ⚠️ {p.disabledReason}
                    </span>
                  )}
                </div>
                <div className="actions">
                  <button className="btn" onClick={() => setActiveConditionPermission(p.permissionCode)}>
                    ⚙️
                  </button>
                </div>
              </div>
            ))}
          </div>
        </div>
      ))}
      
      <div style={{ marginTop: '2rem', display: 'flex', justifyContent: 'flex-end' }}>
        <button className="btn btn-primary" onClick={handleSave}>
          💾 Save Changes
        </button>
      </div>

      {activeConditionPermission && (
        <ConditionBuilder 
          permissionCode={activeConditionPermission}
          policy={policies.find(p => p.permissionCode === activeConditionPermission)}
          validationErrors={validationErrors?.permissionCode === activeConditionPermission ? validationErrors.errors : null}
          onClose={() => setActiveConditionPermission(null)}
          onSave={handleConditionsSaved}
        />
      )}
    </div>
  );
};

export default PolicyGrid;
