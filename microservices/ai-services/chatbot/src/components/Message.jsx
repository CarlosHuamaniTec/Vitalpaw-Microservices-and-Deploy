import React from 'react';
import { Prism as SyntaxHighlighter } from 'react-syntax-highlighter';
import { darcula } from 'react-syntax-highlighter/dist/esm/styles/prism'; // Tema oscuro estilo terminal

function Message({ message }) {
  const isUser = message.sender === 'user';
  const messageClass = isUser
    ? 'bg-green-800 text-green-200 self-end'
    : 'bg-gray-700 text-green-400 self-start';
  const containerClass = isUser ? 'flex justify-end' : 'flex justify-start';

  const renderContent = (content) => {
    const codeBlockRegex = /(```(\w+)?\n([\s\S]*?)```)/g; // Captura todo el bloque de código
    let lastIndex = 0;
    const parts = [];

    content.replace(codeBlockRegex, (match, fullBlock, lang, code, offset) => {
      // Añadir el texto antes del bloque de código
      if (offset > lastIndex) {
        parts.push(<p key={`text-before-${lastIndex}`} className="mb-2">{content.substring(lastIndex, offset).trim()}</p>);
      }

      // Añadir el bloque de código
      parts.push(
        <SyntaxHighlighter
          key={`code-${offset}`}
          language={lang || 'plaintext'} // Usa el lenguaje detectado o 'plaintext'
          style={darcula} // Aplica el tema Darcula (oscuro, estilo terminal)
          customStyle={{
            backgroundColor: '#1e1e1e', // Fondo del bloque de código más oscuro
            padding: '10px',
            borderRadius: '5px',
            overflowX: 'auto',
            fontSize: '0.875rem',
            marginBottom: '1rem',
          }}
        >
          {code}
        </SyntaxHighlighter>
      );
      lastIndex = offset + match.length;
      return match;
    });

    if (lastIndex < content.length) {
      parts.push(<p key={`text-after-${lastIndex}`}>{content.substring(lastIndex).trim()}</p>);
    }

    return parts;
  };

  return (
    <div className={containerClass}>
      <div className={`p-3 rounded-lg max-w-[75%] ${messageClass}`}>
        {renderContent(message.content)} {/* Llama a la función que renderiza el contenido */}
        {message.sources && message.sources.length > 0 && (
          <div className="mt-2 text-xs opacity-70">
            <p className="font-semibold">Fuentes:</p>
            <ul className="list-disc list-inside">
              {message.sources.map((source, index) => (
                <li key={index}>
                  <a
                    href={source.url}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="underline hover:text-green-200"
                  >
                    {source.title}
                  </a>
                </li>
              ))}
            </ul>
          </div>
        )}
      </div>
    </div>
  );
}

export default Message;