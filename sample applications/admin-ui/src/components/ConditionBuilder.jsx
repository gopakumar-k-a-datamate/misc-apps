import React, { useState, useEffect } from 'react';
import { fetchFields } from '../api/apiClient';
import Editor from '@monaco-editor/react';
import DynamicDropdown from './DynamicDropdown';

const getFieldLabel = (fieldPath, fields = []) => {
  if (!fieldPath) return '';
  if (fieldPath.startsWith('resource.')) {
    const rawName = fieldPath.substring(9);
    const matched = fields.find(f => f.fieldName === rawName || f.fieldName === fieldPath);
    return matched?.displayName || rawName;
  }
  if (fieldPath.startsWith('user.')) {
    const rawName = fieldPath.substring(5);
    return `user${rawName.charAt(0).toUpperCase() + rawName.slice(1)}`;
  }
  return fieldPath;
};

const ConditionRule = ({ rule, fields, permissionCode, onChange, onRemove }) => {
  const selectedField = fields.find(f => f.fieldName === rule.field);
  const valueType = rule.valueType || 'VALUE';

  const handleValueTypeChange = (e) => {
    const newType = e.target.value;
    let newVal = rule.value;
    if (newType === 'FIELD' || newType === 'FIELD_LIST') {
      newVal = typeof rule.value === 'string' ? rule.value : '';
    }
    onChange({ 
      ...rule, 
      valueType: newType, 
      value: newVal,
      compareTo: newType === 'MATH_EXPRESSION' ? (rule.compareTo || 'VALUE') : undefined,
      mathOperations: newType === 'MATH_EXPRESSION' ? (rule.mathOperations || []) : undefined
    });
  };

  const renderValueInput = () => {
    if (valueType === 'MATH_EXPRESSION') {
      const mathOps = rule.mathOperations || [];
      const userFieldSuggestions = [];
      const numericTypes = ['NUMBER', 'INTEGER', 'DECIMAL', 'LONG', 'FLOAT', 'DOUBLE', 'BIGDECIMAL'];
      const numericFields = fields.filter(f => !f.fieldType || numericTypes.includes(String(f.fieldType).toUpperCase()));
      const resourceFieldSuggestions = (numericFields.length > 0 ? numericFields : fields)
        .map(f => f.fieldName.startsWith('resource.') ? f.fieldName : `resource.${f.fieldName}`);
      const allSuggestions = [...userFieldSuggestions, ...resourceFieldSuggestions];

      return (
        <div style={{ flex: 1, display: 'flex', flexDirection: 'column', gap: '0.5rem', border: '1px solid var(--border-color)', padding: '0.5rem', borderRadius: '4px' }}>
          <div style={{ fontSize: '0.8rem', color: 'var(--text-secondary)' }}>Math Operations:</div>
          {mathOps.map((op, i) => (
             <div key={i} style={{ display: 'flex', gap: '0.4rem', alignItems: 'center' }}>
               <select value={op.mathOperator} onChange={e => {
                  const newOps = [...mathOps];
                  newOps[i] = { ...op, mathOperator: e.target.value };
                  onChange({ ...rule, mathOperations: newOps });
               }} style={{ padding: '0.2rem', borderRadius: '4px' }}>
                 <option value="ADD">+</option>
                 <option value="SUBTRACT">-</option>
                 <option value="MULTIPLY">*</option>
                 <option value="DIVIDE">/</option>
               </select>
               <select value={op.operandType} onChange={e => {
                  const newOps = [...mathOps];
                  newOps[i] = { ...op, operandType: e.target.value, value: '' };
                  onChange({ ...rule, mathOperations: newOps });
               }} style={{ padding: '0.2rem', borderRadius: '4px' }}>
                 <option value="VALUE">Static</option>
                 <option value="FIELD">Field</option>
               </select>
               {op.operandType === 'FIELD' ? (
                 <select 
                   value={op.value || ''}
                   onChange={e => {
                     const newOps = [...mathOps];
                     newOps[i] = { ...op, value: e.target.value };
                     onChange({ ...rule, mathOperations: newOps });
                   }}
                   style={{ flex: 1, padding: '0.2rem', border: '1px solid var(--border-color)', borderRadius: '4px' }}
                 >
                   <option value="">Select Field...</option>
                   {userFieldSuggestions.length > 0 && (
                     <optgroup label="User Context">
                       {userFieldSuggestions.map(sf => (
                         <option key={sf} value={sf}>{getFieldLabel(sf, fields)}</option>
                       ))}
                     </optgroup>
                   )}
                   <optgroup label="Resource Fields">
                     {resourceFieldSuggestions.map(rf => (
                       <option key={rf} value={rf}>{getFieldLabel(rf, fields)}</option>
                     ))}
                   </optgroup>
                 </select>
               ) : (
                 <input type="text" placeholder="Number" value={op.value || ''} onChange={e => {
                    const newOps = [...mathOps];
                    newOps[i] = { ...op, value: e.target.value };
                    onChange({ ...rule, mathOperations: newOps });
                 }} style={{ flex: 1, padding: '0.2rem', borderRadius: '4px', border: '1px solid var(--border-color)' }}/>
               )}
               <button className="btn" style={{ padding: '0.2rem 0.5rem', background: 'rgba(239, 68, 68, 0.1)', color: '#fca5a5' }} onClick={() => {
                  const newOps = mathOps.filter((_, idx) => idx !== i);
                  onChange({ ...rule, mathOperations: newOps });
               }}>✕</button>
             </div>
          ))}
          <button className="btn" style={{ alignSelf: 'flex-start', fontSize: '0.8rem', padding: '0.3rem 0.6rem' }} onClick={() => {
             onChange({ ...rule, mathOperations: [...mathOps, { mathOperator: 'ADD', operandType: 'VALUE', value: '' }] });
          }}>+ Add Math Op</button>

          <div style={{ height: '1px', background: 'var(--border-color)', margin: '0.5rem 0' }}></div>

          <div style={{ fontSize: '0.8rem', color: 'var(--text-secondary)' }}>Compare Against:</div>
          <div style={{ display: 'flex', gap: '0.4rem', alignItems: 'center' }}>
            <select value={rule.compareTo || 'VALUE'} onChange={e => onChange({ ...rule, compareTo: e.target.value, value: '' })} style={{ padding: '0.3rem', borderRadius: '4px' }}>
               <option value="VALUE">Static Value</option>
               <option value="FIELD">Field</option>
            </select>
            {rule.compareTo === 'FIELD' ? (
              <select 
                value={rule.value || ''}
                onChange={e => onChange({ ...rule, value: e.target.value })}
                style={{ flex: 1, padding: '0.3rem', border: '1px solid var(--border-color)', borderRadius: '4px' }}
              >
                <option value="">Select Field...</option>
                {userFieldSuggestions.length > 0 && (
                  <optgroup label="User Context">
                    {userFieldSuggestions.map(sf => (
                      <option key={sf} value={sf}>{getFieldLabel(sf, fields)}</option>
                    ))}
                  </optgroup>
                )}
                <optgroup label="Resource Fields">
                  {resourceFieldSuggestions.map(rf => (
                    <option key={rf} value={rf}>{getFieldLabel(rf, fields)}</option>
                  ))}
                </optgroup>
              </select>
            ) : (
              <input type="text" placeholder="Target value" value={rule.value || ''} onChange={e => onChange({ ...rule, value: e.target.value })} style={{ flex: 1, padding: '0.3rem', borderRadius: '4px', border: '1px solid var(--border-color)' }}/>
            )}
          </div>
        </div>
      );
    }

    if (valueType === 'FIELD' || valueType === 'FIELD_LIST') {
      const userFieldSuggestions = [
        'user.location',
        'user.department',
        'user.id',
        'user.email',
        'user.roles'
      ];
      const resourceFieldSuggestions = fields.map(f => f.fieldName.startsWith('resource.') ? f.fieldName : `resource.${f.fieldName}`);
      const allSuggestions = [...userFieldSuggestions, ...resourceFieldSuggestions];

      return (
        <div style={{ flex: 1, display: 'flex', gap: '0.4rem', alignItems: 'center' }}>
          <select 
            value={allSuggestions.includes(rule.value) ? rule.value : '__custom__'}
            onChange={e => {
              if (e.target.value !== '__custom__') {
                onChange({ ...rule, value: e.target.value });
              }
            }}
            style={{ padding: '0.4rem', border: '1px solid var(--border-color)', borderRadius: '4px', maxWidth: '180px' }}
          >
            <option value="">Select Field...</option>
            <optgroup label="User Context">
              {userFieldSuggestions.map(sf => (
                <option key={sf} value={sf}>{getFieldLabel(sf, fields)}</option>
              ))}
            </optgroup>
            <optgroup label="Resource Fields">
              {resourceFieldSuggestions.map(rf => (
                <option key={rf} value={rf}>{getFieldLabel(rf, fields)}</option>
              ))}
            </optgroup>
            <option value="__custom__">Custom path...</option>
          </select>
          <input 
            type="text" 
            placeholder={valueType === 'FIELD' ? "e.g. user.location" : "e.g. resource.allowedDepartments"} 
            value={typeof rule.value === 'string' ? rule.value : ''} 
            onChange={e => onChange({ ...rule, value: e.target.value })}
            style={{ flex: 1, padding: '0.4rem', border: '1px solid var(--border-color)', borderRadius: '4px' }}
          />
        </div>
      );
    }

    const isArrayOp = rule.comparison === 'in' || rule.comparison === 'not_in';

    if (isArrayOp) {
      const displayValue = Array.isArray(rule.value) ? rule.value.join(', ') : String(rule.value || '');
      return (
        <input 
          type="text" 
          placeholder="value1, value2..." 
          value={displayValue} 
          onChange={e => {
            const arr = e.target.value.split(',').map(s => s.trimLeft());
            onChange({ ...rule, value: arr });
          }}
          style={{ flex: 1, padding: '0.4rem', border: '1px solid var(--border-color)', borderRadius: '4px' }}
        />
      );
    }

    if (!selectedField) {
      return (
        <input 
          type="text" 
          placeholder="Value..." 
          value={rule.value || ''} 
          onChange={e => onChange({ ...rule, value: e.target.value })}
          style={{ flex: 1, padding: '0.4rem', border: '1px solid var(--border-color)', borderRadius: '4px' }}
        />
      );
    }

    if (selectedField.optionsEndpoint) {
      return (
        <DynamicDropdown 
          endpoint={selectedField.optionsEndpoint}
          permissionCode={permissionCode}
          value={rule.value || ''}
          onChange={(val) => onChange({ ...rule, value: val })}
        />
      );
    }
    if (selectedField.allowedValues && selectedField.allowedValues.length > 0) {
      return (
        <select 
          value={rule.value || ''} 
          onChange={e => onChange({ ...rule, value: e.target.value })}
          style={{ flex: 1, padding: '0.4rem', border: '1px solid var(--border-color)', borderRadius: '4px' }}
        >
          <option value="">Select value...</option>
          {selectedField.allowedValues.map(val => (
            <option key={val} value={val}>{val}</option>
          ))}
        </select>
      );
    }

    if (selectedField.fieldType === 'BOOLEAN') {
      return (
        <select 
          value={rule.value !== undefined ? String(rule.value) : ''} 
          onChange={e => onChange({ ...rule, value: e.target.value === 'true' })}
          style={{ flex: 1, padding: '0.4rem', border: '1px solid var(--border-color)', borderRadius: '4px' }}
        >
          <option value="">Select...</option>
          <option value="true">True</option>
          <option value="false">False</option>
        </select>
      );
    }

    if (selectedField.fieldType === 'NUMBER') {
      return (
        <input 
          type="number" 
          placeholder="Value..." 
          value={rule.value || ''} 
          onChange={e => onChange({ ...rule, value: Number(e.target.value) })}
          style={{ flex: 1, padding: '0.4rem', border: '1px solid var(--border-color)', borderRadius: '4px' }}
        />
      );
    }

    return (
      <input 
        type="text" 
        placeholder="Value..." 
        value={rule.value || ''} 
        onChange={e => onChange({ ...rule, value: e.target.value })}
        style={{ flex: 1, padding: '0.4rem', border: '1px solid var(--border-color)', borderRadius: '4px' }}
      />
    );
  };

  const handleFieldChange = (e) => {
    const newFieldName = e.target.value;
    const newField = fields.find(f => f.fieldName === newFieldName);
    let defaultValue = '';
    if (newField?.fieldType === 'BOOLEAN') defaultValue = true;
    else if (newField?.allowedValues?.length > 0) defaultValue = newField.allowedValues[0];

    onChange({ ...rule, field: newFieldName, value: defaultValue, valueType: valueType });
  };

  return (
    <div style={{ display: 'flex', gap: '0.5rem', marginBottom: '0.5rem', width: '100%', alignItems: valueType === 'MATH_EXPRESSION' ? 'flex-start' : 'center' }}>
      <select value={rule.field || ''} onChange={handleFieldChange} style={{ padding: '0.4rem', border: '1px solid var(--border-color)', borderRadius: '4px', minWidth: '150px' }}>
        {fields.map(f => (
          <option key={f.fieldName} value={f.fieldName}>{f.displayName}</option>
        ))}
      </select>
      <select value={rule.comparison || '=='} onChange={e => {
          const newComp = e.target.value;
          const isArray = (newComp === 'in' || newComp === 'not_in') && valueType === 'VALUE';
          let newVal = rule.value;
          if (isArray && !Array.isArray(newVal)) newVal = newVal ? [String(newVal)] : [];
          if (!isArray && Array.isArray(newVal)) newVal = newVal[0] || '';
          onChange({ ...rule, comparison: newComp, value: newVal, valueType: valueType });
        }} style={{ padding: '0.4rem', border: '1px solid var(--border-color)', borderRadius: '4px' }}>
        <option value="==">==</option>
        <option value="!=">!=</option>
        <option value="in">IN</option>
        <option value="not_in">NOT IN</option>
        <option value="contains">CONTAINS</option>
        <option value="<=">&lt;=</option>
        <option value=">=">&gt;=</option>
        <option value="<">&lt;</option>
        <option value=">">&gt;</option>
      </select>
      <select value={valueType} onChange={handleValueTypeChange} style={{ padding: '0.4rem', border: '1px solid var(--border-color)', borderRadius: '4px', minWidth: '120px' }}>
        <option value="VALUE">Static Value</option>
        <option value="FIELD">Field Comparison</option>
        <option value="FIELD_LIST">Field List</option>
        <option value="MATH_EXPRESSION">Math Expression</option>
      </select>
      {renderValueInput()}
      <button className="btn" style={{ padding: '0.4rem 0.6rem', background: 'rgba(239, 68, 68, 0.1)', color: '#fca5a5' }} onClick={onRemove}>✕</button>
    </div>
  );
};

