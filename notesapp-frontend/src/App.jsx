import { useEffect, useState } from 'react'
import './index.css'

function App() {
  const [notes, setNotes] = useState([])
  const [text, setText] = useState('')

  const fetchNotes = async () => {
    try {
      const response = await fetch('http://localhost:8080/api/notes')
      const data = await response.json()
      setNotes(data)
    } catch (error) {
      console.error('Error fetching notes:', error)
    }
  }

  useEffect(() => {
    fetchNotes()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])
  const addNote = async () => {
    if (!text.trim()) return

    try {
      await fetch('http://localhost:8080/api/notes', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ text }),
      })

      setText('')
      fetchNotes()
    } catch (error) {
      console.error('Error adding note:', error)
    }
  }

  return (
      <div className="container">
        <h1>Notes App</h1>

        <div className="form">
          <input
              type="text"
              placeholder="Write a note..."
              value={text}
              onChange={(e) => setText(e.target.value)}
          />
          <button onClick={addNote}>Add</button>
        </div>

        <ul>
          {notes.map((note) => (
              <li key={note.id}>{note.text}</li>
          ))}
        </ul>
      </div>
  )
}

export default App