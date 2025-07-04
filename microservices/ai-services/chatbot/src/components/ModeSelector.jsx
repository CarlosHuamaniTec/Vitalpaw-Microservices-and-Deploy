import React from 'react';

function ModeSelector({ selectedMode, onModeChange }) {
  return (
    <div className="mb-4 text-center">
      <span className="text-green-400 mr-2">Modo de Chat:</span>
      <select
        value={selectedMode}
        onChange={(e) => onModeChange(e.target.value)}
        className="p-2 border border-green-700 rounded-md bg-gray-700 text-green-400 focus:outline-none focus:ring-2 focus:ring-green-500"
      >
        <option value="only_documentation">Solo Documentación</option>
        <option value="no_documentation">Sin Documentación</option>
      </select>
    </div>
  );
}

export default ModeSelector;