import React, { useState, useEffect } from 'react';
import { fetchRoles, fetchUsers } from '../api/apiClient';

const SubjectSelector = ({ subjectType, setSubjectType, subjectId, setSubjectId }) => {
  const [roles, setRoles] = useState([]);
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    const loadData = async () => {
      setLoading(true);
      try {
        const [rolesData, usersData] = await Promise.all([
          fetchRoles().catch(() => []),
          fetchUsers().catch(() => [])
        ]);
        
        setRoles(rolesData);
        setUsers(usersData);
        
        // Auto-select first item if subjectId is empty and data is loaded
        if (subjectType === 'ROLE' && !subjectId && rolesData.length > 0) {
            setSubjectId(rolesData[0].id || rolesData[0].name);
        } else if (subjectType === 'USER' && !subjectId && usersData.length > 0) {
            setSubjectId(usersData[0].id || usersData[0].email);
        }
      } catch (err) {
        console.error("Failed to load subjects", err);
      } finally {
        setLoading(false);
      }
    };
    loadData();
  }, []);

  const handleTypeChange = (e) => {
    const newType = e.target.value;
    setSubjectType(newType);
    if (newType === 'ROLE' && roles.length > 0) {
        setSubjectId(roles[0].id || roles[0].name);
    } else if (newType === 'USER' && users.length > 0) {
        setSubjectId(users[0].id || users[0].email);
    } else {
        setSubjectId('');
    }
  };

  const options = subjectType === 'ROLE' ? roles : users;

  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
      <label style={{ color: 'var(--text-secondary)', fontWeight: 500 }}>Subject Type:</label>
      <select 
        value={subjectType} 
        onChange={handleTypeChange}
        style={{ minWidth: '150px' }}
      >
        <option value="ROLE">Role</option>
        <option value="USER">User</option>
      </select>

      <label style={{ color: 'var(--text-secondary)', fontWeight: 500 }}>Subject:</label>
      <select 
        value={subjectId} 
        onChange={(e) => setSubjectId(e.target.value)}
        style={{ minWidth: '200px' }}
        disabled={loading || options.length === 0}
      >
        {options.map(opt => {
          const value = opt.id || (subjectType === 'ROLE' ? opt.name : opt.email);
          const nameLabel = opt.name || opt.displayName || opt.email || 'Subject';
          const label = subjectType === 'ROLE'
            ? `${nameLabel}${opt.id ? ` (${opt.id.substring(0, 8)}...)` : ''}`
            : `${opt.firstName || ''} ${opt.lastName || ''} (${opt.email || opt.id || ''})`.trim();
          return <option key={opt.id || value} value={value}>{label}</option>
        })}
      </select>
    </div>
  );
};

export default SubjectSelector;
