import React from 'react';

const RoleSelector = ({ role, setRole }) => {
  const roles = [
    { id: 'ACCOUNTANT', name: 'ACCOUNTANT' },
    { id: 'MANAGER', name: 'MANAGER' },
    { id: 'ADMIN', name: 'ADMIN' },
  ];

  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
      <label style={{ color: 'var(--text-secondary)', fontWeight: 500 }}>Role:</label>
      <select 
        value={role} 
        onChange={(e) => setRole(e.target.value)}
        style={{ minWidth: '200px' }}
      >
        {roles.map(r => (
          <option key={r.id} value={r.id}>{r.name}</option>
        ))}
      </select>
    </div>
  );
};

export default RoleSelector;
