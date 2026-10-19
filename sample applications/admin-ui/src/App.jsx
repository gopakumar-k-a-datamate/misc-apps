import React, { useState, useEffect } from 'react';
import SubjectSelector from './components/SubjectSelector';
import PolicyGrid from './components/PolicyGrid';
import { fetchNamespaces } from './api/apiClient';
import './index.css';

function App() {
  const [subjectType, setSubjectType] = useState('ROLE');
  const [subjectId, setSubjectId] = useState('');
  const [moduleName, setModuleName] = useState('pharmacy');
  const [availableModules, setAvailableModules] = useState(['pharmacy']);

  useEffect(() => {
    const loadModules = async () => {
      try {
        // Fetch from microservices (falling back to empty arrays if offline)
        const [pharmacyModules, identityModules] = await Promise.all([
          fetchNamespaces(8083).catch(() => []),
          fetchNamespaces(8085).catch(() => [])
        ]);
        
        const combined = [...new Set([...pharmacyModules, ...identityModules])];
        if (combined.length > 0) {
          setAvailableModules(combined);
        }
      } catch (err) {
        console.error("Failed to load namespaces");
      }
    };
    loadModules();
  }, []);

  return (
    <div className="app-container">
      <div className="header">
        <h1>Authorization Dashboard</h1>
        <SubjectSelector 
          subjectType={subjectType} setSubjectType={setSubjectType} 
          subjectId={subjectId} setSubjectId={setSubjectId} 
        />
      </div>

      <div className="glass-panel">
        <div className="tabs">
          {availableModules.map(mod => (
            <button 
              key={mod}
              className={`tab ${moduleName === mod ? 'active' : ''}`}
              onClick={() => setModuleName(mod)}
              style={{ textTransform: 'capitalize' }}
            >
              {mod} Module
            </button>
          ))}
        </div>

        <PolicyGrid subjectType={subjectType} subjectId={subjectId} moduleName={moduleName} />
      </div>
    </div>
  );
}

export default App;
