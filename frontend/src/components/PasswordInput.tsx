import { useState, type InputHTMLAttributes } from 'react'
import { EyeIcon, EyeOffIcon } from './icons'

type Props = Omit<InputHTMLAttributes<HTMLInputElement>, 'type'> & {
  /** Control visibility from outside (e.g. show a freshly generated password); uncontrolled otherwise. */
  visible?: boolean
  onVisibleChange?: (visible: boolean) => void
}

/** A password field with an eye button that toggles between hidden and plain text. */
export function PasswordInput({ visible, onVisibleChange, className, style, ...inputProps }: Props) {
  const [ownVisible, setOwnVisible] = useState(false)
  const shown = visible ?? ownVisible

  function toggle() {
    const next = !shown
    if (visible === undefined) setOwnVisible(next)
    onVisibleChange?.(next)
  }

  return (
    <div className={`password-input ${className ?? ''}`} style={style}>
      <input {...inputProps} type={shown ? 'text' : 'password'} />
      <button
        type="button"
        className="password-input-toggle"
        onClick={toggle}
        aria-label={shown ? 'Hide password' : 'Show password'}
        aria-pressed={shown}
        title={shown ? 'Hide password' : 'Show password'}
      >
        {shown ? <EyeOffIcon /> : <EyeIcon />}
      </button>
    </div>
  )
}