const ConditionGroup = ({ node, fields, permissionCode, onChange, onRemove, isRoot }) => {
  const handleOperatorChange = (e) => {
    onChange({ ...node, operator: e.target.value });
  };

  const handleChildChange = (index, newChild) => {
    const newChildren = [...(node.children || [])];
    newChildren[index] = newChild;
    onChange({ ...node, children: newChildren });
  };

  const handleRemoveChild = (index) => {
    const newChildren = (node.children || []).filter((_, i) => i !== index);
    onChange({ ...node, children: newChildren });
  };

  const addRule = () => {
    const defaultField = fields[0];
    let defaultValue = '';
    if (defaultField?.fieldType === 'BOOLEAN') defaultValue = true;
    else if (defaultField?.allowedValues?.length > 0) defaultValue = defaultField.allowedValues[0];

    const newRule = { 
      field: defaultField?.fieldName || '', 
      comparison: '==', 
      value: defaultValue,
      valueType: 'VALUE'
    };
    const newChildren = [...(node.children || []), newRule];
    onChange({ ...node, children: newChildren });
  };

  const addGroup = (op) => {
    const newGroup = { operator: op, children: [] };
    const newChildren = [...(node.children || []), newGroup];
    onChange({ ...node, children: newChildren });
  };

  const children = node.children || [];
  // Separate rules and groups to cluster rules together at the top of the group card
  const rules = children.map((c, i) => ({ child: c, index: i })).filter(item => !item.child.operator);
  const groups = children.map((c, i) => ({ child: c, index: i })).filter(item => item.child.operator);

  return (
    <div style={{ 
      border: '1px solid var(--border-color)', 
      borderRadius: '8px', 
      padding: '1.5rem', 
      background: 'rgba(255, 255, 255, 0.02)',
      position: 'relative',
      marginBottom: isRoot ? '0' : '1rem',
      marginTop: isRoot ? '0' : '1rem'
    }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '1rem' }}>
        <span style={{ fontWeight: 500, color: 'var(--text-primary)' }}>Match</span>
        <select 
          value={node.operator || 'AND'} 
          onChange={handleOperatorChange} 
          style={{ 
            padding: '0.3rem', 
            fontSize: '0.9rem', 
            border: '1px solid var(--border-color)',
            borderRadius: '4px',
            cursor: 'pointer'
          }}
        >
          <option value="AND">ALL (AND)</option>
          <option value="OR">ANY (OR)</option>
          <option value="NOT">NONE (NOT)</option>
        </select>
        {!isRoot && (
          <button className="btn" style={{ padding: '0.3rem 0.6rem', fontSize: '0.8rem', marginLeft: 'auto', background: 'rgba(239, 68, 68, 0.1)', color: '#fca5a5' }} onClick={onRemove}>
            🗑 Remove Group
          </button>
        )}
      </div>

      {node.operator === 'NOT' && children.length > 1 && (
        <div style={{ color: '#fca5a5', fontSize: '0.8rem', marginBottom: '0.5rem' }}>
          Warning: NOT group should ideally have only one condition.
        </div>
      )}

      <div style={{ display: 'flex', flexDirection: 'column', gap: '0.2rem', marginBottom: groups.length > 0 ? '1.5rem' : '0' }}>
        {rules.map((item) => (
          <ConditionRule 
            key={`rule-${item.index}`}
            rule={item.child} 
            fields={fields} 
            permissionCode={permissionCode}
            onChange={(newRule) => handleChildChange(item.index, newRule)} 
            onRemove={() => handleRemoveChild(item.index)} 
          />
        ))}
      </div>

      <div style={{ display: 'flex', flexDirection: 'column' }}>
        {groups.map((item, groupIdx) => {
          return (
            <React.Fragment key={`group-${item.index}`}>
              {groupIdx > 0 && (
                <div style={{ 
                  display: 'flex', 
                  alignItems: 'center', 
                  justifyContent: 'center', 
                  margin: '1rem 0'
                }}>
                  <div style={{ flex: 1, height: '1px', background: 'var(--border-color)' }}></div>
                  <div style={{ 
                    padding: '0 1rem', 
                    color: 'var(--text-secondary)', 
                    fontWeight: 'bold', 
                    fontSize: '0.85rem',
                    letterSpacing: '1px'
                  }}>
                    {node.operator}
                  </div>
                  <div style={{ flex: 1, height: '1px', background: 'var(--border-color)' }}></div>
                </div>
              )}
              <ConditionGroup 
                node={item.child} 
                fields={fields} 
                permissionCode={permissionCode}
                onChange={(newChild) => handleChildChange(item.index, newChild)} 
                onRemove={() => handleRemoveChild(item.index)} 
                isRoot={false}
              />
            </React.Fragment>
          );
        })}
      </div>

      <div style={{ marginTop: '1.5rem', display: 'flex', gap: '0.5rem' }}>
        <button className="btn" style={{ fontSize: '0.8rem', padding: '0.5rem 1rem', background: 'rgba(59, 130, 246, 0.1)', color: '#60a5fa' }} onClick={addRule}>+ Add Rule</button>
        <button className="btn" style={{ fontSize: '0.8rem', padding: '0.5rem 1rem', background: 'rgba(59, 130, 246, 0.1)', color: '#60a5fa' }} onClick={() => addGroup('AND')}>+ Add Group</button>
        <button className="btn" style={{ fontSize: '0.8rem', padding: '0.5rem 1rem', background: 'rgba(239, 68, 68, 0.1)', color: '#fca5a5' }} onClick={() => addGroup('NOT')}>+ Add NOT Group</button>
      </div>
    </div>
  );
};

