import { SearchIcon } from './icons'

interface SearchInputProps {
  value: string
  onChange: (value: string) => void
  placeholder?: string
}

/** A simple local list filter — reuses the global search's input styling, no dropdown. */
export function SearchInput({ value, onChange, placeholder = 'Search…' }: SearchInputProps) {
  return (
    <div className="toolbar-search">
      <div className="global-search-input">
        <SearchIcon />
        <input type="text" value={value} onChange={(e) => onChange(e.target.value)} placeholder={placeholder} />
      </div>
    </div>
  )
}
