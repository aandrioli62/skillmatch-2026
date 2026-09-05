import { Autocomplete, Chip, TextField } from '@mui/material'
import { useEffect, useState } from 'react'
import api from '../services/api'

// Autocomplete backed by the shared /api/v1/skills catalog. `freeSolo` lets
// the user type a brand new skill, created server-side the first time it's
// saved. Two shapes:
//   - single skill (used for a project requirement row): value/onChange are strings.
//   - multiple = true (used for a professional's own skill list): value/onChange are string arrays,
//     rendered as chips (the same "oval" look already used read-only elsewhere in the app).
export default function SkillPicker({ value, onChange, label = 'Skill', multiple = false, ...props }) {
  const [options, setOptions] = useState([])
  const [inputValue, setInputValue] = useState(multiple ? '' : value || '')

  useEffect(() => {
    const handle = setTimeout(() => {
      api
        .get('/skills', { params: { search: inputValue } })
        .then((res) => setOptions(res.data.map((s) => s.name)))
        .catch(() => setOptions([]))
    }, 300)
    return () => clearTimeout(handle)
  }, [inputValue])

  if (multiple) {
    return (
      <Autocomplete
        multiple
        freeSolo
        options={options}
        value={value || []}
        inputValue={inputValue}
        onInputChange={(event, newInputValue) => setInputValue(newInputValue)}
        onChange={(event, newValue) => onChange(newValue)}
        renderValue={(selectedOptions, getItemProps) =>
          selectedOptions.map((option, index) => (
            <Chip label={option} size="small" {...getItemProps({ index })} key={option} />
          ))
        }
        renderInput={(params) => <TextField {...params} label={label} placeholder="Aggiungi competenza" />}
        {...props}
      />
    )
  }

  return (
    <Autocomplete
      freeSolo
      options={options}
      value={value || ''}
      inputValue={inputValue}
      onInputChange={(event, newInputValue) => {
        setInputValue(newInputValue)
        onChange(newInputValue)
      }}
      onChange={(event, newValue) => onChange(newValue || '')}
      renderInput={(params) => <TextField {...params} label={label} />}
      {...props}
    />
  )
}