const generatePreview = (node, fields, depth = 0, isRoot = true) => {
  if (!node) return '';
  const indent = '  '.repeat(depth);
  
  if (node.operator) {
    if (!node.children || node.children.length === 0) return `${indent}(Empty Group)`;
    
    const childDepth = isRoot ? depth : depth + 1;
    const childPreviews = node.children.map(c => generatePreview(c, fields, childDepth, false)).filter(Boolean);
    
    if (childPreviews.length === 0) return `${indent}(Empty Group)`;
    if (childPreviews.length === 1) {
      if (node.operator === 'NOT') return `${indent}NOT (${childPreviews[0].trim()})`;
      return generatePreview(node.children[0], fields, depth, isRoot);
    }
    
    const childIndent = '  '.repeat(childDepth);
    const joiner = `\n${childIndent}${node.operator}\n`;
    
    if (isRoot) {
      return childPreviews.join(joiner);
    } else {
      if (node.operator === 'NOT') {
        return `${indent}NOT (\n${childPreviews.join(joiner)}\n${indent})`;
      }
      return `${indent}(\n${childPreviews.join(joiner)}\n${indent})`;
    }
  }
  
  // It's a rule
  const fieldDisplay = fields.find(f => f.fieldName === node.field)?.displayName || node.field || 'Unknown Field';
  const vType = node.valueType || 'VALUE';
  let valStr = '';

  if (vType === 'MATH_EXPRESSION') {
    let mathStr = fieldDisplay;
    if (node.mathOperations) {
      node.mathOperations.forEach(op => {
        let opSym = op.mathOperator === 'MULTIPLY' ? '*' : op.mathOperator === 'DIVIDE' ? '/' : op.mathOperator === 'SUBTRACT' ? '-' : '+';
        mathStr = `(${mathStr} ${opSym} ${op.value})`;
      });
    }
    const targetStr = node.compareTo === 'FIELD' ? `${node.value || ''} (Field)` : `"${node.value || ''}"`;
    return `${indent}${mathStr} ${node.comparison || '=='} ${targetStr}`;
  }

  if (vType === 'FIELD') {
    valStr = `${node.value || ''} (Field)`;
  } else if (vType === 'FIELD_LIST') {
    valStr = `${node.value || ''} (Field List)`;
  } else if (Array.isArray(node.value)) {
    valStr = `[${node.value.filter(v => v !== '').map(v => `"${v}"`).join(', ')}]`;
  } else if (typeof node.value === 'string') {
    valStr = `"${node.value}"`;
  } else if (node.value === undefined || node.value === null) {
    valStr = 'null';
  } else {
    valStr = String(node.value);
  }
  
  return `${indent}${fieldDisplay} ${node.comparison || '=='} ${valStr}`;
};

const ConditionBuilder = ({ permissionCode, policy, validationErrors, onClose, onSave }) => {
  const [fields, setFields] = useState([]);
  
  const existingExpression = policy?.expressionJson;
  const initUseCustomRego = policy?.useCustomRego || false;
  const initCustomRegoSnippet = policy?.customRegoSnippet || '';
  const initDenialMessage = policy?.denialMessage || '';

  const [useCustomRego, setUseCustomRego] = useState(initUseCustomRego);
  const [customRegoSnippet, setCustomRegoSnippet] = useState(initCustomRegoSnippet);
  const [denialMessage, setDenialMessage] = useState(initDenialMessage);

  const [expressionTree, setExpressionTree] = useState(() => {
    if (existingExpression?.operator) {
      return existingExpression;
    }
    return { operator: 'AND', children: existingExpression?.children || [] };
  });

  useEffect(() => {
    loadFields();
  }, [permissionCode]);

  const loadFields = async () => {
    const data = await fetchFields(permissionCode);
    setFields(data);
  };

  const handleSave = () => {
    if (useCustomRego) {
      onSave(permissionCode, null, true, customRegoSnippet, denialMessage);
    } else {
      if (!expressionTree.children || expressionTree.children.length === 0) {
        onSave(permissionCode, null, false, '', denialMessage);
      } else {
        onSave(permissionCode, expressionTree, false, '', denialMessage);
      }
    }
  };

  const handleEditorMount = (editor, monaco) => {
    if (validationErrors && validationErrors.length > 0) {
      const markers = validationErrors.map(err => ({
        severity: monaco.MarkerSeverity.Error,
        message: err.message,
        startLineNumber: err.line || 1,
        startColumn: err.column || 1,
        endLineNumber: err.line || 1,
        endColumn: (err.column || 1) + 5,
      }));
      monaco.editor.setModelMarkers(editor.getModel(), "owner", markers);
    }
  };

  return (
    <div className="modal-overlay">
      <style>
        {`
          .hide-scrollbar::-webkit-scrollbar {
            display: none;
          }
          .hide-scrollbar {
            -ms-overflow-style: none;
            scrollbar-width: none;
          }
        `}
      </style>
      <div className="glass-panel modal-content" style={{ margin: 'auto', maxWidth: '1200px', width: '95%', maxHeight: '90vh', display: 'flex', flexDirection: 'column' }}>
        <div className="modal-header" style={{ paddingBottom: '1rem', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
            <h3 style={{ margin: 0 }}>Condition Builder: {permissionCode}</h3>
            <label style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', fontSize: '0.9rem', color: 'var(--text-secondary)' }}>
              <input 
                type="checkbox" 
                className="checkbox" 
                checked={useCustomRego} 
                onChange={(e) => setUseCustomRego(e.target.checked)} 
              />
              Use Custom Rego
            </label>
          </div>
          <button className="btn" onClick={onClose}>✕</button>
        </div>

        {useCustomRego ? (
          <div style={{ flex: 1, display: 'flex', flexDirection: 'column', minHeight: '400px' }}>
            <p style={{ color: 'var(--text-secondary)', fontSize: '0.9rem', marginBottom: '1rem' }}>
              Write custom Rego code. You do not need to specify `package` or `allow_rule` / `deny_rule` headers. Just provide helper rules if needed, or directly write logic. Wait, ADR says the snippet is injected verbatim.
            </p>
            <div style={{ flex: 1, border: '1px solid var(--border-color)', borderRadius: '8px', overflow: 'hidden', minHeight: '400px' }}>
              <Editor
                height="400px"
                defaultLanguage="plaintext"
                theme="vs-dark"
                value={customRegoSnippet}
                onChange={setCustomRegoSnippet}
                onMount={handleEditorMount}
                options={{
                  minimap: { enabled: false },
                  lineNumbers: 'on',
                  tabSize: 4,
                  insertSpaces: true
                }}
              />
            </div>
          </div>
        ) : (
          <div style={{ display: 'flex', flex: 1, minHeight: 0, gap: '1.5rem' }}>
            {/* Left Column: Builder */}
            <div className="hide-scrollbar" style={{ flex: '1.8', overflowY: 'auto', paddingRight: '0.5rem', paddingBottom: '1rem' }}>
              {fields.length > 0 ? (
                <ConditionGroup
                  node={expressionTree}
                  fields={fields}
                  permissionCode={permissionCode}
                  onChange={setExpressionTree}
                  isRoot={true}
                />
              ) : (
                <div>Loading fields...</div>
              )}
            </div>

            {/* Right Column: Preview */}
            <div className="hide-scrollbar" style={{
              flex: '1',
              display: 'flex',
              flexDirection: 'column',
              padding: '1.5rem',
              background: 'rgba(0, 0, 0, 0.2)',
              borderRadius: '8px',
              border: '1px solid var(--border-color)',
              overflowY: 'auto'
            }}>
              <h4 style={{ margin: '0 0 1rem 0', color: 'var(--text-secondary)', fontSize: '0.9rem', textTransform: 'uppercase', letterSpacing: '1px', flexShrink: 0 }}>Preview</h4>
              <div style={{
                fontFamily: 'monospace',
                color: 'var(--text-primary)',
                fontSize: '0.9rem',
                lineHeight: '1.5',
                whiteSpace: 'pre-wrap',
                wordBreak: 'break-word',
                flex: 1
              }}>
                {fields.length > 0 ? generatePreview(expressionTree, fields) : 'Loading preview...'}
              </div>
            </div>
          </div>
        )}

        <div style={{ padding: '1rem', borderTop: '1px solid var(--border-color)', marginTop: '1rem', background: 'rgba(255, 255, 255, 0.02)' }}>
          <label style={{ display: 'block', color: 'var(--text-secondary)', fontSize: '0.9rem', marginBottom: '0.5rem', fontWeight: 500 }}>Denial Message (Optional)</label>
          <input 
            type="text" 
            placeholder="Human-readable message shown to end users when this policy causes a denial..." 
            value={denialMessage} 
            onChange={(e) => setDenialMessage(e.target.value)}
            style={{ width: '100%', padding: '0.6rem', border: '1px solid var(--border-color)', borderRadius: '4px', background: 'rgba(0, 0, 0, 0.2)', color: 'var(--text-primary)', fontSize: '0.9rem' }}
          />
        </div>

        <div style={{ padding: '1rem 0 0 0', display: 'flex', justifyContent: 'flex-end', gap: '1rem', borderTop: '1px solid var(--border-color)', marginTop: '0' }}>
          <button className="btn" onClick={onClose}>Cancel</button>
          <button className="btn btn-primary" onClick={handleSave}>Apply</button>
        </div>
      </div>
    </div>
  );
};

export default ConditionBuilder;
